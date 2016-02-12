(ns salava.badge.ui.my
  (:require [reagent.core :refer [atom]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [clojure.set :as set :refer [intersection]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :as h :refer [unique-values navigate-to]]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.grid :as g]
            [salava.badge.ui.settings :as s]
            [salava.core.i18n :as i18n :refer [t]]))


(defn visibility-select-values []
  [{:value "all" :title (t :core/All)}
   {:value "public"  :title (t :core/Public)}
   {:value "shared"  :title (t :core/Shared)}
   {:value "private" :title (t :core/Private)}])

(defn order-radio-values []
  [{:value "mtime" :id "radio-date" :label (t :core/bydate)}
   {:value "name" :id "radio-name" :label (t :core/byname)}])

(defn badge-grid-form [state]
  [:div {:id "grid-filter"
         :class "form-horizontal"}
   [g/grid-search-field (t :core/Search ":")  "badgesearch" (t :core/Searchbyname) :search state]
   [g/grid-select (t :core/Show ":")  "select-visibility" :visibility (visibility-select-values) state]
   [g/grid-buttons (t :core/Tags ":")  (unique-values :tags (:badges @state)) :tags-selected :tags-all state]
   [g/grid-radio-buttons (t :core/Order ":")  "order" (order-radio-values) :order state]])

(defn badge-visible? [element state]
  (if (and
        (or (= (:visibility @state) "all")
            (= (:visibility @state) (:visibility element)))
        (or (> (count
                 (intersection
                   (into #{} (:tags-selected @state))
                   (into #{} (:tags element))))
               0)
            (= (:tags-all @state)
               true))
        (or (empty? (:search @state))
            (not= (.indexOf
                    (.toLowerCase (:name element))
                    (.toLowerCase (:search @state)))
                  -1)))
    true false))

(defn show-settings-dialog [badge-id state]
  (ajax/GET
    (str "/obpv1/badge/settings/" badge-id)
    {:handler (fn [data]
                (swap! state assoc :badge-settings (hash-map :id badge-id
                                                             :visibility (:visibility data)
                                                             :tags (:tags data)
                                                             :evidence-url (:evidence_url data)
                                                             :rating (:rating data)
                                                             :new-tag ""))
                (m/modal! [s/settings-modal data state]
                          {:size :lg}))}))

(defn badge-grid-element [element-data state]
  (let [{:keys [id image_file name description visibility]} element-data]
    [:div {:class "col-xs-12 col-sm-6 col-md-4"
           :key id}
     [:div {:class "media grid-container"}
      [:div.media-content
       (if image_file
         [:div.media-left
          [:img {:src (str "/" image_file)}]])
       [:div.media-body
        [:div.media-heading
         [:a.heading-link {:href (str "/badge/info/" id)}
          name]]
        [:div.visibility-icon
         (case visibility
           "private" [:i {:class "fa fa-lock"
                          :title (t :badge/Private)}]
           "internal" [:i {:class "fa fa-group"
                           :title (t :badge/Shared)}]
           "public" [:i {:class "fa fa-globe"
                         :title (t :badge/Public)}]
           nil)]
        [:div.media-description description]]]
      [:div {:class "media-bottom"}
       [:a {:class "bottom-link"
            :href (str "/badge/info/" id)}
        [:i {:class "fa fa-share-alt"}]
        [:span (t :badge/Share)]]
       [:a {:class "bottom-link pull-right"
            :href "#"
            :on-click #(show-settings-dialog id state)}
        [:i {:class "fa fa-cog"}]
        [:span (t :badge/Settings)]]]]]))

(defn badge-grid [state]
  (let [badges (:badges @state)
        order (:order @state)]
    (into [:div {:class "row"
                 :id    "grid"}]
          (for [element-data (sort-by (keyword order) badges)]
            (if (badge-visible? element-data state)
              (badge-grid-element element-data state))))))

(defn update-status [id new-status state]
  (ajax/POST
    (str "/obpv1/badge/set_status/" id)
    {:params  {:status new-status}
     :handler (fn []
                (let [badge (first (filter #(= id (:id %)) (:pending @state)))]
                  (swap! state assoc :pending (remove #(= badge %) (:pending @state)))
                  (if (= new-status "accepted")
                    (swap! state assoc :badges (conj (:badges @state) badge)))))}))

(defn badge-pending [{:keys [id image_file name description]} state]
  [:div.row {:key id}
   [:div.col-md-12
    [:div.badge-container-pending
     [:div.row
      [:div.col-md-12
       [:div.media
        [:div.pull-left
         [:img.badge-image {:src (str "/" image_file)}]]
        [:div.media-body
         [:h4.media-heading
          name]
         [:div
          description]]]]]
     [:div {:class "row button-row"}
      [:div.col-md-12
       [:button {:class "btn btn-primary"
                 :on-click #(update-status id "accepted" state)}
        (t :badge/Acceptbadge)]
       [:button {:class "btn btn-warning"
                 :on-click #(update-status id "declined" state)}
        (t :badge/Declinebadge)]]]]]])

(defn badges-pending [state]
  (into [:div {:id "pending-badges"}]
        (for [badge (:pending @state)]
          (badge-pending badge state))))

(defn welcome-text []
  [:div.panel
   [:div.panel-body
    [:h2.uppercase-header (t :core/WelcometoOpenBadgePassport)]
    [:div.text
     [:p "Using Open Badge Passport could not be easier:"]
     [:ol.welcome-text
      [:li
       "Add a " [:a {:href "/user/edit_profile"} "profile picture"]
       ", a short bio or contact information to your "
       [:a {:href "/user/edit_profile"} "profile"] "."]
      [:li
       "Do you already have Open Badges saved to Mozilla Backpack? "
       [:a {:href "/badge/import"} "Import your badges"]
       " to Open Badge Passport. "
       [:b "NB! Remember to add your Backpack email to the " [:a {:href "/user/edit/email-addresses"} "email addresses"] "."]]
      [:li
       "No badges yet? "
       [:b "Earn your \"Open Badge Passport - Member\" badge " [:a {:href "/gallery/getbadge"} "here!"]]]
      [:li
       [:a {:href "/page/mypages"} "Create a page"]
       " to display your badges and share it with others in Social Media or in "
       [:a {:href "/gallery/pages"} "in the Gallery"] "."]]]]])

(defn content [state]
  [:div {:class "my-badges"}
   [m/modal-window]
   (if (and (empty? (:pending @state)) (empty? (:badges @state)))
     [welcome-text]
     [:div
      [badges-pending state]
      [badge-grid-form state]
      [badge-grid state]])])

(defn init-data [state]
  (ajax/GET
    "/obpv1/badge"
    {:handler (fn [data]
                (swap! state assoc :badges (filter #(= "accepted" (:status %)) data))
                (swap! state assoc :pending (filter #(= "pending" (:status %)) data)))}))

(defn handler [site-navi]
  (let [state (atom {:badges []
                     :pending []
                     :visibility "all"
                     :order ""
                     :tags-all true
                     :tags-selected []})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
