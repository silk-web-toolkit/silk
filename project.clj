(defproject org.silkyweb/silk "0.8.1"
  :description "Silk static and dynamic publishing toolkit."
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.taoensso/timbre "4.7.4"]
                 [pathetic "0.5.1"]
                 [hickory "0.7.0"]
                 [com.rpl/specter "0.13.1"]
                 [me.rossputin/diskops "0.4.1"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojars.zcaudate/watchtower "0.1.2"]
                 [io.aviso/pretty "0.1.30"]
                 [org.apache.commons/commons-lang3 "3.5"]
                 [org.clojure/data.json "0.2.6"]]
  :profiles {:dev {:dependencies [[midje "1.8.3"]]}}
  :plugins [[lein-midje "3.2.1"]]
  :aot :all
  :main silk.cli.main)
