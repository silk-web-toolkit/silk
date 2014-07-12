(ns silk.core.transform.ast
  "AST transformation."
  (:require [clojure.walk :as walk]
            [me.raynes.laser :as l]
            [me.rossputin.diskops :as do]
            [silk.core.input.env :as se]
            [silk.core.input.ast :as ds]
            [silk.core.input.data :as dt]
            [silk.core.transform.path :as sp]))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- detail-write
  [val attr ext]
  (if (and (= attr :href) (= (sp/extension val) "edn"))
    (let [rel (sp/relativise-> (str (do/pwd) (do/fs) "data" (do/fs)) val)]
      (sp/update-extension rel ext))
    val))

(defn- text-write
  [node datum attrib]
  (if-let [attr (keyword (attrib (:attrs node)))]
    (if-let [result (dt/datum-extract datum attr)]
      (assoc node :content [result])
      node)
    node))

(defn- datum-key
  "Return a key out of the node attributes unless we are working with an href
   attribute which has special contextual possibilities ie '#anchor'."
  [node dattr attr]
  (if-let [datum-key (dattr (:attrs node))]
    (if (= attr :href)
      (if (.contains datum-key "#")
        (keyword (subs datum-key (+ (.indexOf datum-key "#") 1)))
        (keyword datum-key))
      (keyword datum-key))
    (keyword datum-key)))

(defn- datum-value
  "Handle results from href as a special case, there are contextual
   possibilities like prepending with a '#' or 'view.html#'."
  [node datum dattr attr val]
  (let [result (dt/datum-extract datum val)]
    (if (= attr :href)
      (let [href (dattr (:attrs node))]
        (if (.contains href "#")
          (str (subs href 0 (+ (.indexOf href "#") 1)) result)
          result))
      result)))

;; todo: final param is a result of proto code (POC)
(defn- attr-write
  [node datum dattr attr]
  (let [val (datum-key node dattr attr)]
    (if (and (contains? (:attrs node) attr) (contains? (:attrs node) dattr))
      (if-let [result (datum-value node datum dattr attr val)]
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
        text-title (attr-write text-class datum :data-sw-title :title)
        text-id (attr-write text-title datum :data-sw-id :id)]
    text-id))

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
          text-title (attr-write text-class (first data) :data-sw-title :title)
          text-id (attr-write text-title (first data) :data-sw-id :id)]
      text-id)))

(defn repeat-component
  [data]
  (fn [node] (map #(repeated-transform node %) data)))

(defn write-template-class
  [t]
  (l/add-class
   (str "silk-template-" (or (:content (:attrs (first t))) "default"))))
