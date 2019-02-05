(defproject org.silkyweb/silk "0.14.0"
  :description "Silk static and dynamic publishing toolkit."
  :source-paths ["src-clj" "src-cljc"]
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.516"]
                 [cljs-http "0.1.46"]
                 [pathetic "0.5.1"]
                 [hickory "0.7.1"]
                 [expectations "2.1.10"]
                 [com.rpl/specter "1.1.2"]
                 [me.rossputin/diskops "0.6.0"]
                 [org.clojure/tools.cli "0.4.1"]
                 [hawk "0.2.11"]
                 [io.aviso/pretty "0.1.36"]
                 [org.apache.commons/commons-lang3 "3.8.1"]
                 [org.clojure/data.json "0.2.6"]
                 [markdown-clj "1.0.7"]]
  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-expectations "0.0.8"]]
  :cljsbuild {
    :builds [{:source-paths ["src-cljs"]
              :compiler {:output-to "resources/public/js/browser.js"
                         :output-dir "resources/public/js"
                         :optimizations :simple
                         :source-map "resources/public/js/browser.js.map"}}]}
  :aot :all
  :main silk.cli.main)
