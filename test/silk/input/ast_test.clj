(ns silk.input.ast-test
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [me.raynes.laser :as l]
            [hickory.core :as hickory]
            [hickory.zip :as hzip]
            [silk.input.ast :as sel]))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn create-frag
  [frag-str]
  (-> frag-str
      hickory/parse-fragment
      first
      hickory/as-hickory
      hzip/hickory-zip))

(defn fail-li-html-descendant []
  (create-frag "<ul><li><span data-sw-text=\"title\">placeholder</span></li></ul>"))

(defn pass-li-html-descendant []
  (create-frag ""))

(defn fail-li-html-attr []
  (create-frag "<span>placeholder</span>"))

(defn pass-li-html-attr []
  (create-frag "<span data-sw-text=\"title\">placeholder</span>"))


;; =============================================================================
;; Test facts
;; =============================================================================

(facts "about writeable?"
       ((sel/writeable?) (fail-li-html-attr)) => false?
       ((sel/writeable?) (pass-li-html-attr)) => true?)

(facts "about singular?"
       ((sel/singular?) (fail-li-html-descendant)) => false?
       ((sel/singular?) (pass-li-html-attr)) => true?)
