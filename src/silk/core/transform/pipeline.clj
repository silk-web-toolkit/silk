(ns silk.core.transform.pipeline
  "Pipeline abstractions.
   Principally view driven."
  (:require [hickory.render :as hr]
            [silk.core.transform.component :as sc]
            [silk.core.transform.data :as sd]
            ; [silk.core.transform.element :as sel]
            [silk.core.transform.preprocess :as pre]
            ; [silk.core.transform.postprocess :as post]
            [silk.core.transform.view :as sv]))

;; =============================================================================
;; Pipeline abstraction functions, see namespace comment
;; =============================================================================

(defn view-pipline->
  "Combines views, templates and components "
  []
  (->> (sv/template-wrap->)
       (map #(assoc % :content (sc/process-components (:content %))))))

(defn data-detail-pipeline->
  "Transform data in a pipeline suitable for creating detail pages for silk
   content based directory contents."
  [path tpl]
  (->> (sv/template-wrap-detail-> {:path path :template tpl})
       (map #(assoc % :content (sc/process-components (:content %))))))

(defn gen-nav-data-pipeline->
  "Transform data in a pipeline into a data source edn files.
   Persists special edn files for component creation; menu, physical sitemap.
   Reads semantic markup from views."
  [payload]
  (pre/preprocess-> payload)
  payload)

(defn inject-data-pipeline->
  "Reads data source and injects data into markup"
  [payload]
  (->> payload
      (map #(assoc % :content (sd/process-data (:content %))))))

(defn html-pipeline->
  "Relativisation of uri's allows different behaviours across different
   intended environments & converts Hickory into HTML"
  [payload live?]
  (->> payload
      (map #(assoc % :content (hr/hickory-to-html (:content %)))))
      ;  (map #(sc/process-components false %))
      ;  (map #(sel/relativise-attrs :link :href % live?))
      ;  (map #(sel/relativise-attrs :img :src % live?))
      ;  (map #(sel/relativise-attrs :script :src % live?))
      ;  (map #(sel/relativise-attrs :a :href % live?))
      ;  (map #(sel/relativise-attrs :form :action % live?))
      )

(defn text-pipeline->
  "Gets the text content from each view"
  [payload]
  nil)
  ; (post/get-text-> payload))
