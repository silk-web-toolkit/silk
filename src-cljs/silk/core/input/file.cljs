(ns silk.core.input.file
  "File input functions including template and component."
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [silk.macro.core :refer [slurp]])
  (:require [cljs.reader :as r]
            [cljs-http.client :as http]))
            ; [silk.macro.core :include-macros true :refer [slurp]]))
            ; [slurp.core :include-macros true :refer [slurp]]))
            ; [cljs.core.async :refer [<!]]))

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

; (defn slurp-data
;   [path sort direc limit]
;   (go
;     (let [rsp (<! (http/get path))
;           a   (r/read-string (:body rsp))
;           b   (sort-it a sort direc)]
;       (println b)
;       (if limit
;         (vec (take (Integer. (re-find  #"\d+" limit)) b))
;         (vec b)))))

(def lol
  [
    {
      :title "item 1"
      :description "Item 1 should have 1 link"
      :links [
        {
          :title "Silk Home Page"
          :href "http://www.silkyweb.org"
          :contents [
            {:description "content 1"}
          ]
        }
      ]
    } {
      :title "item 2"
      :description "Item 2 should have 4 links"
      :links [
        {
          :title "Blank Template Example"
          :href "http://silk-web-toolkit.github.io/blank-template/"
          :contents [
            {:description "content 1"}
            {:description "content 2"}
          ]
        }
        {
          :title "Sourced Component Example"
          :href "http://silk-web-toolkit.github.io/sourced-component/"
          :contents [
            {:description "content 1"}
            {:description "content 2"}
            {:description "content 3"}
          ]
          :same-levels [
            {:description "Hey look at me I'm on the same level"}
          ]
        }
        {
          :title "Tree Component Example"
          :href "http://silk-web-toolkit.github.io/tree-component/"
          :contents [
            {:description "content 1"}
            {:description "content 2" :subs [{:node "Im a sub node"}]}
            {:description "content 3"}
            {:description "content 4"}
          ]
        }
        {
          :title "Blog Example"
          :href "http://silk-web-toolkit.github.io/blog/"
          :contents [
            {:description "content 1"}
            {:description "content 2"}
            {:description "content 3"}
            {:description "content 4"}
            {:description "content 5"}
          ]
        }
      ]
    }
  ])

(defn slurp-data
  [path sort direc limit]
  (let [a (r/read-string (str lol))
        b (sort-it a sort direc)]
    (if limit
      (vec (take (Integer. (re-find  #"\d+" limit)) b))
      (vec b))))
