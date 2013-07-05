(ns silk.transform.component
  "Component related transformations.  "
  (:require [clojure.java.io :refer [file]]
            [me.raynes.laser :as l]
            [silk.input.env :as se]
            [silk.input.file :as sf])
  (:use [clojure.string :only [split]]))

;; =============================================================================
;; Component transformations, see namespace comment
;; =============================================================================

(defn- get-component-markup
  [path]
  (let [comp-str (str path ".html")
        lcp (str se/pwd se/fs "components" se/fs comp-str)
        c-path (if (.exists (file lcp)) (file lcp) (sf/component comp-str))
        parsed-comp (l/parse c-path)]
    (l/select parsed-comp
              (l/child-of (l/element= :body) (l/any)))))

(defn- get-component-datasource
  [data-params]
  (let [source (:data-swt-source data-params)
        data (sf/get-data-meta source)]
    data))

(defn- build-component
  [comp-params]
  (let [path (:data-swt-component comp-params)
        raw-markup (l/parse-fragment (get-component-markup path))
        markup (filter #(= (type (first %)) clojure.lang.PersistentArrayMap) raw-markup)
        data (get-component-datasource comp-params)]
    ;; parse repeatable components (eat lists)
    (l/parse (l/to-html
              (l/at (first markup)
                    (l/attr? "data-swt-r") #(for [datum (vec data)]
                                        (-> %
                                            (assoc :content [(:name datum)]) )))))
    ;; parse singleton components (eat map entries)

    ))

(defn process-components
  [t]
  (let [comps (l/select (l/parse (:content t)) (l/attr? "data-swt-component"))
        comp-ids (map #(select-keys (:attrs %) [:data-swt-component :data-swt-source]) comps)]
    (assoc t :content
      (reduce
      (fn [c i]
        (l/document
          (l/parse c)
          (l/attr= "data-swt-component" (:data-swt-component i))
          (l/replace (build-component i))))
      (:content t)
      (seq comp-ids)))))
