(ns salava.page.ui.routes 
  (:require [salava.core.ui.layout :as layout]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.page.ui.my :as my]
            [salava.page.ui.edit :as edit]
            [salava.page.ui.view :as view]))

(defn placeholder [content]
  (fn [site-navi params]
    #(layout/default site-navi content)))

(defn ^:export routes [context]
  {"/page" [[""                  my/handler]
            ["/mypages"          my/handler]
            [["/view/" :page-id] view/handler]
            [["/edit/" :page-id] edit/handler]
            [["/edit_skin/" :page-id] (placeholder [:p (t :page/Editpageskin)])]
            [["/settings/" :page-id] (placeholder [:p (t :page/Pagesettings)])]]})

(defn ^:export navi [context]
  {"/page"         {:weight 30 :title (t :page/Pages)    :breadcrumb (str (t :page/Pages) " / " (t :page/Mypages))}
   "/page/mypages" {:weight 31 :title (t :page/Mypages)  :breadcrumb (str (t :page/Pages) " / " (t :page/Mypages))}})

