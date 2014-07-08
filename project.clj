(defproject silk-core "0.4.1-SNAPSHOT"
  :description "Silk static and dynamic publishing toolkit."
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.taoensso/timbre "1.5.2"]
                 [pathetic "0.4.0"]
                 [me.raynes/laser "1.1.1"]
                 [me.rossputin/diskops "0.2.0"]]
  :profiles {:dev {:dependencies [[midje "1.5.1"]]}}
  :plugins [[lein-midje "3.1.0"]]

  :aot :all)
