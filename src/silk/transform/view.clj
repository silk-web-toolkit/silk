(ns silk.transform.view
  "View related transformations.
   Principally view driven."
  (:require [me.raynes.laser :as l]
            [silk.input.env :as se]
            [silk.input.file :as sf]
            [silk.input.file :as sf]
            [silk.transform.path :as sp])
  (:use [clojure.string :only [split]]))

;; =============================================================================
;; Payload transformation functions, see namespace comment
;; =============================================================================

(defn- view-inject
  [v]
  (let [parsed-view (l/parse v)
        meta-template (l/select parsed-view
                       (l/and (l/element= :meta) (l/attr= :name "template")))
        template (if-not (nil? (first meta-template))
                   (sf/template
                    (str (:content (:attrs (first meta-template))) ".html"))
                   (sf/template "default.html"))]
    {:path (sp/relativise-> se/views-path (.getPath v))
     :content (l/document
                (l/parse template)
                (l/id="silk-view")
                  (l/replace
                    (l/select parsed-view
                      (l/child-of (l/element= :body) (l/any))))
                (l/element= :body)
                  (l/add-class 
                    (or
                      (:content (:attrs (first meta-template)))
                      "default"))
                (l/element= :body)
                  (l/add-class (first (split (.getName v) #"\."))))}))

(defn template-wrap->
  []
  (let [views (sf/get-views)]
    (map #(view-inject %) views)))
