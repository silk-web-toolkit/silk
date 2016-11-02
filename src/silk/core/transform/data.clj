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
    (update-in (assoc hick :content [(get-data d v)]) [:attrs] dissoc :data-sw-text)
    hick))

(defn- inject-attr
  [hick d]
  (assoc hick :attrs (into (sorted-map)
    (for [[k v] (:attrs hick)]
      (if-let [k2 (keyword (last (silk-attr? k)))]
        {k2 (get-data d v)}
        {k v})))))

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

(defn- inject-single
  [hick data data-pos]
  (def add-data? true)
  (map-content hick (fn [h]
    (let [; _ (println "->" data-pos)
          dl-data (get-in data data-pos)
          _ (println dl-data)
          ]
    (if (and (not (= hick h)) (repeating-tag? (:tag h)))
        (let [new-pos (if-let [d (data-level h)] (conj data-pos (last d)) data-pos)
              new-data (get-in data new-pos)]
          (println new-pos)
          ; (println new-data)
          (println ">>>>>>>>>>>>>>>>>>>>>>>>>>>>")
          (assoc h :content (flatten (map-indexed
            (fn [i d] (if-let [x (empty? (:content (inject-single h data (conj new-pos i))))] x [(str (conj new-pos i))]))
            new-data))))
      (if (or (= hick h) (and (not (repeating-tag? (:tag h))) add-data?))
        (-> h
            (inject-text dl-data)
            (inject-attr dl-data))
        (do (def add-data? false)
            h)))))))

(defn- source
  [hick]
  (let [src    (:data-sw-source (:attrs hick))
        param  (:data-sw-sort (:attrs hick))
        direc  (:data-sw-sort-dir (:attrs hick))
        edn    (edn/read-string (slurp (sf/data src)))
        sorted (if-let [p param] (sort-> edn p direc) edn)
        limit  (if-let [s (:data-sw-limit (:attrs hick))] (sc/parse-int s) (count sorted))
        data   (vec (take limit sorted))]
   (println "--------------------------------------")
   (spec/transform
     (spec/walker #(repeating-tag? (:tag %)))
     #(assoc % :content (flatten (map-indexed (fn [i d] (:content (inject-single % data (vector i)))) data)))
     (update-in hick [:attrs] dissoc :data-sw-source :data-sw-limit :data-sw-sort :data-sw-sort-dir))))

;; =============================================================================
;; Data transformations
;; =============================================================================

(defn process-data
  "Looks for data-sw-source and injects into it"
  [hick]
  (spec/transform (spec/walker #(get-in % [:attrs :data-sw-source])) #(source %) hick))
