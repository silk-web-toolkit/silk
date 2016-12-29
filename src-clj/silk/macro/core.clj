(ns silk.macro.core
  (:refer-clojure :exclude [slurp]))

(defmacro slurp [f] (clojure.core/slurp f))
