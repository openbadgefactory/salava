(ns salava.page.ui.routes
  (:require [salava.core.ui.layout :as layout]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.core.ui.helper :refer [base-path]]
            [salava.page.ui.my :as my]
            [salava.page.ui.edit :as edit]
            [salava.page.ui.view :as view]
            [salava.page.ui.embed :as embed]
            [salava.page.ui.theme :as theme]
            [salava.page.ui.settings :as settings]
            [salava.page.ui.preview :as preview]
            [salava.page.ui.modal :as pagemodel]))

(defn ^:export routes [context]
  {(str (base-path context) "/profile/page") [["" my/handler]
                                              ["/mypages" my/handler]
                                              [["/view/" :page-id] view/handler]
                                              [["/view/" :page-id "/embed"] embed/handler]
                                              [["/edit/" :page-id] edit/handler]
                                              [["/edit_theme/" :page-id] theme/handler]
                                              [["/settings/" :page-id] settings/handler]
                                              [["/preview/" :page-id] preview/handler]]})

(defn ^:export navi [context]
  {;(str (base-path context) "/profile/page")                 {:breadcrumb (t :page/Pages " / " :page/Mypages)}
   (str (base-path context) "/profile/page")         {:weight 32 :title (t :page/Pages) :site-navi true :breadcrumb (t :user/Profile " / " :page/Pages)}
   (str (base-path context) "/profile/page/view/\\d+")       {:breadcrumb (t :page/Pages " / " :page/Viewpage)}
   (str (base-path context) "/profile/page/edit/\\d+")       {:breadcrumb (t :page/Pages " / " :page/Editpage)}
   (str (base-path context) "/profile/page/edit_theme/\\d+") {:breadcrumb (t :page/Pages " / " :page/Choosetheme)}
   (str (base-path context) "/profile/page/settings/\\d+")   {:breadcrumb (t :page/Pages " / " :page/Settings)}
   (str (base-path context) "/profile/page/preview/\\d+")    {:breadcrumb (t :page/Pages " / " :page/Viewpage)}})

