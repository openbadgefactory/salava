(ns salava.extra.cancred.ui.routes
  (:require [salava.extra.cancred.ui.footer :refer [footer]]))

(defn ^:export routes [context] {})


(defn ^:export navi [context]
  {"cancred/footer" {:footer footer}
   "https://passport.cancred.ca/about/" {:weight 1 :title "About" :top-navi-landing true}
   "https://passport.cancred.ca/news/" {:weight 2 :title "News" :top-navi-landing true}
   "https://passport.cancred.ca/faq/" {:weight 3 :title "FAQ" :top-navi-landing true}})
