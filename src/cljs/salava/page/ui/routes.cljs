(ns salava.page.ui.routes 
  (:require [salava.core.ui.layout :as layout]
            [salava.core.i18n :as i18n :refer [t]]))

(defn placeholder [content]
  (fn [site-navi params]
    #(layout/default site-navi content)))

(defn ^:export routes [context]
  {"/pages" [[""         (placeholder [:p "My pages"])]
             ["/mypages" (placeholder [:p "My pages"])]]})


(defn ^:export navi [context]
  {"/pages"         {:weight 30 :title "Pages"    :breadcrumb (str (t :page/pages) " / ")}
   "/pages/mypages" {:weight 31 :title "My pages" :breadcrumb (str (t :page/pages) " / " (t :page/mypages))}})

