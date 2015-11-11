(ns salava.file.ui.routes
  (:require [salava.core.ui.layout :as layout]
            [salava.core.i18n :as i18n :refer [t]]))


(defn placeholder [content]
  (fn [site-navi params]
    #(layout/default site-navi content)))

(defn ^:export routes [context]
  {"/page" [["/files" (placeholder [:p "My files"])]]})

(defn ^:export navi [context]
  {"/page/files" {:weight 35 :title "Files" :breadcrumb (t :page/Pages " / "  :file/Files)}})

