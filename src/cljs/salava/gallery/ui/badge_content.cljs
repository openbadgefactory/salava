(ns salava.gallery.ui.badge-content
  (:require [reagent.core :refer [atom create-class]]
            [salava.core.i18n :refer [t]]
            [salava.badge.ui.helper :as bh]
            [salava.core.ui.rate-it :as r]
            [salava.core.helper :refer [dump]]))

(defn badge-content-modal [{:keys [badge public_users private_user_count]}]
  (let [{:keys [name image_file description issuer_content_name issuer_content_url issuer_contact html_content criteria_url average_rating rating_count]} badge]
    [:div {:id "badge-content"}
     [:div.modal-body
      [:div.row
       [:div.col-md-12
        [:button {:type         "button"
                  :class        "close"
                  :data-dismiss "modal"
                  :aria-label   "OK"
                  }
         [:span {:aria-hidden             "true"
                 :dangerouslySetInnerHTML {:__html "&times;"}}]]]]
      [:div.row
       [:div {:class "col-md-3 badge-image modal-left"}
        [:img {:src (str "/" image_file)}]
        (when (> average_rating 0)
          [:div.rating
           [r/rate-it average_rating]
           [:div (if (= rating_count 1)
                   (str (t :gallery/Ratedby) " " (t :gallery/oneearner))
                   (str (t :gallery/Ratedby) " " rating_count " " (t :gallery/earners)))]])]
       [:div {:class "col-md-9 badge-content"}
        [:div.row
         [:div {:class "col-md-12 badge-info"}
          [:h2.uppercase-header name]
          (bh/issuer-label-and-link issuer_content_name issuer_content_url issuer_contact)
          [:div.row
           [:div.col-md-12
            description]]]]
        [:div.row
         [:div {:class "col-md-12 badge-info"}
          [:h3 (t :badge/Criteria)]
          [:div.row
           [:div.col-md-12
            [:a {:href criteria_url
                 :target "_blank"} (t :badge/Opencriteriapage)]]]
          [:div.row
           [:div.col-md-12
            {:dangerouslySetInnerHTML {:__html html_content}}]]]]
        [:div.row
         [:div.col-md-12
          [:h3 (t :gallery/Recipients)]]]
        (if (> (count public_users) 0)
          [:div.row
           [:div {:class "col-md-12"}
            (into [:div]
                  (for [user public_users]
                    [:a {:href "#"
                         :class "badge-recipient"}
                     (:first_name user) " " (:last_name user)]))
            (if (> private_user_count 0)
              [:span
               "... " (t :core/and) " " private_user_count " " (t :core/more)])]])
        ]]]
     [:div.modal-footer
      [:button {:type         "button"
                :class        "btn btn-primary"
                :data-dismiss "modal"}
       (t :core/Close)]]]))