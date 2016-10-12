(ns silk.core.transform.data
  "Data related transformations.  "
  (:require [hickory.core :as h]
            [hickory.select :as hs]
            [com.rpl.specter :as spec]
            [clojure.java.io :refer [file]]
            [clojure.edn :as edn]
            ; [clojure.walk :as walk]
            ; [me.raynes.laser :as l]
            ; [silk.core.input.ast :as ds]
            [silk.core.input.env :as se]
            ; [silk.core.input.data :as dt]
            [silk.core.input.file :as sf]
            ; [silk.core.transform.ast :as tx]
            [silk.core.transform.coerce :as sc]
            [silk.core.transform.path :as sp])
  (:use [clojure.string :only [split]]
        [clojure.set    :only [rename-keys]]))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- sort->
  "Default to descending sort."
  [data sort dir]
  (let [ascending (sort-by (keyword sort) data)]
    (if-let [d dir]
      (if (= d "ascending") ascending (reverse ascending))
      ascending)))
;
; (defn- get-component-datasource
;   [{source :data-sw-source limit :data-sw-limit sort :data-sw-sort
;     dir :data-sw-sort-dir parent :data-sw-parent}]
;   (if-let [src (if parent (str "protean-api/" parent "/" source) source)]
;     (let [res (sf/data src)
;           data (sf/get-data-meta res)
;           sorted (if-let [srt sort] (sort-> data srt dir) data)]
;       (if-let [lim limit] (take (sc/int-> lim) sorted) sorted))
;     '()))
;
; ;; fatigued, will abstract this out later :-(
; (defn- get-component-datasource-tree
;   [data-params]
;   (if-let [source (:data-sw-source data-params)]
;     (let [res (sf/data source)]
;       (sf/get-data-meta-tree res))
;     {}))
;
; (defn- transcend-dir
;   [datum]
;   (let [rl {:type :element :tag :li :attrs {:class "folder"} :content [(:name datum)]}
;         cont (if (every? #(= (:tag %) :li) (:content datum))
;                [(update-in rl [:content] into [{:type :element :tag :ul :content (:content datum)}])]
;                [(update-in rl [:content] into (:content datum))])]
;     (-> datum
;         (assoc :type :element :tag :ul :content cont))))
;
; (defn- transcend-file
;   [datum]
;   (let [rel (sp/relativise-> (se/project-data-path) (:path datum))
;         conv-p (sp/update-extension rel "html")
;         cont {:type :element :tag :a :attrs {:href conv-p} :content (:content datum)}
;         mod (assoc datum :type :element :tag :li :content [cont])]
;     mod))
;
; (defn- eval-element
;   [datum]
;   (cond
;    (= (:node-type datum) :directory) (transcend-dir datum)
;    (= (:node-type datum) :file) (transcend-file datum)
;     :else datum))
;
;
; (defn- prepare-keys [attrs]
;   (let [sk (select-keys attrs (ds/get-component-attribs))]
;     (if-let [p (:parent attrs)]
;       (assoc (assoc sk :data-sw-parent p) :data-sw-source (:data-sw-source attrs))
;       sk)))
;

; data-sw-* add as attriute *
; data-sw-content add as content
; use / to access path e.g. data-sw-content "item/name"
; li ol and tr should be treated as loops

(defn- repeating-tag? [t] (.contains [:ul :ol :tbody] t))

(defn- silk-attr? [k] (re-find #"data-sw-(\S+)" (name k)))

(defn- inject-text
  [hick d]
  (if-let [v (get-in hick [:attrs :data-sw-text])]
    (assoc hick :content [(str (get d (keyword v)))])
    hick))

(defn- inject-attr
  [hick d]
  (assoc hick :attrs (into (sorted-map)
    (for [[k v] (:attrs hick)]
      (if-let [k2 (keyword (last (silk-attr? k)))]
        {k2 (str (get d (keyword v)))}
        {k v})))))

(defn- inject-single
  [hick data]
  (spec/transform (spec/walker #(some (fn [k] (silk-attr? (first k))) (:attrs %)))
    (fn [h]
      (-> h
          (inject-text data)
          (update-in [:attrs] dissoc :data-sw-text)
          (inject-attr data)))
    hick))

(defn- inject-list
  [hick data]
  (spec/transform (spec/walker #(repeating-tag? (:tag %)))
    (fn [h]
      (assoc h :content
        (flatten (map #(:content %) (for [d data] (inject-single h d))))))
    hick))

(defn- source
  [hick]
  (let [src    (:data-sw-source (:attrs hick))
        param  (:data-sw-sort (:attrs hick))
        direc  (:data-sw-sort-dir (:attrs hick))
        edn    (edn/read-string (slurp (sf/data src)))
        sorted (if-let [p param] (sort-> edn p direc) edn)
        limit  (if-let [s (:data-sw-limit (:attrs hick))] (sc/parse-int s) (count sorted))
        data   (take limit sorted)]
   (-> hick
       (update-in [:attrs] dissoc :data-sw-source :data-sw-limit :data-sw-sort :data-sw-sort-dir)
       (inject-list data)
       (inject-single (first data)))))

;; =============================================================================
;; Data transformations
;; =============================================================================

(defn process-data
  "Looks for data-sw-source and injects into it"
  [hick]
  (spec/transform (spec/walker #(get-in % [:attrs :data-sw-source])) #(source %) hick))
