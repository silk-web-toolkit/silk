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
;; Helper functions
;; =============================================================================

(defn- template->
  [c]
  )



;; =============================================================================
;; Payload transformation functions, see namespace comment
;; =============================================================================

(defn- view-inject
  [v]
  (println (str "v is : " v))
  (let [parsed-view (l/parse v)
        meta-template (l/select parsed-view
                       (l/and (l/element= :meta) (l/attr= :name "template")))
        template (if-not (nil? (first meta-template))
                   (sf/template
                    (str (:content (:attrs (first meta-template))) ".html"))
                   (sf/template "default.html"))]
    (println (str "template is : " template))
    {:path (sp/relativise-> se/views-path (.getPath v))
     :content (l/document
                (l/parse template)
                (l/attr? "data-sw-view")
                  (l/replace
                    (l/select parsed-view
                      (l/child-of (l/element= :body) (l/any))))
                (l/element= :body)
                  (l/add-class
                    (str "silk-template-" (or
                      (:content (:attrs (first meta-template)))
                      "default")))
                (l/element= :body)
                  (l/add-class (str "silk-view-" (first (split (.getName v) #"\.")))))}))

(defn template-wrap->
  []
  (let [views (sf/get-views)]
    (map #(view-inject %) views)))

(defn template-wrap-detail->
  [payload]
  (println (str "payload is : " payload))
  (map #(view-inject %) (take (count (:path payload)) (repeat (:template payload)))))
