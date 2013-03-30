(ns silk.transform.entire
  "Full data set related transformations.
   Note our payload is typically something like paginate-payload
   see input.pipeline.")

;; =============================================================================
;; Payload transformation functions, see namespace comment
;; =============================================================================

(defn data->
  [d m]
  (assoc m :data d))
