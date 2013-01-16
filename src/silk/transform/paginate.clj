(ns silk.transform.paginate
  "Pagination related transformations.
   Note our payload is typically something like pipe-data
   see input.pipeline."
   (:use [taoensso.timbre :as timbre :only (trace debug info warn error)]))

;; =============================================================================
;; Utility functions
;; =============================================================================

(defn page-2-offset->
  [p l]
  (if (= p 1) 
    0 
    (* (dec p) l)))

(defn offset-2-page->
  [o l]
  (if (= o 0)
    1
    (inc (/ o l))))


;; =============================================================================
;; Payload transformation functions, see namespace comment
;; =============================================================================

(defn paginate-data-> 
  [d o l]
  (take l (drop o d)))

(defn page-offset->
  [o m]
  (assoc-in m [:page :offset] o))

(defn page-limit->
  [l m]
  (assoc-in m [:page :limit] l))

(defn page-total->
  [t m]  
  (assoc-in m [:page :total] t))

(defn data->
  [d o l m]
  (debug (str "d sample : " (vec (take 5 d))))
  (debug (str "o : " o))
  (debug (str "l : " l))
  (debug (str "m : " m))
  (assoc m :data (paginate-data-> d o l)))

(defn paginate-pipeline->
  "Combinatorial paginated pipeline transformer."
  [o l t d m]
  (debug "Entering paginate-pipeline combinatorial transformer")
  (->> m
       (page-offset-> o)
       (page-limit-> l)
       (page-total-> t)
       (data-> d o l)))
