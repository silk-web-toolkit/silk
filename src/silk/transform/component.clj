(ns silk.transform.component
  "Component related transformations.  "
  (:require [clojure.java.io :refer [file]]
            [me.raynes.laser :as l]
            [silk.input.env :as se]
            [silk.input.file :as sf]
            [clojure.walk :as walk]
            [clojure.edn :as edn])
  (:use [clojure.string :only [split]]))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- get-dynamic-attribs
  []
  [:data-sw-rtext :data-sw-rhref :data-sw-rclass :data-sw-rsrc :data-sw-rtitle])


(defn- get-component-markup
  [path]
  (let [source (str path ".html")
        res (sf/quantum-resource source "components" se/components-path)]
    (l/select (l/parse res)
              (l/child-of (l/element= :body) (l/any)))))

(defn- get-component-datasource
  [data-params]
  (let [source (:data-sw-source data-params)
        res (sf/quantum-resource source "data" se/data-path)]
    (sf/get-data-meta res)))

(defn- enhance-datum-content
  [datum]
  (assoc datum :content (edn/read-string (slurp (:path datum)))))

(defn- datum-extract
  "Determine if the data item we want is in the datum, if not try supplementing
   by loading edn file content."
  [datum item]
  (or (item datum) (item (:content (enhance-datum-content datum)))))

(defn- text-write
  [node datum attrib]
  (if-let [attr (keyword (attrib (:attrs node)))]
    (if-let [result (datum-extract datum attr)]
      (assoc node :content [result])
      node)
    node))

;; todo: final param is a result of proto code (POC)
(defn- attr-write
  [node datum dattr attr]
  (let [val (keyword (dattr (:attrs node)))]
    (if (contains? (:attrs node) attr)
      (if-let [result (datum-extract datum val)]
        (assoc-in node [:attrs attr] result)
        node)
      node)))

;; todo: very proto code (POC)
(defn- transcend
  [node datum]
  (let [text-ins (text-write node datum :data-sw-rtext)
        text-href (attr-write text-ins datum :data-sw-rhref :href)
        text-src (attr-write text-href datum :data-sw-rsrc :src)
        text-class (attr-write text-src datum :data-sw-rclass :class)
        text-title (attr-write text-class datum :data-sw-rtitle :title)]
    text-title))

(defn- eval-element
  [node datum]
  (if (seq (filter (set (keys (:attrs node))) (get-dynamic-attribs)))
    (transcend node datum)
    node))

(defn- repeated-transform
  [node datum]
  (walk/postwalk #(eval-element % datum) node))

(defn- repeat-component
  [data]
  (fn [node] (map #(repeated-transform node %) data)))

(defn- single-component
  [data]
  (fn [node]
    (text-write node (first data) :data-sw-text)))

(defn- build-component
  [comp-params]
  (let [path (:data-sw-component comp-params)
        raw-markup (l/parse-fragment (get-component-markup path))
        markup (filter #(= (type (first %)) clojure.lang.PersistentArrayMap) raw-markup)
        data (get-component-datasource comp-params)]
    (if (seq data)
      (l/parse (l/to-html
                (l/at (first markup)
                      (l/attr? "data-sw-r") (repeat-component data)
                      (l/attr? "data-sw-text") (single-component data))))
      (first markup))))


;; =============================================================================
;; Component transformations, see namespace comment
;; =============================================================================

(defn process-components
  [t]
  (let [comps (l/select (l/parse (:content t)) (l/attr? "data-sw-component"))
        comp-ids (map #(select-keys (:attrs %) [:data-sw-component :data-sw-source]) comps)]
    (assoc t :content
      (reduce
      (fn [c i]
        (l/document
          (l/parse c)
          (l/attr= "data-sw-component" (:data-sw-component i))
          (l/replace (build-component i))))
      (:content t)
      (seq comp-ids)))))
