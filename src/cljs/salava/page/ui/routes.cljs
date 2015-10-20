(ns salava.page.ui.routes 
  (:require [salava.core.ui.layout :as layout]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.page.ui.my :as my]))

(defn ^:export routes [context]
  {"/page" [[""         my/handler]
             ["/mypages" my/handler]]})

(defn ^:export navi [context]
  {"/page"         {:weight 30 :title (t :page/Pages)    :breadcrumb (str (t :page/Pages) " / " (t :page/Mypages))}
   "/page/mypages" {:weight 31 :title (t :page/Mypages)  :breadcrumb (str (t :page/Pages) " / " (t :page/Mypages))}})

