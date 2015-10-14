(ns salava.badge.ui.info
  (:require [reagent.core :refer [atom]]
            [reagent.session :as session]
            [salava.core.ui.layout :as layout]
            [clojure.walk :refer [keywordize-keys]]
            [ajax.core :refer [GET POST]]
            [salava.core.i18n :refer [t]]))

(defn toggle-visibility [state]
  (let [id (:id @state)
        new-value (if (= (:visibility @state) "private")
                         "public"
                         "private")]
    (POST
      (str (session/get :apihost) "/obpv1/badge/set_visibility/" id)
      {:params {:visibility new-value}
       :handler (fn []
                  (swap! state assoc :visibility new-value))})))

(defn toggle-recipient-name [state]
  (let [id (:id @state)
        new-value (not (:show_recipient_name @state))]
    (POST
      (str (session/get :apihost) "/obpv1/badge/toggle_recipient_name/" id)
      {:params {:show-recipient-name new-value}
       :handler (fn []
                  (swap! state assoc :show_recipient_name new-value))})))


(defn content [state]
  [:div {:class ""}
         [:div.row
          [:div.col-sm-4
           [:input {:type "checkbox"
                    :id "checkbox-share"
                    :on-change #(toggle-visibility state)
                    :checked (= "public" (:visibility @state))}]
           [:label {:for "checkbox-share"}
            (t :badges/publish-and-share)]]
          [:div.col-sm-4
           [:input {:type "checkbox"
                    :on-change #(toggle-recipient-name state)
                    :id "checkbox-show-recipient-name"
                    :checked (:show_recipient_name @state)}]
           [:label {:for "checkbox-show-recipient-name"}
            (t :badges/show-your-name)]]
          [:div.col-sm-4
           [:button {:class "btn btn-primary"
                     :on-click #(.print js/window)}
            (t :badges/print)]]]
         [:div.row
          [:div {:class "col-md-3 badge-image"}
           [:img {:src (str "/" (:image_file @state))}]]
          [:div {:class "col-md-9"}
           [:div.row
            [:div {:class "col-md-12 badge-info"}
             [:h1 (:name @state)]
             [:div.issuer
              [:label
               (str (t :badges/issued-by) ": ")]
              [:a {:href (:issuer_url @state)
                   :target "_blank"} (:issuer_name @state)] " / " [:a {:href (str "mailto:" (:issuer_contact @state))} (:issuer_contact @state)]]
             (if (:show_recipient_name @state)
               [:div
                (str (t :badges/recipient) ": " (:first_name @state) " " (:last_name @state))])
             [:div]
             [:div
              (:description @state)]
             [:h2 (t :badges/criteria)]
             [:a {:href (:criteria_url @state)
                  :target "_blank"}
              (t :badges/open-criteria-page)]]]]]])


(defn handler [site-navi params]
  (let [id (:badge-id params)
        state (atom {})
        init-data (fn []
                    (GET
                      (str (session/get :apihost) "/obpv1/badge/info/" id)
                      {:handler (fn [data]
                                  (reset! state (keywordize-keys data)))}))]
    (init-data)
    (fn []
      (layout/default site-navi (content state)))))

