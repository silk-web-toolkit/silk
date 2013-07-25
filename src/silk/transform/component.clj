(ns silk.transform.component
  "Component related transformations.  "
  (:require [clojure.java.io :refer [file]]
            [me.raynes.laser :as l]
            [silk.input.env :as se]
            [silk.input.file :as sf]
            [silk.ast.select :as sel]
            [clojure.walk :as walk]
            [clojure.edn :as edn])
  (:use [clojure.string :only [split]]))

;; =============================================================================
;; Helper functions
;; =============================================================================

;; TODO: very proto code (POC)
(defn- get-dynamic-attribs
  []
  [:data-sw-text :data-sw-href :data-sw-class :data-sw-src :data-sw-title])


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
  (let [text-ins (text-write node datum :data-sw-text)
        text-href (attr-write text-ins datum :data-sw-href :href)
        text-src (attr-write text-href datum :data-sw-src :src)
        text-class (attr-write text-src datum :data-sw-class :class)
        text-title (attr-write text-class datum :data-sw-title :title)]
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
                      (sel/repeating?) (repeat-component data)
                      (sel/singular?) (single-component data))))
      (first markup))))


;; =============================================================================
;; Component transformations, see namespace comment
;; =============================================================================

(defn process-components
  [t]
  (let [comps (l/select (l/parse (:content t)) (l/attr? "data-sw-component"))
        comp-ids (map #(select-keys (:attrs %) [:data-sw-component :data-sw-source :data-sw-sort]) comps)]
    (assoc t :content
      (reduce
      (fn [c i]
        (l/document
          (l/parse c)
          (l/attr= "data-sw-component" (:data-sw-component i))
          (l/replace (build-component i))))
      (:content t)
      (seq comp-ids)))))
