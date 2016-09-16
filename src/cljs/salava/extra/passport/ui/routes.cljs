(ns salava.extra.passport.ui.routes
  (:require [salava.extra.passport.ui.footer :refer [footer]]
            [salava.core.ui.helper :refer [base-path]]
            [salava.extra.passport.ui.badge-application :as b]
            [salava.core.i18n :as i18n :refer [t]]))

(defn ^:export routes [context]
  {(str (base-path context) "/gallery") [["/application" b/handler]]})

(defn ^:export navi [context]
  {(str (base-path context) "/gallery/application")       {:weight 44 :title (t :extra-factory/Application) :site-navi true :breadcrumb (t :gallery/Gallery " / " :extra-factory/Application)}
   "passport/footer" {:footer footer}
   "https://openbadgepassport.com/" {:weight 1 :title (t :core/Home) :top-navi-landing true}
   "https://openbadgepassport.com/about/" {:weight 2 :title (t :core/About) :top-navi-landing true}
   "https://openbadgepassport.com/news/" {:weight 3 :title (t :core/News) :top-navi-landing true}
   "https://openbadgepassport.com/faq/" {:weight 4 :title (t :core/Faq) :top-navi-landing true}})
