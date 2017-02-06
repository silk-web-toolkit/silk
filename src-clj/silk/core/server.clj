(ns silk.core.server
  (:require [hickory.core :refer :all]
            [hickory.render :as hr]
            [hickory.select :as s]
            [silk.core.common.core :as cr]))

(defn- hickorify [tpl] (-> tpl parse as-hickory))

(defn- elementById [id tpl] (-> (s/select (s/child (s/id id)) tpl) first))

(defn spin-component-with-data
  "Splices data with component with given id"
  [tpl id data]
  (let [slurped (slurp tpl)
        hick (hickorify slurped)
        component (elementById id hick)
        res (cr/process-component-with-data component data)]
    (hr/hickory-to-html res)))
