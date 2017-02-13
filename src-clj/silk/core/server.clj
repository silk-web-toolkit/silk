(ns silk.core.server
  (:require [clojure.edn :as edn]
            [com.rpl.specter :as spec]
            [hickory.core :refer :all]
            [hickory.render :as hr]
            [hickory.select :as s]
            [silk.core.common.core :as cr]
            [silk.core.input.file :as sf]))

(defn spin-component-with-data
  "Splices data with component with given id"
  [tpl id data]
  (hr/hickory-to-html
    (spec/transform (spec/walker #(= (get-in % [:attrs :id]) id))
                    #(cr/process-component-with-data % data)
                    (sf/hick-file tpl))))

(defn spin-components
  "Looks for data-sw-source and injects into it"
  [tpl]
  (hr/hickory-to-html
    (spec/transform (spec/walker #(get-in % [:attrs :data-sw-source]))
                    #(let [path (get-in % [:attrs :data-sw-source])
                           data (edn/read-string (slurp path))]
                      (cr/process-component-with-data % data))
                    (sf/hick-file tpl))))
