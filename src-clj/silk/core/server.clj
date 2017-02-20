(ns silk.core.server
  (:require [clojure.edn :as edn]
            [com.rpl.specter :as spec]
            [hickory.render :as hr]
            [silk.core.common.core :as cr]
            [silk.core.input.file :as sf]))

(defn spin-by-id
  "Splices markup restricted by given id and data"
  [markup id data]
  (hr/hickory-to-html
    (spec/transform (spec/walker #(= (get-in % [:attrs :id]) id))
                    #(cr/splice-hick-with-data % data)
                    (sf/hick-file markup))))

(defn spin-by-data-sw-source
  "Splices markup restricted by data-sw-source, data is provided by data-sw-source attribute value"
  [markup]
  (hr/hickory-to-html
    (spec/transform (spec/walker #(get-in % [:attrs :data-sw-source]))
                    #(let [path (get-in % [:attrs :data-sw-source])
                           data (edn/read-string (slurp path))]
                      (cr/splice-hick-with-data % data))
                    (sf/hick-file markup))))
