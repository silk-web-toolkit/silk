(ns silk.transform.validate
  "TODO: this is really an import validate pipeline, rename/reorg as such.
     seed-data, structural-analysis and field-analysis are more generic but
     tx-eligibility-analysis is fairly specific to an import.
   Validation related transformations.
   Note our payload is typically something like validate-payload
   see input.pipeline.")

;; =============================================================================
;; Validate transformation functions, see namespace comment
;; =============================================================================

(defn seed-data->
  [d m]
  (assoc m :seed-data d :seed-data-count (count d)))

;;(every? #(= 8 (count %)) data)
(defn structural-precog->
  "Validate structural integrity of seed-data according to a predicate p.
   Yields :invalid or :valid."
  [p m]
  (if p (assoc m :structural-precog :valid) m))

(defn field-precog->
  "TODO: refactor common elements with this and tx-eligibility-precog->.
   Validate specific data fields, partitioning given a function fn.
   Reduces :seed-data accordingly upon finding invalid entries.
   Yields :invalid, :partially-valid or :valid."
  [fn m]
  (if (= :valid (:structural-precog m))
    (let [fn-val (fn (:seed-data m))]
      (cond
        (empty? (last fn-val)) (assoc m :field-precog :valid)
        (empty? (first fn-val)) m
        (and (> (count (first fn-val)) 0) (> (count (last fn-val)) 0))
          (assoc m :field-precog :partially-valid
                   :seed-data (first fn-val)
                   :invalid-field-data (last fn-val))))
    m))

(defn tx-eligibility-precog->
  "TODO: refactor common elements with this and field-precog->.
   Validate if the system should accept the data, partitioning given a function fn.
   Reduces :seed-data accordingly upon finding invalid entries.
   Yields :invalid, :partially-valid or :valid."
  [fn c m]
  (if (= (:field-precog m) :invalid)
    m
    (let [fn-val (fn c (:seed-data m))]
      (cond
        (empty? (last fn-val)) (assoc m :tx-eligibility-precog :valid :seed-data (first fn-val))
        (empty? (first fn-val)) m
        (and (> (count (first fn-val)) 0) (> (count (last fn-val)) 0))
          (assoc m :tx-eligibility-precog :partially-valid
                   :seed-data (first fn-val)
                   :invalid-tx-data (last fn-val))))))
