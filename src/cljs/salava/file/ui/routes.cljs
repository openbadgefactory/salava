(ns salava.file.ui.routes
  (:require [salava.core.ui.layout :as layout]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.file.ui.my :as my]
            [salava.file.ui.upload :as upload]))

(defn ^:export routes [context]
  {"/page" [["/files" my/handler]]
   "/file" [["/upload" upload/handler]]})

(defn ^:export navi [context]
  {"/page/files" {:weight 35 :title (t :page/Files) :breadcrumb (str (t :page/Pages) " / " (t :page/Files))}})

