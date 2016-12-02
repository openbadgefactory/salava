(ns salava.extra.passport.ui.routes
  (:require [salava.extra.passport.ui.block]
            [salava.core.ui.helper :refer [base-path]]
            [salava.core.i18n :as i18n :refer [t]]))

(defn ^:export routes [context]
  {})

(defn ^:export navi [context]
  {"https://openbadgepassport.com/about/" {:weight 2 :title (t :core/About) :top-navi-landing true}
   "https://openbadgepassport.com/news/" {:weight 3 :title (t :core/News) :top-navi-landing true}
   "https://openbadgepassport.com/faq/" {:weight 4 :title (t :core/Faq) :top-navi-landing true}})
