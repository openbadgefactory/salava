(ns salava.extra.spaces.ui.my
  (:require
   [reagent.core :refer [atom cursor]]
   [reagent-modals.modals :as m]
   [salava.core.i18n :refer [t]]
   [salava.core.ui.ajax-utils :as ajax]
   [salava.core.ui.helper :refer [path-for]]
   [salava.core.ui.layout :as layout]
   [salava.core.ui.modal :as mo]
   [salava.extra.spaces.ui.helper :refer [space-card]]))

(defn init-data [state]
  (ajax/GET
   (path-for "/obpv1/spaces/")
   {:handler (fn [data]
               (swap! state assoc :spaces data))}))

(defn create! [state]
  (ajax/POST
   (path-for "/obpv1/spaces/create")
   {:params @(cursor state [:new])}
   {:handler (fn [data]
               (if (= "success" (:status data))
                 "" ""))}))

(defn space-gallery [state]
  (into [:div#grid.row.wrap-grid
         [:div.col-xs-12.col-sm-6.col-md-4
          [:div {:class "media grid-container"}
           [:a.add-element-link {:href  (path-for "/admin/spaces/creator")}
             [:div.media-content
              [:div.media-body
               [:div.text-center {:id "add-element-icon"}
                [:i.fa.fa-plus]]
               #_[:div {:id "add-element-link"}
                  (t :badge/New)]]]]]]]
    (for [space @(cursor state [:spaces])]
     (space-card state))))


(defn content [state]
  [:div#space-gallery
   [m/modal-window]
   [space-gallery state]])


(defn handler [site-navi]
  (let [state (atom {:spaces []
                     :new nil})]
   (init-data state)
   (fn []
     (layout/default site-navi [content state]))))
