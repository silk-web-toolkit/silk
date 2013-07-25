(ns silk.transform.component
  "Component related transformations.  "
  (:require [clojure.java.io :refer [file]]
            [me.raynes.laser :as l]
            [silk.input.env :as se]
            [silk.input.file :as sf]
            [silk.ast.select :as sel]
            [silk.ast.transform :as tx]
            [silk.ast.describe :as ds])
  (:use [clojure.string :only [split]]))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- get-component-markup
  [path]
  (let [source (str path ".html")
        res (sf/quantum-resource source "components" se/components-path)]
    (l/select (l/parse res)
              (l/child-of (l/element= :body) (l/any)))))

(defn- get-component-datasource
  [data-params]
  (if-let [source (:data-sw-source data-params)]
    (let [res (sf/quantum-resource source "data" se/data-path)]
      (if-let [sort (:data-sw-sort data-params)]
        (reverse (sort-by (keyword sort) (sf/get-data-meta res)))
        (sf/get-data-meta res)))
    '()))

;; todo: handle singular attribute writing - very proto code (POC)
;; todo: only handles two types of repeating element; tr and li - very proto code (POC)
(defn- build-component
  [comp-params]
  (let [path (:data-sw-component comp-params)
        raw-markup (l/parse-fragment (get-component-markup path))
        markup (filter #(= (type (first %)) clojure.lang.PersistentArrayMap) raw-markup)
        data (get-component-datasource comp-params)]
    (if (seq data)
      (l/parse (l/to-html
                (l/at (first markup)
                      (sel/repeating?) (tx/repeat-component data)
                      (sel/singular?) (tx/single-component data))))
      (first markup))))

(defn- swap-component->
  [c i]
  (l/document
   (l/parse c)
   (l/attr= "data-sw-component" (:data-sw-component i))
   (l/replace (build-component i))))


;; =============================================================================
;; Component transformations, see namespace comment
;; =============================================================================

(defn process-components
  [t]
  (let [comps (l/select (l/parse (:content t)) (l/attr? "data-sw-component"))
        comp-ids (map #(select-keys (:attrs %) (ds/get-component-attribs)) comps)]
    (assoc
      t
      :content
      (reduce #(swap-component-> %1 %2) (:content t) (seq comp-ids)))))
