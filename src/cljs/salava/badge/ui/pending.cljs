(ns salava.badge.ui.pending
  (:require [reagent.core :refer [atom cursor]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.ui.content-language :refer [init-content-language content-language-selector content-setter]]
            [salava.badge.ui.helper :as bh]
            [salava.badge.ui.modal :as bm]
            [salava.core.time :refer [date-from-unix-time]]
            [salava.metabadge.ui.metabadge :as mb]))


(defn- init-badge-preview [state]
  (ajax/GET
    (path-for (str "/obpv1/badge/info/" (:id @state)))
    {:handler (fn [data]
                (swap! state assoc :result data
                       :id (:id @state)
                       :content-language (init-content-language (:content data))))}))

(defn num-days-left [timestamp]
  (int (/ (- timestamp (/ (.now js/Date) 1000)) 86400)))


(defn- show-more-content [state]
  (fn []
    (let [ data (:result @state)
           selected-language (cursor state [:content-language])
           {:keys [id badge_id issued_on expires_on issued_by_obf verified_by_obf obf_url first_name last_name content]}  data
           expired?                                                                 (bh/badge-expired? expires_on)
           show-recipient-name-atom                                                 (cursor state [:result :show_recipient_name])
           {:keys [name description tags alignment criteria_content image_file issuer_content_id issuer_content_name issuer_content_url issuer_contact issuer_image issuer_description criteria_url creator_content_id creator_name creator_url creator_email creator_image creator_description message_count endorsement_count]} (content-setter @selected-language content)]
      [:div#badge-info {:class "preview-badge" :style {:display (:show-result @state)}}
       [:div.row.flip
        [:div.col-md-9.badge-info
         (if (< 1 (count content))
           [:div.inline [:label (t :core/Languages)": "](content-language-selector selected-language content)])
         (bm/issuer-modal-link issuer_content_id issuer_content_name)
         (bm/creator-modal-link creator_content_id creator_name)
         (if (and issued_on (> issued_on 0))
           [:div [:label (t :badge/Issuedon) ": "]  (date-from-unix-time (* 1000 issued_on))])
         (if (and expires_on (not expired?))
           [:div [:label (t :badge/Expireson) ": "] (str (date-from-unix-time (* 1000 expires_on)) " ("(num-days-left expires_on) " " (t :badge/days)")")])
         (if (pos? @show-recipient-name-atom)
           [:div [:label (t :badge/Recipient) ": "]  first_name " " last_name])
         [:div {:class "criteria-html"}
          [:h2.uppercase-header (t :badge/Criteria)]
          [:a {:href criteria_url :target "_blank"} (t :badge/Opencriteriapage) "..."]
          [:div {:dangerouslySetInnerHTML {:__html criteria_content}}]]
         ]]])))

(defn- show-more [state]
  (fn []
    [:div
     [:br]
     [:a {:href "#"
          :style {:display (:show-link @state)}
          :on-click #(do
                       (swap! state assoc :show-result "block"
                              :show-link "none")
                       (init-badge-preview state))
          }(t :admin/Showmore)]
     [show-more-content state]]))


(defn pending-badge-content [{:keys [id image_file name description assertion_url meta_badge meta_badge_req issuer_content_name issuer_content_url issued_on issued_by_obf verified_by_obf obf_url]}]
  (let [state (atom {:id id
                     :show-result "none"
                     :show-link "block"
                     :result {}})]
    (init-badge-preview state)
    (fn []
      (let [data (:result @state)
            selected-language (cursor state [:content-language])
            {:keys [badge_id content assertion_url]} data
            {:keys [name description tags alignment criteria_content image_file issuer_content_id issuer_content_name issuer_content_url issuer_contact issuer_image issuer_description criteria_url creator_content_id creator_name creator_url creator_email creator_image creator_description message_count endorsement_count ]} (content-setter @selected-language content)]
        [:div
         (if (or verified_by_obf issued_by_obf)
           [:div.row.flip
            (bh/issued-by-obf obf_url verified_by_obf issued_by_obf)])
         [:div.row.flip
          [:div.col-md-3.badge-image
           [:img.badge-image {:src (str "/" image_file)}]
           (bm/badge-endorsement-modal-link badge_id endorsement_count)
           [mb/metabadge assertion_url]]
          [:div.col-md-9
           [:h4.media-heading name]

           ;METABADGE
           [:div (bh/meta-badge meta_badge meta_badge_req)]
           [:div assertion_url]
           [:div description]
           [show-more state]]] ]))))
