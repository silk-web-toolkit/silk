(ns silk.core.transform.component
  "Component related transformations.  "
  (:require [clojure.java.io :refer [file]]
            [clojure.walk :as walk]
            [me.raynes.laser :as l]
            [me.rossputin.diskops :as do]
            [silk.core.input.ast :as ds]
            [silk.core.input.env :as se]
            [silk.core.input.data :as dt]
            [silk.core.input.file :as sf]
            [silk.core.transform.ast :as tx]
            [silk.core.transform.path :as sp])
  (:use [clojure.string :only [split]]))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- get-component-markup
  [path]
  (let [res (sf/component path)]
    (ds/body-content (l/parse res))))

(defn- get-component-datasource
  [data-params]
  (if-let [source (:data-sw-source data-params)]
    (let [res (sf/data source)]
      (if-let [sort (:data-sw-sort data-params)]
        (let [data (sf/get-data-meta res)
              enhanced (map #(merge % (dt/read-datum %)) data)]
          (reverse (sort-by (keyword sort) enhanced)))
        (sf/get-data-meta res)))
    '()))

;; fatigued, will abstract this out later :-(
(defn- get-component-datasource-tree
  [data-params]
  (if-let [source (:data-sw-source data-params)]
    (let [res (sf/data source)]
      (sf/get-data-meta-tree res))
    {}))

(defn- transcend-dir
  [datum]
  (let [rl {:type :element :tag :li :attrs {:class "folder"} :content [(:name datum)]}
        cont (if (every? #(= (:tag %) :li) (:content datum))
               [(update-in rl [:content] into [{:type :element :tag :ul :content (:content datum)}])]
               [(update-in rl [:content] into (:content datum))])]
    (-> datum
        (assoc :type :element :tag :ul :content cont))))

(defn- transcend-file
  [datum]
  (let [rel (sp/relativise-> (str (do/pwd) (do/fs) "data" (do/fs)) (:path datum))
        conv-p (sp/update-extension rel "html")
        cont {:type :element :tag :a :attrs {:href conv-p} :content (:content datum)}
        mod (assoc datum :type :element :tag :li :content [cont])]
    mod))

(defn- eval-element
  [datum]
  (cond
   (= (:node-type datum) :directory) (transcend-dir datum)
   (= (:node-type datum) :file) (transcend-file datum)
    :else datum))

(defn- map-walk
  [data]
  (walk/postwalk eval-element data))

(defn- build-component
  [comp-params]
  (let [path (:data-sw-component comp-params)
        raw-markup (get-component-markup path)]
    (if-let [tree (:data-sw-type comp-params)]
      (let [data (get-component-datasource-tree comp-params)
            walkin (map-walk data)]
        walkin)
      (let [data (get-component-datasource comp-params)]
        (if (seq data)
          (l/fragment (l/parse-fragment raw-markup)
                      (ds/repeating?) (tx/repeat-component data)
                      (ds/singular?) (tx/single-component data))
          raw-markup)))))

(defn- swap-component->
  [c i]
  (if (:data-sw-source i)
    (l/document
      (l/parse c)
      (l/and
        (l/attr= "data-sw-source" (:data-sw-source i))
        (l/attr= "data-sw-component" (:data-sw-component i)) )
      (l/replace (build-component i)))
    (l/document
      (l/parse c)
      (l/attr= "data-sw-component" (:data-sw-component i))
      (l/replace (build-component i)))))


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
