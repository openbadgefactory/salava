(ns salava.location.ui.routes
  (:require [salava.core.ui.layout :as layout]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.core.ui.helper :refer [base-path path-for]]
            [salava.location.ui.explore :as explore]
            [salava.location.ui.block]
            [salava.location.ui.modal]
            ))

(defn ^:export routes [context]
  {(str (base-path context) "/gallery") [["/map" explore/handler]]})

(defn ^:export navi [context]
  {(str (base-path context) "/gallery/map") {:weight 50 :title (t :location/Map) :site-navi true :breadcrumb (t :gallery/Gallery " / " :location/Map)}})

(defn ^:export quicklinks []
  [{:title [:p (t :location/Iwanttosetmylocation)]
    :url (str (path-for "/user/edit"))
    :weight 1}])
