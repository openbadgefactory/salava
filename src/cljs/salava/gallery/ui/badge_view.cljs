(ns salava.gallery.ui.badge-view
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.helper :refer [path-for]]
            [salava.gallery.ui.badge-content :refer [badge-content]]
            [salava.core.ui.share :refer [share-buttons]]
            [salava.admin.ui.admintool :refer [admintool]]
            [salava.admin.ui.reporttool :refer [reporttool]]
            [salava.core.i18n :refer [t]]))

(defn content [state]
  (let [{content :content badge-content-id :badge-content-id} @state
        {{name :name} :badge} content]
    [:div {:id "badge-gallery-view"}
     [:div.panel
      [:div.panel-body
       [share-buttons (str (session/get :site-url) (path-for "/gallery/badgeview/") badge-content-id) name true true (cursor state [:show-link-or-embed])]
       [badge-content content]]
      (reporttool badge-content-id name "badges")
      (admintool)
      ]]))

(defn init-data [state badge-content-id]
  (ajax/GET
    (path-for (str "/obpv1/gallery/public_badge_content/" badge-content-id))
    {:handler (fn [data]
                (swap! state assoc :content data))}))

(defn handler [site-navi params]
  (let [badge-content-id (:badge-content-id params)
        state (atom {:badge-content-id badge-content-id
                     :content nil
                     :show-link-or-embed nil})]
    (init-data state badge-content-id)
    (fn []
      (if (session/get :user)
        (layout/default site-navi (content state))
        (layout/landing-page site-navi (content state))))))
