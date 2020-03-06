(ns salava.file.ui.routes
  (:require [salava.core.ui.layout :as layout]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.core.ui.helper :refer [base-path]]
            [salava.file.ui.my :as my]
            [salava.file.ui.browser :as browser]))

(defn ^:export routes [context]
  {(str (base-path context) "/profile") [["/files" my/handler]]
   (str (base-path context) "/file") [[["/browser/" :editor "/" :callback "/" :lang] browser/handler]]})

(def about
  {:heading (t :user/Profile " / " :page/Files)
   :content [:div
             [:p.page-tip (t :file/Aboutfiles)]
             [:p (t :file/Aboutfiles2)]]})


(defn ^:export navi [context]
  {(str (base-path context) "/profile/files") {:weight 35 :title (t :page/Files) :site-navi true :breadcrumb (t :user/Profile " / " :page/Files) :about about}})
