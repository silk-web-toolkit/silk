(ns silk.core.transform.pipeline
  "Pipeline abstractions.
   Principally view driven."
  (:require [me.raynes.laser :as l]
            [silk.core.input.env :as se]
            [silk.core.input.file :as sf]
            [silk.core.transform.component :as sc]
            [silk.core.transform.element :as sel]
            [silk.core.transform.preprocess :as pre]
            [silk.core.transform.postprocess :as post]
            [silk.core.transform.view :as sv]
            [silk.core.transform.path :as sp]))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- pre-process
  [payload mode]
  (->> payload
       (map #(sc/process-components true %))))

(defn- process
  [payload mode]
  (->> payload
       (map #(sc/process-components false %))
       (map #(sel/relativise-attrs :link :href % mode))
       (map #(sel/relativise-attrs :img :src % mode))
       (map #(sel/relativise-attrs :script :src % mode))
       (map #(sel/relativise-attrs :a :href % mode))
       (map #(sel/relativise-attrs :form :action % mode))))

;; =============================================================================
;; Pipeline abstraction functions, see namespace comment
;; =============================================================================

(defn preprocessor->
  "Transform data in a pipeline into a data source edn files.
   Persists special edn files for component creation; menu, physical sitemap.
   Reads semantic markup from views."
  [mode]
  (pre/preprocess-> (pre-process (sv/template-wrap->) mode)))

(defn view-driven-pipeline->
  "Transform data in a pipeline suitable for the majority of standard
   view presentation cases, including template wrapping, component injection
   and relativisation of uri's.
   mode enables different behaviours across different intended environments."
  [mode]
  (process (sv/template-wrap->) mode))

(defn data-detail-pipeline->
  "Transform data in a pipeline suitable for creating detail pages for silk
   content based directory contents."
  [p tpl mode]
  (process (sv/template-wrap-detail-> {:path p :template tpl}) mode))

(defn text-pipeline->
  ""
  [items]
  (post/get-text-> items))
