(ns silk.core.main
  (:require [silk.core.transform.data :as dt]))

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
(dt/process-component some-data)
