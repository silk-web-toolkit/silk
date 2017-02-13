(ns silk.core.test-server
  (:require [hickory.core :refer :all]
            [silk.core.server :as sv]
            [expectations :refer :all]))

(def some-data [{:title "A test title"}])

(let [f "test-data/html-sample.html"
      spun (sv/spin-component-with-data f "flibble" some-data)]
      (println "spun : " spun)
  (expect #"A test title" spun))

(let [f "test-data/html-source-sample.html"
      spun (sv/spin-components f)]
      (println "spun : " spun)
  (expect #"A test title2" spun))
