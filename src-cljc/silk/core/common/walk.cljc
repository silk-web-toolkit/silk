(ns silk.core.common.walk
  "Walks hick contents and transform")

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- keys-with-last-index
  [hick keys]
  (loop [k keys h hick indexes []]
    (if-let [fk (first k)]
      (recur (next k) (last (fk h)) (into indexes [fk (dec (count (fk h)))]))
      (vec (drop-last indexes)))))

(defn- update-hick
  [hick keys node]
  (update-in hick (keys-with-last-index hick keys) conj node))

;; =============================================================================
;; Hick Walkers
;; =============================================================================

(defn map-content
  [hick func]
  (let [fhick (func hick)]
    (loop [h (:content fhick) hl [:content] n nil nl nil r-hick (assoc fhick :content [])]
      (if (or (vector? h) (seq? h))
        (let [changed-hick (if (map? (first h)) (func (first h)) (first h))]
          (if-let [c (:content changed-hick)]
            (recur c
                   (conj hl :content)
                   (concat (next h) n)
                   (concat (for [i (next h)] hl) nl)
                   (update-hick r-hick hl (assoc changed-hick :content [])))
           (recur (next h)
                  hl
                  n
                  nl
                  (update-hick r-hick hl changed-hick))))
          (if n
            (recur (flatten (vector (first n)))
                   (flatten (vector (first nl)))
                   (next n)
                   (next nl)
                   r-hick)
            r-hick)))))
