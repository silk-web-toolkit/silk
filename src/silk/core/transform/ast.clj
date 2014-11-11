(ns silk.core.transform.ast
  "AST transformation."
  (:require [clojure.walk :as walk]
            [me.raynes.laser :as l]
            [silk.core.input.env :as se]
            [silk.core.input.ast :as ds]
            [silk.core.input.data :as dt]
            [silk.core.transform.path :as sp])
  (:use [clojure.string :only [lower-case join split]])
  (:import java.io.File))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- detail-write
  [val attr ext]
  (if (and (= attr :href) (= (sp/extension (first (split val #"#"))) "edn"))
    (let [rel (sp/relativise-> (se/project-data-path) val)
          id (if (.contains val "#") (subs val (.indexOf val "#")) "")]
      (str (sp/update-extension rel ext) id))
    val))

(defn- write-text->
  [node datum attrib]
  (if-let [attr (keyword (attrib (:attrs node)))]
    (if-let [result (dt/datum-extract datum attr)]
      (if (.contains (name attr) "-html")
        (let [frag (l/fragment (l/parse-fragment result))]
          (assoc node :content frag))
        (assoc node :content [result]))
      node)
    node))

(defn- datum-keys
  "Return a key out of the node attributes unless we are working with an href
   attribute which has special contextual possibilities ie '#anchor'."
  [node dattr attr]
  (if-let [keys (dattr (:attrs node))]
    (if (= attr :href)
      (map keyword (split keys #"#"))
      (map keyword (split keys #" ")))
    (map keyword keys)))

(defn- datum-values
  "Handle results from href as a special case, there are contextual
   possibilities like prepending with a '#' or 'view.html#'."
  [node datum dattr attr vals]
  (if (= attr :href)
    (join "#" (map #(dt/datum-extract datum %) vals))
    (join " " (map #(dt/datum-extract datum %) vals))))

;; todo: final param is a result of proto code (POC)
(defn- write-attr->
  [node datum dattr attr]
  (let [vals (datum-keys node dattr attr)]
    (if (contains? (:attrs node) dattr)
      (if-let [result (datum-values node datum dattr attr vals)]
        (let [dw (detail-write result attr "html")
              res (if (= attr :parent) (first (split (.getName (File. dw)) #"\.")) dw)]
          (assoc-in node [:attrs attr] (detail-write res attr "html")))
        node)
      node)))

;; todo: very proto code (POC)
(defn- transcend
  [node datum]
  (def n node)
  (doseq [f
    (filter #(.startsWith (lower-case (name (first %))) "data-sw-") (:attrs n))]
    (let [k1 (first f)
          k2 (keyword (subs (name k1) 8))]
      (if (= (lower-case (name k1)) "data-sw-text")
        (def n (write-text-> n datum k1))
        (def n (write-attr-> n datum k1 k2)))))
  n)

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
  (fn [node] (transcend node (first data))))

(defn repeat-component
  [data]
  (fn [node] (map #(repeated-transform node %) data)))

(defn write-template-class
  [t]
  (l/add-class
    (str "silk-template-" (or (:content (:attrs (first t))) "default"))))
