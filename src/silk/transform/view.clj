(ns silk.transform.view
  "View related transformations.
   Principally view driven."
  (:require [me.raynes.laser :as l]
            [silk.input.env :as se]
            [silk.input.ast :as ds]
            [silk.input.file :as sf]
            [silk.transform.ast :as tx]
            [silk.transform.path :as sp])
  (:use [clojure.string :only [split]]))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- get-template
  [t]
  (if-not (nil? (first t))
                   (sf/template
                    (str (:content (:attrs (first t))) ".html"))
                   (sf/template "default.html")))

(defn- view-inject
  [v]
  (let [parsed-view (l/parse v)
        meta-template (ds/template parsed-view)
        template (get-template meta-template)]
    {:path (sp/relativise-> se/views-path (.getPath v))
     :content (l/document
                (l/parse template)
                (l/attr? "data-sw-view")
                  (l/replace (ds/body-content parsed-view))
                (l/element= :body) (tx/write-template-class meta-template)
                (l/element= :body)
                  (l/add-class (str "silk-view-" (first (split (.getName v) #"\.")))))}))


;; =============================================================================
;; Payload transformation functions, see namespace comment
;; =============================================================================

(defn template-wrap->
  []
  (let [views (sf/get-views)]
    (map #(view-inject %) views)))

(defn template-wrap-detail->
  [{path :path template :template}]
  (let [wrapped (map #(view-inject %) (take (count path) (repeat template)))]
    (for [p path w wrapped]
      (let [rel-p (sp/relativise-> (str se/pwd se/fs "data" se/fs) (.getPath p))
            data-inj
            (l/document
             (l/parse (:content w))
             (l/attr? :data-sw-component)
             (l/attr :data-sw-source rel-p))]
        (assoc w :path rel-p :content data-inj)))))
