(ns salava.admin.ui.admintool-content
  (:require [reagent.core :refer [atom cursor]]
            [reagent-modals.modals :as m]
            [clojure.string :refer [trim]]
            [salava.badge.ui.helper :as bh]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.grid :as g]
            [salava.core.ui.helper :refer [path-for current-path navigate-to input-valid?]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.admin.ui.helper :refer [valid-item-type? valid-item-id? checker admin?]]))




(defn admin-modal-container [item-type item-id mail user-id state init-data {:keys [badge public_users private_user_count]}]
  (let [{:keys [name image_file description issuer_content_name issuer_content_url issuer_contact issuer_image issuer_description html_content criteria_url average_rating rating_count obf_url verified_by_obf issued_by_obf creator_name creator_url creator_email creator_image creator_description]} badge]
    [:div {:id "badge-contents"}
     (if (or verified_by_obf issued_by_obf)
       (bh/issued-by-obf obf_url verified_by_obf issued_by_obf))
     [:div.row
      [:div {:class "col-md-3 badge-image modal-left"}
       [:img {:src (str "/" image_file)}]
       (when (> average_rating 0)
         [:div.rating
          ;[r/rate-it average_rating]
          [:div (if (= rating_count 1)
                  (str (t :gallery/Ratedby) " " (t :gallery/oneearner))
                  (str (t :gallery/Ratedby) " " rating_count " " (t :gallery/earners)))]])]
      [:div {:class "col-md-9 badge-info"}
       [:div.row
        [:div {:class "col-md-12"}
         [:h1.uppercase-header name]
         [:div.row
          [:div {:class "col-md-5 col-md-7"}
           (bh/issuer-image issuer_image)]
          (bh/issuer-label-and-link issuer_content_name issuer_content_url issuer_contact)
          (bh/issuer-description issuer_description)]
         [:div (bh/creator-image creator_image)
          (bh/creator-label-and-link creator_name creator_url creator_email)
          (bh/creator-description creator_description)]
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
           
           (if (> private_user_count 0)
             (if (> (count public_users) 0)
               [:span "... " (t :core/and) " " private_user_count " " (t :core/more)]
               [:span private_user_count " " (if (> private_user_count 1) (t :gallery/recipients) (t :gallery/recipient))]))]])]]]))

(defn admin-modal-container1 [item-type item-id mail user-id state init-data data]
  [:div.row
   [:h1.uppercase-header "Peukaloija"]
   [:div {:class "row"}
    [:div {:class "col-md-12 sub-heading"}
     "Piilota"]
    [:div.col-md-12
     "tähän jotain siistii"]]
   [:div.row
    [:div {:class "col-md-12 badge-info"}
     [:h2.uppercase-header "Piilota"]
     [:div.row
      [:div.col-md-12
       [:a {:href "#"
            :target "_blank"} "kkk"]]]
     ]]
   ])


(defn admin-modal [item-type item-id mail user-id state init-data data]
  [:div
   [:div.modal-header
      [:button {:type         "button"
                :class        "close"
                :data-dismiss "modal"
                :aria-label   "OK"}
       [:span {:aria-hidden             "true"
               :dangerouslySetInnerHTML {:__html "&times;"}}]]]
   [:div.modal-body
      (admin-modal-container item-type item-id mail user-id state init-data data)
      ]
     [:div.modal-footer
      [:button {:type         "button"
                :class        "btn btn-primary"
                :data-dismiss "modal"}
       "close"]]])
