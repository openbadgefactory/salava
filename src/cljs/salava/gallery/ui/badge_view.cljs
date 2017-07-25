#_(ns salava.gallery.ui.badge-view
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.helper :refer [path-for]]
            ;[salava.gallery.ui.badge-content :refer [badge-content]]
            [salava.core.ui.share :refer [share-buttons]]
            [salava.admin.ui.admintool :refer [admintool]]
            [reagent-modals.modals :as m]
            [salava.core.helper :refer [dump]]
            ;[salava.admin.ui.reporttool :refer [reporttool]]
            [salava.core.i18n :refer [t]]))

#_(defn content [state]
  (let [{content :content badge-id :badge-id} @state
        {{name :name} :badge} content]
    [:div {:id "badge-gallery-view"}
     [m/modal-window]
     [:div.panel
      [:div.panel-body
       (admintool badge-id "badges")
       [share-buttons (str (session/get :site-url) (path-for "/gallery/badgeview/") badge-id) name true true (cursor state [:show-link-or-embed])]

       #_(if (:content @state)
         [badge-content (:content @state) false])]
      ;(reporttool badge-id name "badges")
      ]]))

#_(defn init-data [state badge-id]
  (ajax/GET
    (path-for (str "/obpv1/gallery/public_badge_content/" badge-id))
    {:handler (fn [data]
                (swap! state assoc :content data))}))

#_(defn handler [site-navi params]
  (let [badge-id (:badge-id params)
        state (atom {:badge-id badge-id
                     :content nil
                     :show-link-or-embed nil})]
    (init-data state badge-id)
    (fn []
      (if (session/get :user)
        (layout/default site-navi (content state))
        (layout/landing-page site-navi (content state))))))
