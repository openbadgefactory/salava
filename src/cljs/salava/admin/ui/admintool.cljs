(ns salava.admin.ui.admintool
  (:require [reagent.core :refer [atom cursor]]
            [reagent-modals.modals :as m]
            [clojure.string :refer [trim]]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.grid :as g]
            [salava.core.ui.helper :refer [path-for current-path navigate-to input-valid?]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.admin.ui.helper :refer [valid-item-type? valid-item-id? checker admin?]]))

(defn set-private [item-type item-id state init-data]
  (ajax/POST
   (path-for (str "/obpv1/admin/private_"item-type"/" item-id))
   {:response-format :json
    :keywords? true
    :params {:item-type item-type :item-id item-id}
    :handler (fn [data]
               (if (not-empty (str init-data))
                 (init-data state nil)
                 (navigate-to "/admin")))
    :error-handler (fn [{:keys [status status-text]}]
                     (.log js/console (str status " " status-text)))}))

(defn admin-modal [item-type item-id state init-data]
  [:div
   [:div.modal-header
    [:button {:type "button"
              :class "close"
              :data-dismiss "modal"
              :aria-label "OK"}
     [:span {:aria-hidden "true"
             :dangerouslySetInnerHTML {:__html "&times;"}}]]]
   [:div.modal-body
    (str  (t :admin/Privatethis) "?" (:user-id state))]
   [:div.modal-footer
    [:button {:type "button"
              :class "btn btn-primary"
              :data-dismiss "modal"
              :on-click #(set-private item-type item-id state init-data)}
     (t :admin/Yes)]
    [:button {:type "button"
              :class "btn btn-primary"
              :data-dismiss "modal"}
     (t :admin/No)]]])

(defn admin-gallery-modal [item-type item-id state init-data]
  [:div
   [:div.modal-header
    [:button {:type "button"
              :class "close"
              :data-dismiss "modal"
              :aria-label "OK"}
     [:span {:aria-hidden "true"
             :dangerouslySetInnerHTML {:__html "&times;"}}]]]
   [:div.modal-body
    (str  (t :admin/Privatethis) "?" )]
   [:div.modal-footer
    [:button {:type "button"
              :class "btn btn-primary"
              :data-dismiss "modal"
              :on-click #(set-private item-type item-id state init-data)}
     (t :admin/Yes)]
    [:button {:type "button"
              :class "btn btn-primary"
              :data-dismiss "modal"}
     (t :admin/No)]]])




(defn private-this-page[]
   (if (admin?)  
     (let [{:keys [item-type item-id]} (checker (current-path))]
       [:div.row
        [:div {:class "pull-right"}
         [m/modal-window]
         [:button {:type "button"
                   :class "btn btn-danger"
                   :on-click #(do (.preventDefault %)
                                  (m/modal! (admin-modal item-type item-id "" "") ))} (t :admin/Private)]]])))

(defn private-gallery-badge [item-id item-type state init-data]
  (if (admin?)
    [:a {:class "bottom-link pull-right"
         :on-click #(do (.preventDefault %)
                        (m/modal! (admin-gallery-modal item-type item-id state init-data) ))}
     [:i {:class "fa fa-lock"}] (t :admin/Private)]))

(defn private-gallery-page [item-id item-type state init-data]
  (if (admin?)
    [:div.media-bottom-admin
     [:a {:class "bottom-link pull-right"
          :on-click #(do (.preventDefault %)
                         (m/modal! (admin-gallery-modal item-type item-id state init-data) ))}
      [:i {:class "fa fa-lock"}] (t :admin/Private)]]))
