(ns salava.core.ui.routes)

(def not-found
  (fn [] [:p "404 Not Found"]))


(defn ^:export routes [context]
  {"/" [["" (constantly [:p "Home page"])]
        [true not-found]]})


(defn ^:export navi [context] {})


(defn ^:export heading [context] {})
