(ns silk.transform.pipeline
  "Pipeline abstractions.
   Principally view driven."
  (:require [me.raynes.laser :as l]
            [silk.input.env :as se]
            [silk.input.file :as sf]
            [silk.input.file :as sf]
            [silk.transform.component :as sc]
            [silk.transform.view :as sv]
            [silk.transform.path :as sp]))

;; =============================================================================
;; Pipeline abstraction functions, see namespace comment
;; =============================================================================

(defn view-driven-pipeline->
  []
  (->> (sv/template-wrap->) 
       (map #(sc/process-components %))
       (map #(sc/process-components %))))
