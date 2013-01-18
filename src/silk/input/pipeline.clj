(ns silk.input.pipeline
  "TODO : rename ns to something better... (webapp ?)
   Generic inputs (payloads) within a transformation pipeline.")

(def pipe-data {:data []
                :page {:offset nil :limit nil :total nil}
                :search nil
                :uri nil})

;; Structural precog checks the integrity of the source as a unit
;;   :structural-precog (:invalid|:valid)
;; Field precog checks the validity specified elements of the data
;;   :field-precog (:invalid|:partially-valid|:invalid)
;; Transaction eligibility precog determines if the system should accept the data
;; based on what is already present in the systm
;;    :tx-eligibility-precog (:invalid|:partially-valid|:valid)
;; N.B. Our stance on validation is pessimistic by default, strict.
(def validate-payload {:seed-data []
                       :seed-data-count 0
                       :structural-precog :invalid
                       :field-precog :invalid
                       :invalid-field-data []
                       :invalid-tx-data []
                       :tx-eligibility-precog :invalid})
