(ns salava.gallery.ui.badge-content
  (:require [reagent.core :refer [atom create-class]]
            [reagent-modals.modals :refer [close-modal!]]
            [salava.core.i18n :refer [t]]
            [salava.badge.ui.helper :as bh]
            [salava.user.ui.helper :refer [profile-link-inline]]
            [salava.core.ui.rate-it :as r]
            [salava.core.helper :refer [dump]]
            [salava.admin.ui.reporttool :refer [reporttool]]
            [salava.social.ui.badge-message :refer [badge-message-handler]]
            ))

(defn badge-content [{:keys [badge public_users private_user_count]}]
  (let [{:keys [name image_file description issuer_content_name issuer_content_url issuer_contact issuer_image issuer_description html_content criteria_url average_rating rating_count obf_url verified_by_obf issued_by_obf creator_name creator_url creator_email creator_image creator_description]} badge]
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
                  (str (t :gallery/Ratedby) " " rating_count " " (t :gallery/earners)))]])]
      [:div {:class "col-md-9 badge-info"}
       [:div.row
        [:div {:class "col-md-12"}
         [:h1.uppercase-header name]
         (bh/issuer-label-image-link issuer_content_name issuer_content_url issuer_contact issuer_image)
         
         (bh/creator-label-image-link creator_name creator_url creator_email creator_image)
         
         
         [:div.row
          [:div {:class "col-md-12 description"}
           description]]]]
       [:div.row
        [:div {:class "col-md-12 badge-info"}
         [:h2.uppercase-header (t :badge/Criteria)]
         [:div.row
          [:div.col-md-12
           [:a {:href criteria_url
                :target "_blank"} (t :badge/Opencriteriapage)]]]
         [:div.row
          [:div.col-md-12
           {:dangerouslySetInnerHTML {:__html html_content}}]]]]
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
               [:span private_user_count " " (if (> private_user_count 1) (t :gallery/recipients) (t :gallery/recipient))]))]])]]]))

(defn badge-content-modal-render [data reporttool-atom]
  [:div {:id "badge-content"}
   [:div.modal-body
    [:div.row
     [:div.col-md-12
      [:button {:type         "button"
                :class        "close"
                :data-dismiss "modal"
                :aria-label   "OK"}
       [:span {:aria-hidden             "true"
               :dangerouslySetInnerHTML {:__html "&times;"}}]]]]
    (badge-content data)]
   [:div.modal-footer
    
    [:button {:type         "button"
              :class        "btn btn-primary"
              :data-dismiss "modal"}
     (t :core/Close)]
    [badge-message-handler (get-in data [:badge :badge_content_id])]
    (reporttool (get-in data [:badge :badge_content_id]) (get-in data [:badge :name]) "badges" reporttool-atom)
    ]])

(defn badge-content-modal [modal-data reporttool-atom]
  (create-class {:reagent-render (fn [] (badge-content-modal-render modal-data reporttool-atom))
                 :component-will-unmount (fn [] (close-modal!))}))
