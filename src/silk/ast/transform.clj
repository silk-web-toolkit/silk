(ns silk.ast.transform
  "AST transformation."
  (:require [clojure.walk :as walk]
            [clojure.edn :as edn]
            [silk.ast.describe :as ds]))

;; =============================================================================
;; Helper functions
;; =============================================================================

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
  (if (seq (filter (set (keys (:attrs node))) (ds/get-dynamic-attribs)))
    (transcend node datum)
    node))

(defn- repeated-transform
  [node datum]
  (walk/postwalk #(eval-element % datum) node))


;; =============================================================================
;; AST transformations, see namespace comment
;; =============================================================================

(defn single-component
  [data]
  (fn [node]
    (text-write node (first data) :data-sw-text)))

(defn repeat-component
  [data]
  (fn [node] (map #(repeated-transform node %) data)))
