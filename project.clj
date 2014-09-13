(defproject silk "0.6.0-SNAPSHOT"
  :description "Silk static and dynamic publishing toolkit."
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.taoensso/timbre "1.5.2"]
                 [pathetic "0.4.0"]
                 [me.raynes/laser "1.1.1"]
                 [me.rossputin/diskops "0.2.0"]
                 [org.clojars.zcaudate/watchtower "0.1.2"]
                 [io.aviso/pretty "0.1.12"]
                 [org.apache.commons/commons-lang3 "3.1"]
                 [org.clojure/data.json "0.2.5"]]
  :profiles {:dev {:dependencies [[midje "1.5.1"]]}}
  :plugins [[lein-midje "3.1.0"]]

  :aot :all
  :main silk.cli.main)
