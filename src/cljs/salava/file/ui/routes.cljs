(ns salava.file.ui.routes
  (:require [salava.core.ui.layout :as layout]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.file.ui.my :as my]
            [salava.file.ui.browser :as browser]))

(defn ^:export routes [context]
  {"/page" [["/files" my/handler]]
   "/file" [[["/browser/" :editor "/" :callback "/" :lang] browser/handler]]})

(defn ^:export navi [context]
  {"/page/files" {:weight 35 :title (t :page/Files) :site-navi true :breadcrumb (t :page/Pages " / " :page/Files)}})

