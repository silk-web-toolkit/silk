(ns silk.transform.view
  "View related transformations.
   Principally view driven."
  (:require [me.raynes.laser :as l]
            [silk.input.env :as se]
            [silk.input.file :as sf]
            [silk.input.file :as sf]
            [silk.transform.path :as sp]
            [silk.ast.select :as sel])
  (:use [clojure.string :only [split]]))

;; =============================================================================
;; Payload transformation functions, see namespace comment
;; =============================================================================

(defn- view-inject
  [v]
  (let [parsed-view (l/parse v)
        meta-template (sel/template parsed-view)
        template (if-not (nil? (first meta-template))
                   (sf/template
                    (str (:content (:attrs (first meta-template))) ".html"))
                   (sf/template "default.html"))]
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
