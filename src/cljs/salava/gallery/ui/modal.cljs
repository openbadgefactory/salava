(ns salava.gallery.ui.modal
  (:require [reagent.core :refer [atom create-class cursor]]
            [reagent-modals.modals :refer [close-modal!]]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.i18n :refer [t]]
            [salava.badge.ui.helper :as bh]
            [salava.badge.ui.modal :as bm]
            [salava.user.ui.helper :refer [profile-link-inline-modal]]
            [salava.core.ui.rate-it :as r]
            [clojure.string :as s]
            [salava.core.helper :refer [dump]]
            [salava.admin.ui.reporttool :refer [reporttool1]]
            [salava.social.ui.follow :refer [follow-badge]]
            [salava.social.ui.badge-message :refer [badge-message-handler]]
            [salava.core.ui.helper :refer [path-for private? plugin-fun]]
            [salava.core.ui.content-language :refer [init-content-language content-language-selector content-setter]]
            [salava.social.ui.badge-message-modal :refer [gallery-modal-message-info-link]]
            [salava.gallery.ui.block :as block]))

(defn tag-parser [tags]
  (if tags
    (s/split tags #",")))

(defn content [state show-messages]
  (let [{:keys [badge public_users private_user_count reload-fn hide-info-link]} @state
        {:keys [badge_id gallery_id content average_rating rating_count obf_url verified_by_obf issued_by_obf endorsement_count]} badge
        selected-language (cursor state [:content-language])
        {:keys [name description tags alignment criteria_content image_file image_file issuer_content_id issuer_content_name issuer_content_url issuer_contact issuer_image issuer_description criteria_url creator_content_id creator_name creator_url creator_email creator_image creator_description message_count]} (content-setter @selected-language content)
        tags (tag-parser tags)]

    [:div#gallery-modal
     (when (session/get :user) [bm/follow-verified-bar badge "gallery" @show-messages])
     [:div {:id "badge-contents"}
      [:div.row.flip
       [:div {:class "col-md-3 badge-image modal-left"}
        [:div.badge-image [:img {:src (str "/" image_file) :alt name}]]
        (when (> average_rating 0)
          [:div.rating
           [r/rate-it "rateit1" average_rating]
           [:div (if (= rating_count 1)
                   (str (t :gallery/Ratedby) " " (t :gallery/oneearner))
                   (str (t :gallery/Ratedby) " " rating_count " " (t :gallery/earners)))]])
        #_[:div
           (when (and (session/get :user) (not hide-info-link)) [gallery-modal-message-info-link show-messages badge_id])]

        (when-not @show-messages (bm/badge-endorsement-modal-link badge_id endorsement_count))]

       [:div {:class "col-md-9 badge-info"}
        (if @show-messages
          [:div.rowmessage
           [:h1.uppercase-header (str name " - " (t :social/Messages))]
           [badge-message-handler badge_id reload-fn]]


          [:div.rowcontent
           [:h1.uppercase-header name]
           [:div
            (if (< 1 (count content))
              [:div.inline [:label (t :core/Languages)": "](content-language-selector selected-language content)])
            (bm/issuer-modal-link issuer_content_id issuer_content_name)
            (bm/creator-modal-link creator_content_id creator_name)
            [:div.row
             [:div {:class "col-md-12 description"}
              description]]

            (when-not (empty? alignment)
              [:div.row
               [:div.col-md-12
                [:h2.uppercase-header (t :badge/Alignments)]
                (doall
                  (map (fn [{:keys [name url description]}]
                         [:p {:key url}
                          [:a {:target "_blank" :rel "noopener noreferrer" :href url} name] [:br] description])
                       alignment))]])

            [:div.row
             [:div {:class "col-md-12 badge-info"}
              [:h2.uppercase-header (t :badge/Criteria)]
              [:div.row
               [:div.col-md-12
                [:a {:href   criteria_url
                     :target "_blank"} (t :badge/Opencriteriapage)]]]
              [:div.row
               [:div.col-md-12
                {:dangerouslySetInnerHTML {:__html criteria_content}}]]]]]

           (if (not (empty? tags))
             (into [:div]
                   (for [tag tags]
                     [:p {:id "tag"}
                      (str "#" tag)])))
           (if (or (> (count public_users) 0) (> private_user_count 0))
            (into [:div]
              (for [f (plugin-fun (session/get :plugins) "block" "badge_recipients")]
                [f  {:data {:private_user_count private_user_count :public_users public_users :all_recipients_count (+ private_user_count (count public_users))}}])))])

        (when (and gallery_id (not @show-messages) (session/get :user))
          (into [:div]
                (for [f (plugin-fun (session/get :plugins) "block" "gallery_badge")]
                  [f gallery_id badge_id])))]]

      (if (and badge_id name)
        [reporttool1 badge_id name "badges"])]]))

(defn init-data [gallery-id badge-id state]
  (ajax/GET
    (path-for (str "/obpv1/gallery/public_badge_content/" (or gallery-id 0) "/" badge-id) true)
    {:handler (fn [data]
                (reset! state (assoc data
                                :permission "success"
                                :reload-fn (:reload-fn @state)
                                :hide-info-link (:hide-info-link @state)
                                :content-language (init-content-language  (get-in data [:badge :content])))))}
    (fn [] (swap! state assoc :permission "error"))))

(defn handler [params]
  (let [badge-id (:badge-id params)
        gallery-id (:gallery-id params)
        state (atom {:permission "initial"
                     :badge {:badge_id badge-id :gallery_id gallery-id}
                     :public_users []
                     :private_user_count 0
                     :badge-small-view false
                     :pages-small-view true
                     :reload-fn (or (:reload-fn params) nil)
                     :hide-info-link (or (:hide-info-link params) false)})
        user (session/get :user)
        show-messages (atom (or (:show-messages params) false))]
    (init-data gallery-id badge-id state)
    (fn []
      (content state show-messages))))

(def ^:export modalroutes
  {:gallery {:badges handler
             :profiles block/allprofilesmodal}})
