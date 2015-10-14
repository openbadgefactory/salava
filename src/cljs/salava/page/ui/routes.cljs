(ns salava.page.ui.routes 
  (:require [salava.core.ui.layout :as layout]))

(defn placeholder [content]
  (fn [site-navi params]
    #(layout/default site-navi content)))

(defn ^:export routes [context]
  {"/pages" [[""         (placeholder [:p "My pages"])]
             ["/mypages" (placeholder [:p "My pages"])]]})


(defn ^:export navi [context]
  {"/pages/"         {:weight 30 :title "Pages"}
   "/pages/mypages/" {:weight 31 :title "My pages"}})

(defn ^:export heading [context]
  {"/pages/" "Pages / My pages"
   "/pages/mypages/" "Pages / My pages"
   "/pages/files/" "Pages / My files"})
