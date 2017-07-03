(ns salava.gallery.ui.modal
  (:require [reagent.core :refer [atom create-class cursor]]
            [reagent-modals.modals :refer [close-modal!]]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.i18n :refer [t]]
            [salava.badge.ui.helper :as bh]
            [salava.user.ui.helper :refer [profile-link-inline-modal]]
            [salava.core.ui.rate-it :as r]
            [clojure.string :as s]
            [salava.core.helper :refer [dump]]
            [salava.admin.ui.reporttool :refer [reporttool1]]
            [salava.social.ui.follow :refer [follow-badge]]
            [salava.social.ui.badge-message :refer [badge-message-handler]]
            [salava.core.ui.helper :refer [path-for private?]]
            [salava.core.ui.content-language :refer [init-content-language content-language-selector content-setter]]
            [salava.social.ui.badge-message-modal :refer [gallery-modal-message-info-link]]
            ))


(defn tag-parser [tags]
  (if tags
    (s/split tags #",")))




(defn content [state show-messages]
  (let [{:keys [badge public_users private_user_count reload-fn]} @state
        {:keys [badge_id content average_rating rating_count obf_url verified_by_obf issued_by_obf]} badge
        selected-language (cursor state [:content-language])
        {:keys [name description tags criteria_content image_file image_file issuer_content_name issuer_content_url issuer_contact issuer_image issuer_description criteria_url  creator_name creator_url creator_email creator_image creator_description message_count]} (content-setter @selected-language content)
        tags (tag-parser tags)]
    [:div
     [:div.col-xs-12
      [:div.pull-right
       [follow-badge badge_id]]]
     [:div {:id "badge-contents"}
      [:div {:class "pull-right text-right"}]
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
         [gallery-modal-message-info-link show-messages badge_id]
         ]]
       [:div {:class "col-md-9 badge-info"}
        (content-language-selector selected-language content)
        (if @show-messages
          [:div.rowmessage
           [:h1.uppercase-header (str name " - " (t :social/Messages))]
           [badge-message-handler badge_id reload-fn]]
          
          [:div.rowcontent
           [:h1.uppercase-header name]
           [:div
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
                {:dangerouslySetInnerHTML {:__html criteria_content}}]]]]
            ]
           (if (not (empty? tags))
             (into [:div]
                   (for [tag tags]
                     [:p {:id "tag"}
                      (str "#" tag )])))
           (if (or (> (count public_users) 0) (> private_user_count 0))
             [:div.recipients
              [:div

               [:h2.uppercase-header (t :gallery/Allrecipients)]]
              [:div
               (into [:div]
                     (for [user public_users
                           :let [{:keys [id first_name last_name profile_picture]} user]]
                       (profile-link-inline-modal id first_name last_name profile_picture)))
               (if (> private_user_count 0)
                 (if (> (count public_users) 0)
                   [:span "... " (t :core/and) " " private_user_count " " (t :core/more)]
                   [:span private_user_count " " (if (> private_user_count 1) (t :gallery/recipients) (t :gallery/recipient))]))]])])
        ]]
      [reporttool1 badge_id name "badges"]]]
    ))


(defn init-data [badge-id state]
  (ajax/GET
     (path-for (str "/obpv1/gallery/public_badge_content/" badge-id) true)
     {:handler (fn [data]               
                 (reset! state (assoc data 
                                      :permission "success"
                                      :reload-fn (:reload-fn @state)
                                      :content-language (init-content-language  (get-in data [:badge :content])))))}
     (fn [] (swap! state assoc :permission "error")))
  )

(defn handler [params]
  (let [badge-id (:badge-id params)
        state (atom {:permission "initial"
                     :badge {:badge_id badge-id}
                     :public_users []
                     :private_user_count 0
                     :badge-small-view false
                     :pages-small-view true
                     :reload-fn (or (:reload-fn params) nil)})
        user (session/get :user)
        show-messages (atom (or (:show-messages params) false))]
    (init-data badge-id state)
    (fn []
      (content state show-messages))))

(def ^:export modalroutes
  {:gallery {:badges handler}}
  )
