(ns silk.core.server
  (:require [com.rpl.specter :as spec]
            [hickory.core :refer :all]
            [hickory.render :as hr]
            [hickory.select :as s]
            [silk.core.common.core :as cr]
            [silk.core.input.file :as sf]))

(defn spin-component-with-data
  "Splices data with component with given id"
  [tpl id data]
  (let [hick (sf/hick-file tpl)]
    (hr/hickory-to-html (spec/transform (spec/walker #(= (get-in % [:attrs :id]) id)) #(cr/process-component-with-data % data) hick))))
