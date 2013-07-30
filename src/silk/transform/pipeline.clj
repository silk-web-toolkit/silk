(ns silk.transform.pipeline
  "Pipeline abstractions.
   Principally view driven."
  (:require [me.raynes.laser :as l]
            [silk.input.env :as se]
            [silk.input.file :as sf]
            [silk.input.file :as sf]
            [silk.transform.component :as sc]
            [silk.transform.element :as sel]
            [silk.transform.view :as sv]
            [silk.transform.path :as sp]))

;; =============================================================================
;; Pipeline abstraction functions, see namespace comment
;; =============================================================================

(defn view-driven-pipeline->
  "Transform data in a pipeline suitable for the majority of standard
   view presentation cases, including template wrapping, component injection
   and relativisation of uri's.
   mode enables different behaviours across different intended environments."
  [mode]
  (->> (sv/template-wrap->)
       (map #(sc/process-components %))
       (map #(sc/process-components %))
       (map #(sel/relativise-attrs :link :href % mode))
       (map #(sel/relativise-attrs :img :src % mode))
       (map #(sel/relativise-attrs :script :src % mode))
       (map #(sel/relativise-attrs :a :href % mode))))

(defn data-detail-pipeline->
  "Transform data in a pipeline suitable for creating detail pages for silk
   content based directory contents."
  [p tpl mode]
  (->> (sv/template-wrap-detail-> {:path p :template tpl})
       (map #(sc/process-components %))
       (map #(sc/process-components %))
       (map #(sel/relativise-attrs :link :href % mode))
       (map #(sel/relativise-attrs :img :src % mode))
       (map #(sel/relativise-attrs :script :src % mode))
       (map #(sel/relativise-attrs :a :href % mode))))
