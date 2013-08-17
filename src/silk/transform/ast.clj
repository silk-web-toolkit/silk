(ns silk.transform.ast
  "AST transformation."
  (:require [clojure.walk :as walk]
            [me.raynes.laser :as l]
            [silk.input.env :as se]
            [silk.input.ast :as ds]
            [silk.input.data :as dt]
            [silk.transform.path :as sp]))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn extension
  [p]
  (subs p (+ 1 (.lastIndexOf p "."))))

(defn detail-write
  [val attr ext]
  (if (and (= attr :href) (= (extension val) "edn"))
    (let [rel (sp/relativise-> (str se/pwd se/fs "data" se/fs) val)]
      (sp/update-extension rel ext))
    val))

(defn- text-write
  [node datum attrib]
  (if-let [attr (keyword (attrib (:attrs node)))]
    (if-let [result (dt/datum-extract datum attr)]
      (assoc node :content [result])
      node)
    node))

;; todo: final param is a result of proto code (POC)
(defn- attr-write
  [node datum dattr attr]
  (let [val (keyword (dattr (:attrs node)))]
    (if (contains? (:attrs node) attr)
      (if-let [result (dt/datum-extract datum val)]
        (assoc-in node [:attrs attr] (detail-write result attr "html"))
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
    (let [text-ins (text-write node (first data) :data-sw-text)
          text-href (attr-write text-ins (first data) :data-sw-href :href)
          text-src (attr-write text-href (first data) :data-sw-src :src)
          text-class (attr-write text-src (first data) :data-sw-class :class)
          text-title (attr-write text-class (first data) :data-sw-title :title)]
      text-title)))

(defn repeat-component
  [data]
  (fn [node] (map #(repeated-transform node %) data)))

(defn write-template-class
  [t]
  (l/add-class
   (str "silk-template-" (or (:content (:attrs (first t))) "default"))))
