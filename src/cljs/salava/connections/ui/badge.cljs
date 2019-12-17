(ns salava.connections.ui.badge
  (:require [salava.core.ui.layout :as layout]
            [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.helper :refer [dump]]
            [reagent-modals.modals :as m]
            [salava.core.ui.modal :as mo]
            [salava.core.ui.helper :as h :refer [unique-values navigate-to path-for plugin-fun]]
            [salava.badge.ui.issuer :as s]))

(defn init-issuer-connections [state]
  (ajax/GET
    (path-for "/obpv1/connections/connections_issuer")
    {:handler (fn [data]
                (swap! state assoc :issuers data))}))

(defn init-data [state]
  (ajax/GET
    (path-for "/obpv1/connections/connections_badge" true)
    {:handler (fn [data]
                ; (prn data)
                (swap! state assoc :badges data
                       :visible-area (session/get! :visible-area nil))
                (init-issuer-connections state))}))


(defn unfollow [badge-id state]
  [:a {:href "#" :on-click #(ajax/POST
                              (path-for (str "/obpv1/social/delete_connection_badge/" badge-id))
                              {:response-format :json
                               :keywords?       true
                               :handler         (fn [data]
                                                  (do
                                                    (init-data state)))
                               :error-handler   (fn [{:keys [status status-text]}]
                                                  (.log js/console (str status " " status-text)))})}
      (t :social/Unfollow)])

(defn remove-issuer-from-favourites [issuer-id state]
  [:a {:href "#"
       :on-click #(s/remove-issuer-from-favourites issuer-id state (init-data state))}
   [:i {:class "fa fa-bookmark"}] (str " " (t :badge/Removefromfavourites))])

(defn toggle-panel [key atom]
  (if (= key @atom)
    (reset! atom nil)
    (reset! atom key)))

(defn issuer-connections [issuers state]
  (let [panel-identity :issuers
        visible-area-atom (cursor state [:visible-area])
        icon-class (if (= @visible-area-atom :issuers) "fa-chevron-circle-down" "fa-chevron-circle-right")]
    [:div.expandable-block {:class "panel issuer-panel"}
     [:div.panel-heading
      [:a {:href "#" :on-click #(do (.preventDefault %) (toggle-panel :issuers visible-area-atom) #_(reset! (cursor state [:visible-area]) :issuers))}
       [:h2 (str (t :badge/Issuers) " (" (count issuers) ")")]
       #_[:div {:style {:margin-top "5px"}} (t :connections/Issuerblockinfo)]
       [:i.fa.fa-lg.panel-status-icon {:class icon-class}]]]
     (when (= @(cursor state [:visible-area]) panel-identity)
       [:div.panel-body
        [:table {:class "table" :summary (t :badge/Issuers)}
         (into [:tbody]
               (for [issuer issuers
                     :let [{:keys [id name image_file]} issuer]]
                 [:tr
                  [:td.name [:a {:href "#"
                                 :on-click #(do
                                              (mo/open-modal [:badge :issuer] id {:hide (fn [] (init-data state))})
                                              (.preventDefault %)) }(if image_file [:img.badge-icon {:src (str "/" image_file) :alt (str name " icon")}]
                                                                      [:span {:style {:width "30px" :display "inline-block" :margin-right "10px"} :dangerouslySetInnerHTML {:__html "&nbsp;"}}]) name]]
                  [:td.action (remove-issuer-from-favourites id state)]]))]])]))


(defn badge-connections [badges state]
  (let [panel-identity :badges
        visible-area-atom (cursor state [:visible-area])
        icon-class (if (= @visible-area-atom :badges) "fa-chevron-circle-down" "fa-chevron-circle-right")]
    [:div.panel.expandable-block
     [:div.panel-heading
      [:a {:href "#" :on-click #(do (.preventDefault %) (toggle-panel :badges visible-area-atom) #_(reset! (cursor state [:visible-area]) :badges))}
       [:h2 (str (t :badge/Badges) " (" (count badges) ")")]
       #_[:p {:style {:margin-top "5px"}} (t :connections/Badgeblockinfo)]
         [:i.fa.fa-lg.panel-status-icon {:class icon-class}]]]
     (when (= @(cursor state [:visible-area]) panel-identity)
       [:div.panel-body
        [:table {:class "table" :summary (t :badge/Badgeviews)}
         [:thead
          [:tr
           [:th (t :badge/Badge)]
           [:th (t :badge/Name)]
           #_[:th ""]]]

         (into [:tbody]
               (for [badge-views badges
                     :let [{:keys [id name image_file reg_count anon_count latest_view]} badge-views]]
                 [:tr
                  [:td.icon [:img.badge-icon {:src (str "/" image_file)
                                              :alt ""}]]
                  [:td.name [:a {:href "#"
                                 :on-click #(do
                                              (mo/open-modal [:gallery :badges] {:badge-id id} {:hide (fn [] (init-data state))})
                                              ;(b/open-modal id false init-data state)
                                              (.preventDefault %)) } name]]
                  [:td.action (unfollow id state)]]))]])]))



(defn content [state]
  (let [badges (:badges @state)
        issuers (:issuers @state)]
    [:div
     [m/modal-window]
     [:div {:id "badge-stats"}
      [:h1.uppercase-header
       (t :connections/BadgeConnections)]

      [:div {:style {:margin-bottom "10px"}} (t :connections/Badgeconnectionsinfo)]
      (badge-connections badges state)
      (issuer-connections issuers state)]]))
     ;(user-connections)



(defn handler [site-navi]
  (let [state (atom {:badges []
                     :issuers []})]
    (init-data state)

    (fn []
      (layout/default site-navi (content state)))))
