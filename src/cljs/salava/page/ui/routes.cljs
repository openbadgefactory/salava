(ns salava.page.ui.routes 
  (:require [salava.core.ui.layout :as layout]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.page.ui.my :as my]
            [salava.page.ui.edit :as edit]
            [salava.page.ui.view :as view]
            [salava.page.ui.theme :as theme]
            [salava.page.ui.settings :as settings]
            [salava.page.ui.preview :as preview]))

(defn ^:export routes [context]
  {"/page" [[""                  my/handler]
            ["/mypages"          my/handler]
            [["/view/" :page-id] view/handler]
            [["/edit/" :page-id] edit/handler]
            [["/edit_theme/" :page-id] theme/handler]
            [["/settings/" :page-id] settings/handler]
            [["/preview/" :page-id] preview/handler]]})

(defn ^:export navi [context]
  {"/page"         {:weight 30 :title (t :page/Pages)   :top-navi true  :breadcrumb (t :page/Pages " / " :page/Mypages)}
   "/page/mypages" {:weight 31 :title (t :page/Mypages) :site-navi true :breadcrumb (t :page/Pages " / " :page/Mypages)}
   "/page/view/\\d+" {:breadcrumb (t :page/Pages " / " :page/Viewpage)}
   "/page/edit/\\d+" {:breadcrumb (t :page/Pages " / " :page/Editpage)}
   "/page/edit_theme/\\d+" {:breadcrumb (t :page/Pages " / " :page/Choosetheme)}
   "/page/settings/\\d+" {:breadcrumb (t :page/Pages " / " :page/Settings)}
   "/page/preview/\\d+" {:breadcrumb (t :page/Pages " / " :page/Viewpage)}})

