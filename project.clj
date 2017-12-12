(defproject org.silkyweb/silk "0.12.0"
  :description "Silk static and dynamic publishing toolkit."
  :source-paths ["src-clj" "src-cljc"]
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [cljs-http "0.1.43"]
                 [pathetic "0.5.1"]
                 [hickory "0.7.1"]
                 [expectations "2.1.1"]
                 [com.rpl/specter "0.13.1"]
                 [me.rossputin/diskops "0.6.0"]
                 [org.clojure/tools.cli "0.3.5"]
                 [hawk "0.2.11"]
                 [io.aviso/pretty "0.1.34"]
                 [org.apache.commons/commons-lang3 "3.5"]
                 [org.clojure/data.json "0.2.6"]
                 [markdown-clj "1.0.1"]]
  :plugins [[lein-cljsbuild "1.1.6"]
            [lein-expectations "0.0.8"]]
  :cljsbuild {
    :builds [{:source-paths ["src-cljs"]
              :compiler {:output-to "resources/public/js/browser.js"
                         :output-dir "resources/public/js"
                         :optimizations :simple
                         :source-map "resources/public/js/browser.js.map"}}]}
  :aot :all
  :main silk.cli.main)
