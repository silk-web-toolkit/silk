(ns silk.core.main
  (:require [hickory.core :as h]
            [hickory.render :as hr]
            [silk.core.transform.data :as sd]))

(enable-console-print!)

(defn html [] (.-innerHTML (.-documentElement js/document)))
;
; (println "BEFORE")
; (println (html))
; (println "AFTER")
; (println (sd/process-data (h/as-hickory (h/parse (html)))))
; (println (hr/hickory-to-html (sd/process-data (h/as-hickory (h/parse (html))))))
; 
(set! (.-innerHTML (.-documentElement js/document))
      (hr/hickory-to-html (sd/process-data (h/as-hickory (h/parse (html))))))
