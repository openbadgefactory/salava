(ns salava.badge.ui.info
  (:require [reagent.core :refer [atom]]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]
            [salava.badge.ui.helper :as bh]))

(defn toggle-visibility [state]
  (let [id (:id @state)
        new-value (if (= (:visibility @state) "private")
                         "public"
                         "private")]
    (ajax/POST
      (str "/obpv1/badge/set_visibility/" id)
      {:params {:visibility new-value}
       :handler (fn []
                  (swap! state assoc :visibility new-value))})))

(defn toggle-recipient-name [state]
  (let [id (:id @state)
        new-value (not (:show_recipient_name @state))]
    (ajax/POST
      (str "/obpv1/badge/toggle_recipient_name/" id)
      {:params {:show_recipient_name new-value}
       :handler (fn []
                  (swap! state assoc :show_recipient_name new-value))})))


(defn content [state]
  [:div {:class "badge-info"}
   [:div.row
    [:div.col-sm-4
     [:input {:type "checkbox"
              :id "checkbox-share"
              :on-change #(toggle-visibility state)
              :checked (= "public" (:visibility @state))}]
     [:label {:for "checkbox-share"}
     (t :badge/Publishshare)]]
    [:div.col-sm-4
     [:input {:type "checkbox"
              :on-change #(toggle-recipient-name state)
              :id "checkbox-show-recipient-name"
              :checked (:show_recipient_name @state)}]
     [:label {:for "checkbox-show-recipient-name"}
      (t :badge/Showyourname)]]
    [:div.col-sm-4
     [:button {:class "btn btn-primary"
               :on-click #(.print js/window)}
      (t :badge/Print)]]]
   [:div.row
    [:div {:class "col-md-3 badge-image"}
     [:img {:src (str "/" (:image_file @state))}]]
    [:div {:class "col-md-9"}
     [:div.row
      [:div {:class "col-md-12 badge-info"}
       [:h1 (:name @state)]
       (bh/issuer-label-and-link (:issuer_name @state) (:issuer_url @state) (:issuer_contact @state))
       (if (:show_recipient_name @state)
         [:div
          (t :badge/Recipient ": " (:first_name @state) " " (:last_name @state))])
       [:div
        (:description @state)]
       [:h2.uppercase-header (t :badge/Criteria)]
       [:a {:href (:criteria_url @state)
            :target "_blank"}
        (t :badge/Opencriteriapage)]]]]]])

(defn init-data [state id]
  (ajax/GET
    (str "/obpv1/badge/info/" id)
    {:handler (fn [data]
                (reset! state data))}))


(defn handler [site-navi params]
  (let [id (:badge-id params)
        state (atom {})]
    (init-data state id)
    (fn []
      (layout/default site-navi (content state)))))

