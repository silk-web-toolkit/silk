(ns silk.core.common.core
  #?(:clj (:require [clojure.string :refer [split]]
            [hickory.core :as h]
            [com.rpl.specter :as spec]
            [silk.core.common.walk :as sw]
            [silk.core.input.env :as se]
            [silk.core.transform.path :as sp]
            [markdown.core :as md])
     :cljs (:require [clojure.string :refer [split]]
                     [com.rpl.specter :as spec]
                     [hickory.core :as h]
                     [silk.core.common.walk :as sw]
                     [markdown.core :as md])))

(defn repeating-tag? [hick] (some #(= (:tag hick) %) [:ul :ol :tbody]))

(defn silk-attr? [k] (re-find #"data-sw-(\S+)" (name k)))

(defn- rich-empty? [v]
  (cond
    (boolean? v) (not v)
    (number? v)  false
    :else        (empty? v)))

(defn get-data
  [project d v]
  (let [ks (map keyword (split v #"\."))
        r  (get-in d ks)]
    #?(:clj (if (= (last ks) :sw/path)
               (sp/update-extension (sp/relativise-> (se/project-data-path project) r) "html")
              r))
    #?(:cljs r)))

(defn inject-when
  [hick project d]
  (let [attrs (:attrs hick)
        handler (fn [key]
                  (let [field-value (get-in hick [:attrs key])
                        data-value (get-data project d field-value)]
                    (if (or (and (= key :data-sw-when) (rich-empty? data-value))
                            (and (= key :data-sw-when-not) (not (rich-empty? data-value))))
                      {:type :comment}
                      (update-in hick [:attrs] dissoc key))))]
    (cond
      (contains? attrs :data-sw-when)     (handler :data-sw-when)
      (contains? attrs :data-sw-when-not) (handler :data-sw-when-not)
      :else                               hick)))

(defn inject-text
  [hick project d]
  (if-let [v (get-in hick [:attrs :data-sw-text])]
    (let [t (str (get-data project d v))
          c (cond
              (.endsWith v "-html") (mapv h/as-hickory (h/parse-fragment (java.net.URLDecoder/decode t "UTF-8")))
              (.endsWith v "-md")   (mapv h/as-hickory (h/parse-fragment (md/md-to-html-string t)))
              :else                 [t])]
      (-> hick
          (assoc :content (or (not-empty c) [""]))
          (update-in [:attrs] dissoc :data-sw-text)))
    hick))

(defn inject-attr
  [hick project d]
  (let [attrs (:attrs hick)
        s-attrs (select-keys attrs (filter #(and (silk-attr? %) (not (= :data-sw-text %)) (not (= :data-sw-content %))) (keys attrs)))
        n-attrs (into (sorted-map) (for [[k v] s-attrs] {(keyword (last (silk-attr? k))) (str (get-data project d v))}))]
    (-> hick
       (update-in [:attrs] merge n-attrs)
       (update-in [:attrs] #(apply dissoc %1 %2) (keys s-attrs)))))

(defn data-level
  "Data level based on deepest data-sw-* value .e.g items.childs"
  [hick]
  (let [list1 (sw/map-content hick #(when (or (= hick %) (not (repeating-tag? %))) %))
        attrs (spec/select (spec/walker #(some (fn [k] (silk-attr? (first k))) (:attrs %))) list1)
        avals (flatten (map #(vals (:attrs %)) attrs))]
    (when-let [deepest (last (sort-by #(count (re-seq #"\." %)) avals))]
      (seq (map #(keyword %) (drop-last (split deepest #"\.")))))))

(defn remove-item
  [list item]
  (let [[n m] (split-with (partial not= item) list)]
    (vec (concat n (rest m)))))

(defn flatten-in
  [data keys]
  (loop [d data ks keys d2 data ks2 [(first keys)]]
    (let [k (first ks)]
      (cond
        (number? k)  (recur
                       (if (= (count ks2) 1)
                         (nth d2 (first ks2))
                         (assoc-in d (drop-last ks2) (get d2 k)))
                       (next ks)
                       (nth d2 k)
                       (conj (remove-item ks2 k) (fnext ks)))
        (keyword? k) (recur
                       (assoc-in d ks2  (get d2 k))
                       (next ks)
                       (get d2 k)
                       (conj ks2 (fnext ks)))
        :else        d))))

(defn inject-in
  [project hick data ks]
  (sw/map-content hick (fn [h]
    (cond
      (= hick h)         h
      (repeating-tag? h) (let [k (if-let [dl (data-level h)] (conj ks (last dl)) ks)
                               d (get-in data k)]
                           (cond
                             (= (count d) 0) (assoc h :content [""])
                             (not (= k ks))  (assoc h :content (flatten (map-indexed (fn [i _] (:content (inject-in project h data (conj k i)))) d)))
                             :else           h))
      :else              (let [dl-data (flatten-in data ks)]
                           (-> h
                               (inject-when project dl-data)
                               (inject-text project dl-data)
                               (inject-attr project dl-data)))))))

(defn vec-it
  [m]
  (cond
    (map? m)                      (into {} (for [[k v] m] {k (vec-it v)}))
    (or (sequential? m) (set? m)) (mapv vec-it m)
    :else                         m))

(defn sort-it
  "Default to descending sort"
  [data sort direc]
  (cond
    (nil? sort)           (sort-by :sw/path data)
    (nil? direc)          (sort-by (keyword sort) data)
    (= direc "ascending") (sort-by (keyword sort) data)
    :else                 (reverse (sort-by (keyword sort) data))))

(defn as-seq [nodes] (for [i (range (. nodes -length))] (.item nodes i)))

(defn splice-hick-with-data
  "Looks for data-sw-source and injects into it"
  ([template data] (splice-hick-with-data "" template data))
  ([project template data]
  (let [sort  (get-in template [:attrs :data-sw-sort])
        direc (get-in template [:attrs :data-sw-sort-dir])
        limit (get-in template [:attrs :data-sw-limit])
        h     (update-in template [:attrs] dissoc :data-sw-source :data-sw-sort :data-sw-sort-dir :data-sw-limit)]
    (cond
      (map? data)        (inject-in project h [data] [0])
      (= (count data) 0) (assoc h :content [""])
      :else              (let [s-data (sort-it data sort direc)
                               l-data (if limit
                                        (vec-it (take (Integer. (re-find  #"\d+" limit)) s-data))
                                        (vec-it s-data))]
                           (spec/transform
                             (spec/walker #(repeating-tag? %))
                             #(assoc % :content (flatten (map-indexed (fn [i _] (:content (inject-in project % l-data [i]))) l-data)))
                             h))))))
