(ns salava.location.ui.routes
  (:require [salava.core.ui.layout :as layout]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.core.ui.helper :refer [base-path path-for]]
            [salava.location.ui.explore :as explore]
            [salava.location.ui.block]
            [salava.location.ui.modal]
            [salava.location.ui.embed :as embed]))


(defn ^:export routes [context]
  {(str (base-path context) "/gallery") [["/map" explore/handler]
                                         ["/map/embed" embed/handler]
                                         ["/map/embed/generate-link" embed/link-handler]]})
(def about
  {:heading (t :location/Map)
   :content [:div
             [:p (t :location/Aboutlocation)]
             [:h5 [:b (t :location/Notonmap)]]
             [:p (t :Toappearonmap)]]})

(defn ^:export navi [context]
  {(str (base-path context) "/gallery/map") {:weight 50 :title (t :location/Map) :site-navi true :breadcrumb (t :gallery/Gallery " / " :location/Map) :about about}})

(defn ^:export quicklinks []
  [{:title [:p (t :location/Iwanttosetmylocation)]
    :url (str (path-for "/user/edit"))
    :weight 1}])
