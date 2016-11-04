(ns silk.core.transform.data
  "Data related transformations.  "
  (:require [com.rpl.specter :as spec]
            [hickory.core :as h]
            [silk.core.input.env :as se]
            [silk.core.input.file :as sf]
            [silk.core.transform.path :as sp]
            [silk.core.transform.walk :as sw])
  (:use [clojure.string :only [split]]
        [clojure.set    :only [rename-keys]]))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- repeating-tag? [t] (.contains [:ul :ol :tbody] t))

(defn- silk-attr? [k] (re-find #"data-sw-(\S+)" (name k)))

(defn- get-data
  [d v]
  (if (= v "sw/path")
    (sp/update-extension (sp/relativise-> (se/project-data-path) (:sw/path d)) "html")
    (str (get d (keyword (last (split v #"\.")))))))

(defn- inject-text
  [hick d]
  (if-let [v (get-in hick [:attrs :data-sw-text])]
    (let [t (get-data d v)
          c (if (.endsWith v "-html") (h/as-hickory (h/parse (java.net.URLDecoder/decode t))) t)]
      (-> hick
          (assoc :content [c])
          (update-in [:attrs] dissoc :data-sw-text)))
    hick))

(defn- inject-attr
  [hick d]
  (let [attrs (:attrs hick)
        s-attrs (select-keys attrs (filter #(and (silk-attr? %) (not (= :data-sw-text %)) (not (= :data-sw-content %))) (keys attrs)))
        n-attrs (into (sorted-map) (for [[k v] s-attrs] {(keyword (last (silk-attr? k))) (get-data d v)}))]
    (-> hick
       (update-in [:attrs] merge n-attrs)
       (update-in [:attrs] #(apply dissoc %1 %2) (keys s-attrs)))))

(defn- data-level
  "Data level based on deepest data-sw-* value .e.g items.childs"
  [hick]
  (let [list1 (sw/map-content hick #(when (or (= hick %) (not (repeating-tag? (:tag %)))) %))
        attrs (spec/select (spec/walker #(some (fn [k] (silk-attr? (first k))) (:attrs %))) list1)
        avals (flatten (map #(vals (:attrs %)) attrs))]
    (when-let [deepest (last (sort-by #(count (re-seq #"\." %)) avals))]
      (seq (map #(keyword %) (drop-last (split deepest #"\.")))))))

(defn- inject
  [hick data data-pos]
  (def drill? false)
  (sw/map-content hick (fn [h]
    (when (not (or (= hick h) (repeating-tag? (:tag h)))) (def drill? true))
    (if (and drill? (repeating-tag? (:tag h)))
      (let [p (if-let [dl (data-level h)] (conj data-pos (last dl)) data-pos)
            d (get-in data p)]
        (cond
          (= (count d) 0)      (assoc h :content [""])
          (not (= p data-pos)) (assoc h :content (flatten (map-indexed (fn [i _] (:content (inject h data (conj p i)))) d)))
          :else                h))
      (if-let [dl-data (when (or (map? h) drill?) (get-in data data-pos))]
        (-> h
            (inject-text dl-data)
            (inject-attr dl-data))
        h)))))

(defn- source
  [hick]
  (let [data (sf/slurp-data (get-in hick [:attrs :data-sw-source])
                            (get-in hick [:attrs :data-sw-sort])
                            (get-in hick [:attrs :data-sw-dir])
                            (get-in hick [:attrs :data-sw-limit]))
        h (update-in hick [:attrs] dissoc :data-sw-source :data-sw-sort :data-sw-sort-dir :data-sw-limit)]
   (cond
     (map? data)        (inject h [data] [0])
     (= (count data) 0) (assoc h :content [""])
     :else              (spec/transform
                          (spec/walker #(repeating-tag? (:tag %)))
                          #(assoc % :content (flatten (map-indexed (fn [i _] (:content (inject % data [i]))) data)))
                          h))))

;; =============================================================================
;; Data transformations
;; =============================================================================

(defn process-data
  "Looks for data-sw-source and injects into it"
  [hick]
  (spec/transform (spec/walker #(get-in % [:attrs :data-sw-source])) #(source %) hick))
