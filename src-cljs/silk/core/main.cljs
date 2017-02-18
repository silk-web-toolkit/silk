(ns silk.core.main
  (:require [silk.core.browser :as sb]))

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

; (sb/spin-component-with-data js/document "whatever" some-data)
(sb/spin-components js/document)
