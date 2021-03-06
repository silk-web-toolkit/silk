(ns silk.core.transform.component
  "Component related transformations.  "
  (:require [hickory.select :as hs]
            [com.rpl.specter :as spec]
            [silk.core.input.file :as sf]))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- load-comp
  [project comp-hickory]
  (let [path (:data-sw-component (:attrs comp-hickory))
        f (sf/component project path)
        c (-> (hs/select (hs/tag :body) (sf/hick-file f)) first :content)
        old (spec/select (spec/walker #(:data-sw-component %)) c)]
    ; Make sure the component does not call itself
    (if (some #(= (:data-sw-component %) path) old)
      (update-in comp-hickory [:attrs] dissoc :data-sw-component)
      (-> comp-hickory
          (update-in [:attrs] dissoc :data-sw-component)
          (assoc :content c)))))

;; =============================================================================
;; Component transformations
;; =============================================================================

(defn process-components
  "Adds components"
  [project hick]
  (let [n (spec/transform (spec/walker #(get-in % [:attrs :data-sw-component]))
                          #(load-comp project %)
                          hick)]
    (if (= hick n) n (process-components project n))))
