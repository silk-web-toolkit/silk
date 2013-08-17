(ns silk.transform.component
  "Component related transformations.  "
  (:require [clojure.java.io :refer [file]]
            [clojure.walk :as walk]
            [me.raynes.laser :as l]
            [silk.input.ast :as ds]
            [silk.input.env :as se]
            [silk.input.file :as sf]
            [silk.transform.ast :as tx]
            [silk.transform.path :as sp])
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
        (reverse (sort-by (keyword sort) (sf/get-data-meta res)))
        (sf/get-data-meta res)))
    '()))

;; fatigued, will abstract this out later :-(
(defn get-component-datasource-tree
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
               ;;[rl {:type :element :tag :ul :content (:content datum)}]
               [(update-in rl [:content] into (:content datum))])]
    (-> datum
        (assoc :type :element :tag :ul :content cont))))

(defn- transcend-file
  [datum]
  (let [rel (sp/relativise-> (str se/pwd se/fs "data" se/fs) (:path datum))
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

;; todo: only handles two types of repeating element; tr and li - very proto code (POC)
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
