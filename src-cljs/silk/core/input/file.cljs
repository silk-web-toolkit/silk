(ns silk.core.input.file
  "File input functions including template and component."
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.reader :as r]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- sort-it
  "Default to descending sort"
  [data sort direc]
  (cond
    (nil? sort)           (sort-by :sw/path data)
    (nil? direc)          (sort-by (keyword sort) data)
    (= direc "ascending") (sort-by (keyword sort) data)
    :else                 (reverse (sort-by (keyword sort) data))))

;; =============================================================================
;; File based input, see namespace comment
;; =============================================================================

(defn slurp-data
  [path sort direc limit]
  (go
    (let [rsp (<! (http/get path))
          a   (r/read-string (:body rsp))
          b   (sort-it a sort direc)]
      (println b)
      (if limit
        (vec (take (Integer. (re-find  #"\d+" limit)) b))
        (vec b)))))
