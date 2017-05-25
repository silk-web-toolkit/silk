(ns silk.core.test-server
  (:require [silk.core.server :as sv]
            [expectations :refer :all]))

(def some-data [{:title "A test title"}])

(let [f "test-data/html-sample.html"
      spun (sv/spin-by-id f "flibble" some-data)]
      (println "spun : " spun)
  (expect #"A test title" spun))

(let [f "test-data/html-source-sample.html"
      spun (sv/spin-by-data-sw-source f)]
      (println "spun : " spun)
  (expect #"A test title2" spun))
