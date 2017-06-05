(ns silk.core.transform.pipeline
  "Pipeline abstractions.
   Principally view driven."
  (:require [hickory.render :as hr]
            [silk.core.transform.component :as sc]
            [silk.core.transform.data :as sd]
            [silk.core.transform.element :as sel]
            [silk.core.transform.preprocess :as pre]
            [silk.core.transform.postprocess :as post]
            [silk.core.transform.view :as sv]))

;; =============================================================================
;; Pipeline abstraction functions, see namespace comment
;; =============================================================================

(defn view-pipline->
  "Combines views, templates and components "
  [project]
  (->> (sv/template-wrap-> project)
       (map #(assoc % :content (sc/process-components project (:content %))))))

(defn data-detail-pipeline->
  "Transform data in a pipeline suitable for creating detail pages for silk
   content based directory contents."
  [project path tpl]
  (->> (sv/template-wrap-detail-> project {:path path :template tpl})
       (map #(assoc % :content (sc/process-components project (:content %))))))

(defn gen-nav-data-pipeline->
  "Transform data in a pipeline into a data source edn files.
   Persists special edn files for component creation; menu, physical sitemap.
   Reads semantic markup from views."
  [payload]
  (pre/preprocess-> payload)
  payload)

(defn inject-data-pipeline->
  "Reads data source and injects data into markup"
  [payload project]
  (->> payload
      (map #(assoc % :content (sd/process-data project (:content %))))))

(defn html-pipeline->
  "Relativisation of uri's allows different behaviours across different
   intended environments & converts Hickory into HTML"
  [payload project live?]
  (let [tags {:link :href, :img :src, :script :src, :a :href, :form :action}]
    (->> payload
         (map #(sel/relativise-attrs project tags % live?))
         (map #(assoc % :content (hr/hickory-to-html (:content %)))))))

(defn text-pipeline->
  "Gets the text content from each view"
  [payload]
  (post/get-text-> payload))
