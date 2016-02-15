(ns salava.badge.ui.info
  (:require [reagent.core :refer [atom]]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]
            [salava.badge.ui.helper :as bh]
            [salava.core.ui.rate-it :as r]))

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

(defn congratulate [state]
  (ajax/POST
    (str "/obpv1/badge/congratulate/" (:id @state))
    {:handler (fn []
                (swap! state assoc :congratulated? true))}))


(defn content [state]
  (let [{:keys [owner? visibility show_recipient_name image_file rating issuer_content_name issuer_content_url issuer_contact first_name last_name description criteria_url html_content user-logged-in? congratulated? congratulations view_count issued_by_obf verified_by_obf obf_url]} @state]
    [:div {:id "badge-info"}
     (if owner?
       [:div.row
        [:div.col-sm-4
         [:input {:type "checkbox"
                  :id "checkbox-share"
                  :on-change #(toggle-visibility state)
                  :checked (= "public" visibility)}]
         [:label {:for "checkbox-share"}
          (t :badge/Publishshare)]]
        [:div.col-sm-4
         [:input {:type "checkbox"
                  :on-change #(toggle-recipient-name state)
                  :id "checkbox-show-recipient-name"
                  :checked show_recipient_name}]
         [:label {:for "checkbox-show-recipient-name"}
          (t :badge/Showyourname)]]
        [:div.col-sm-4
         [:button {:class "btn btn-primary"
                   :on-click #(.print js/window)}
          (t :core/Print)]]])
     (if (or verified_by_obf issued_by_obf)
       (bh/issued-by-obf obf_url verified_by_obf issued_by_obf))
     [:div.row
      [:div {:class "col-md-3 badge-image"}
       [:div.row
        [:div.col-xs-12
         [:img {:src (str "/" image_file)}]]]
       (if owner?
         [:div.row
          [:div.col-xs-12
           [:div.rating
            [r/rate-it rating]]
           [:div.view-count
            (cond
              (= view_count 1) (t :badge/Viewedonce)
              (> view_count 1) (str (t :badge/Viewed) " " view_count " " (t :badge/times))
              :else (t :badge/Badgeisnotviewedyet))]]])
       [:div.row
        [:div.col-xs-12
         (if (and user-logged-in? (not owner?))
           (if congratulated?
             [:div.congratulated
              [:i {:class "fa fa-heart"}]
              (str " " (t :badge/Congratulated))]
             [:button {:class "btn btn-primary"
                       :on-click #(congratulate state)}
              [:i {:class "fa fa-heart"}]
              (str " " (t :badge/Congratulations) "!")])
           )]]]
      [:div {:class "col-md-9"}
       [:div.row
        [:div {:class "col-md-12"}
         [:h1 (:name @state)]
         (bh/issuer-label-and-link issuer_content_name issuer_content_url issuer_contact)
         (if show_recipient_name
           [:div
            (t :badge/Recipient ": " first_name " " last_name)])
         [:div
          description]
         [:h2.uppercase-header (t :badge/Criteria)]
         [:a {:href criteria_url
              :target "_blank"}
          (t :badge/Opencriteriapage)]]]
       [:div.row
        [:div.col-md-12
         {:dangerouslySetInnerHTML {:__html html_content}}]]
       (if owner?
         [:div.row
          [:div.col-md-12
           [:h3.congratulated-header
            [:i {:class "fa fa-heart"}]
            " " (t :badge/Congratulatedby) ":"]
           (into [:div ]
                 (for [congratulated-user congratulations]
                   [:a {:href "#"}
                    (:first_name congratulated-user) " " (:last_name congratulated-user)]))]])]]]))

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

