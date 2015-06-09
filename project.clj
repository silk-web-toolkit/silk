(defproject org.silkyweb/silk "0.7.0"
  :description "Silk static and dynamic publishing toolkit."
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.taoensso/timbre "3.3.1"]
                 [pathetic "0.5.1"]
                 [me.raynes/laser "1.1.1"]
                 [me.rossputin/diskops "0.4.1"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.clojars.zcaudate/watchtower "0.1.2"]
                 [io.aviso/pretty "0.1.12"]
                 [org.apache.commons/commons-lang3 "3.3.2"]
                 [org.clojure/data.json "0.2.5"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]]}}
  :plugins [[lein-midje "3.1.0"]]
  :aot :all
  :main silk.cli.main)
