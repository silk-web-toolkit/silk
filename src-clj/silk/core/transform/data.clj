(ns silk.core.transform.data
  "Data related transformations.  "
  (:require [com.rpl.specter :as spec]
            [silk.core.input.file :as sf]
            [silk.core.common.core :as cr]))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- source
  [hick]
  (let [data (sf/slurp-data (get-in hick [:attrs :data-sw-source])
                            (get-in hick [:attrs :data-sw-sort])
                            (get-in hick [:attrs :data-sw-sort-dir])
                            (get-in hick [:attrs :data-sw-limit]))
        h (update-in hick [:attrs] dissoc :data-sw-source :data-sw-sort :data-sw-sort-dir :data-sw-limit)]
   (cond
     (map? data)        (cr/inject-in h [data] [0])
     (= (count data) 0) (assoc h :content [""])
     :else              (spec/transform
                          (spec/walker #(cr/repeating-tag? %))
                          #(assoc % :content (flatten (map-indexed (fn [i _] (:content (cr/inject-in % data [i]))) data)))
                          h))))

;; =============================================================================
;; Data transformations
;; =============================================================================

(defn process-data
  "Looks for data-sw-source and injects into it"
  [hick]
  (spec/transform (spec/walker #(get-in % [:attrs :data-sw-source])) #(source %) hick))
