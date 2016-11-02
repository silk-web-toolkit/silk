(ns silk.core.transform.data
  "Data related transformations.  "
  (:require [com.rpl.specter :as spec]
            [clojure.edn :as edn]
            [silk.core.input.file :as sf]
            [silk.core.transform.coerce :as sc])
  (:use [clojure.string :only [split]]
        [clojure.set    :only [rename-keys]]))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- sort->
  "Default to descending sort."
  [data sort dir]
  (let [ascending (sort-by (keyword sort) data)]
    (if-let [d dir]
      (if (= d "ascending") ascending (reverse ascending))
      ascending)))

(defn- repeating-tag? [t] (.contains [:ul :ol :tbody] t))

(defn- silk-attr? [k] (re-find #"data-sw-(\S+)" (name k)))

(defn- get-data [d v] (str (get d (keyword (last (split v #"\."))))))

(defn- inject-text
  [hick d]
  (if-let [v (get-in hick [:attrs :data-sw-text])]
    (-> hick
        (assoc :content [(get-data d v)])
        (update-in [:attrs] dissoc :data-sw-text))
    hick))

(defn- inject-attr
  [hick d]
  (let [attrs (:attrs hick)
        s-attrs (select-keys attrs (filter #(and (silk-attr? %) (not (= :data-sw-text %))) (keys attrs)))
        n-attrs (into (sorted-map) (for [[k v] s-attrs] {(keyword (last (silk-attr? k))) (get-data d v)}))]
    (-> hick
       (update-in [:attrs] merge n-attrs)
       (update-in [:attrs] #(apply dissoc %1 %2) (keys s-attrs)))))

(defn- keys-with-last-index
  [hick keys]
  (loop [k keys h hick indexes []]
    (if-let [fk (first k)]
      (recur (next k) (last (fk h)) (into indexes [fk (dec (count (fk h)))]))
      (vec (drop-last indexes)))))

(defn- update-hick
  [hick keys node]
  (update-in hick (keys-with-last-index hick keys) conj node))

(defn- map-content
  [hick func]
  (let [fhick (func hick)]
    (loop [h (:content fhick) hl [:content] n nil nl nil r-hick (assoc fhick :content [])]
      (if (or (vector? h) (seq? h))
        (let [changed-hick (if (map? (first h)) (func (first h)) (first h))]
          (if-let [c (:content changed-hick)]
            (recur c
                   (conj hl :content)
                   (concat (next h) n)
                   (concat (for [i (next h)] hl) nl)
                   (update-hick r-hick hl (assoc changed-hick :content [])))
           (recur (next h)
                  hl
                  n
                  nl
                  (update-hick r-hick hl changed-hick))))
          (if n
            (recur (flatten (vector (first n)))
                   (flatten (vector (first nl)))
                   (next n)
                   (next nl)
                   r-hick)
            r-hick)))))

(defn- data-level
  "Data level based on deepest data-sw-* value .e.g items.childs"
  [hick]
  (let [list1 (map-content hick #(when (or (= hick %) (not (repeating-tag? (:tag %)))) %))
        attrs (spec/select (spec/walker #(some (fn [k] (silk-attr? (first k))) (:attrs %))) list1)
        avals (flatten (map #(vals (:attrs %)) attrs))]
    (when-let [deepest (last (sort-by #(count (re-seq #"\." %)) avals))]
      (seq (map #(keyword %) (drop-last (split deepest #"\.")))))))

(defn- inject
  [hick data data-pos]
  (def skip? false)
  (map-content hick (fn [h]
    (when (not (or (= hick h) (repeating-tag? (:tag h)))) (def skip? true))
    (if (and skip? (repeating-tag? (:tag h)))
      (let [p (if-let [d (data-level h)] (conj data-pos (last d)) data-pos)
            d (get-in data p)
            c (map-indexed (fn [i _] (:content (inject h data (conj p i)))) d)]
        (if (some keyword? p)
          (assoc h :content (flatten c))
          h))
      (if-let [dl-data (when (or (map? h) skip?) (get-in data data-pos))]
        (-> h
            (inject-text dl-data)
            (inject-attr dl-data))
        h)))))

(defn- source
  [hick]
  (let [src    (:data-sw-source (:attrs hick))
        param  (:data-sw-sort (:attrs hick))
        direc  (:data-sw-sort-dir (:attrs hick))
        edn    (edn/read-string (slurp (sf/data src)))
        sorted (if-let [p param] (sort-> edn p direc) edn)
        limit  (if-let [s (:data-sw-limit (:attrs hick))] (sc/parse-int s) (count sorted))
        data   (vec (take limit sorted))]
   (spec/transform
     (spec/walker #(repeating-tag? (:tag %)))
     #(assoc % :content (flatten (map-indexed (fn [i _] (:content (inject % data (vector i)))) data)))
     (update-in hick [:attrs] dissoc :data-sw-source :data-sw-limit :data-sw-sort :data-sw-sort-dir))))

;; =============================================================================
;; Data transformations
;; =============================================================================

(defn process-data
  "Looks for data-sw-source and injects into it"
  [hick]
  (spec/transform (spec/walker #(get-in % [:attrs :data-sw-source])) #(source %) hick))
