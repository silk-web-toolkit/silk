(defproject org.silkyweb/silk "0.9.0"
  :description "Silk static and dynamic publishing toolkit."
  :source-paths ["src-clj" "src-cljc"]
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.293"]
                 [cljs-http "0.1.42"]
                 [com.taoensso/timbre "4.7.4"]
                 [pathetic "0.5.1"]
                 [hickory "0.7.0"]
                 [expectations "2.0.13"]
                 [com.rpl/specter "0.13.1"]
                 [me.rossputin/diskops "0.6.0"]
                 [org.clojure/tools.cli "0.3.5"]
                 [hawk "0.2.11"]
                 [io.aviso/pretty "0.1.33"]
                 [org.apache.commons/commons-lang3 "3.5"]
                 [org.clojure/data.json "0.2.6"]]
  :plugins [[lein-cljsbuild "1.1.5"]
            [lein-expectations "0.0.8"]]
  :cljsbuild {
    :builds [{:source-paths ["src-cljs"]
              :compiler {:output-to "resources/public/js/main.js"
                         :output-dir "resources/public/js"
                         :optimizations :simple
                         :source-map "resources/public/js/main.js.map"}}]}
  :aot :all
  :main silk.cli.main)
