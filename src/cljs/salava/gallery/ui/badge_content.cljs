(ns salava.gallery.ui.badge-content
  (:require [reagent.core :refer [atom create-class]]
            [reagent-modals.modals :refer [close-modal!]]
            [salava.core.i18n :refer [t]]
            [salava.badge.ui.helper :as bh]
            [salava.user.ui.helper :refer [profile-link-inline]]
            [salava.core.ui.rate-it :as r]
            [salava.core.helper :refer [dump]]
            [salava.admin.ui.reporttool :refer [reporttool]]
            [salava.social.ui.follow :refer [follow-badge]]
            [salava.social.ui.badge-message :refer [badge-message-handler]]
            [salava.social.ui.badge-message-modal :refer [gallery-modal-message-info-link]]
            ))

(defn badge-content [{:keys [badge public_users private_user_count]} messages?]
  (let [{:keys [badge_content_id name image_file description issuer_content_name issuer_content_url issuer_contact issuer_image issuer_description html_content criteria_url average_rating rating_count obf_url verified_by_obf issued_by_obf creator_name creator_url creator_email creator_image creator_description message_count]} badge
        show-messages (atom messages?)
        ]
    (fn []
      [:div {:id "badge-contents"}
       (if (or verified_by_obf issued_by_obf)
         (bh/issued-by-obf obf_url verified_by_obf issued_by_obf))
       [:div.row
        [:div {:class "col-md-3 badge-image modal-left"}
         [:img {:src (str "/" image_file)}]
         (when (> average_rating 0)
           [:div.rating
            [r/rate-it average_rating]
            [:div (if (= rating_count 1)
                    (str (t :gallery/Ratedby) " " (t :gallery/oneearner))
                    (str (t :gallery/Ratedby) " " rating_count " " (t :gallery/earners)))]])
         [:div
          [gallery-modal-message-info-link show-messages badge_content_id]
          ]]
        [:div {:class "col-md-9 badge-info"}
         
         (if @show-messages
           [:div.row
            [:h1.uppercase-header (str name " - " (t :social/Messages))]
            [badge-message-handler badge_content_id]]
           [:div.row
            [:h1.uppercase-header name]
            [:div {:class "col-md-12"}
             (bh/issuer-label-image-link issuer_content_name issuer_content_url issuer_contact issuer_image)
             (bh/creator-label-image-link creator_name creator_url creator_email creator_image)
             [:div.row
              [:div {:class "col-md-12 description"}
               description]]
             [:div.row
              [:div {:class "col-md-12 badge-info"}
                  [:h2.uppercase-header (t :badge/Criteria)]
               [:div.row
                [:div.col-md-12
                 [:a {:href   criteria_url
                      :target "_blank"} (t :badge/Opencriteriapage)]]]
                  [:div.row
                   [:div.col-md-12
                    {:dangerouslySetInnerHTML {:__html html_content}}]]]]
             ]
            (if (or (> (count public_users) 0) (> private_user_count 0))
              [:div.row
               [:div.col-md-12
                [:h2.uppercase-header (t :gallery/Allrecipients)]]
               [:div {:class "col-md-12"}
                (into [:div]
                      (for [user public_users
                            :let [{:keys [id first_name last_name profile_picture]} user]]
                        (profile-link-inline id first_name last_name profile_picture)))
                (if (> private_user_count 0)
                  (if (> (count public_users) 0)
                    [:span "... " (t :core/and) " " private_user_count " " (t :core/more)]
                    [:span private_user_count " " (if (> private_user_count 1) (t :gallery/recipients) (t :gallery/recipient))]))]])])]]])))

(defn badge-content-modal-render [data reporttool-atom messages?]
  [:div {:id "badge-content"}
   [:div.modal-body
    [:div.row
     [:div.col-md-12
      [:div {:class "text-right"}
       [follow-badge (get-in data [:badge :badge_content_id]) (get-in data [:badge :followed?])]
       [:button {:type         "button"
                 :class        "close"
                 :data-dismiss "modal"
                 :aria-label   "OK"}
        [:span {:aria-hidden             "true"
                :dangerouslySetInnerHTML {:__html "&times;"}}]]]]]
    [badge-content data messages?]]
   [:div.modal-footer
    (reporttool (get-in data [:badge :badge_content_id]) (get-in data [:badge :name]) "badges" reporttool-atom)
    ]])

(defn badge-content-modal [modal-data reporttool-atom messages? init-data state]
  (create-class {:reagent-render (fn [] (badge-content-modal-render modal-data reporttool-atom messages?))
                 :component-will-unmount (fn [] (do (close-modal!)
                                                    (if (and init-data state)
                                                      (init-data state))))}))
