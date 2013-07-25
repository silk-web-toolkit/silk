(ns silk.ast.describe
  "Describe the shape of key elements of the AST.")

(defn get-component-attribs
  []
  [:data-sw-component :data-sw-source :data-sw-sort])

;; TODO: very proto code (POC)
(defn get-dynamic-attribs
  []
  [:data-sw-text :data-sw-href :data-sw-class :data-sw-src :data-sw-title])
