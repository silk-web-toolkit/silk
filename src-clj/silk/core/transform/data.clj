(ns silk.core.transform.data
  "Data related transformations.  "
  (:require [com.rpl.specter :as spec]
            [silk.core.input.file :as sf]
            [silk.core.common.core :as cr]))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- get-source [hick] (get-in hick [:attrs :data-sw-source]))

;; =============================================================================
;; Data transformations
;; =============================================================================

(defn process-data
  "Looks for data-sw-source and injects into it"
  [project hick]
  (spec/transform
    (spec/walker #(get-source %))
    #(cr/process-component-with-data project % (sf/slurp-data project (get-source %)))
    hick))
