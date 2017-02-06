(ns silk.core.main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<!]]
            [cljs.reader :as r]
            [cljs-http.client :as http]
            [hickory.core :as h]
            [hickory.render :as hr]
            [hickory.select :as hs]
            [silk.core.browser :as sb]
            [silk.core.common.core :as cr]
            [silk.core.transform.data :as dt]))

(enable-console-print!)

(def some-data
  [
    {
      :title "Room - Hall"
      :description "The Hall of at home"
      :links [
        {
          :transition-type "Door"
          :direction "West"
          :title "Enter"
          :href "http://www.silkyweb.org"
        }
      ]
    }
  ]
)

(sb/spin-component-with-data "whatever" some-data)
;;(sb/spin-components)
