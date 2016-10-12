(ns silk.core.transform.coerce
  "Generic Optimus transformer library, elements of which are paralleled in
   protean.core, we may need to think about a coercion library sometime.")

(defn parse-int
  [s]
  (Integer. (re-find  #"\d+" s)))
