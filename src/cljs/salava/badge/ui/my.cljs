(ns salava.badge.ui.my
  (:require
            [reagent.core :refer [atom]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [clojure.set :as set :refer [intersection]]
            [clojure.string :refer [upper-case]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :as h :refer [unique-values navigate-to path-for]]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.grid :as g]
            [salava.badge.ui.settings :as s]
            [salava.badge.ui.helper :as bh]
            [salava.core.time :refer [unix-time date-from-unix-time]]
            [salava.core.i18n :as i18n :refer [t]]))

(defn init-data [state]
  (ajax/GET
    (path-for "/obpv1/badge" true)
    {:handler (fn [data]
                (swap! state assoc :badges (filter #(= "accepted" (:status %)) data)
                                   :pending (filter #(= "pending" (:status %)) data)
                                   :initializing false))}))

(defn visibility-select-values []
  [{:value "all" :title (t :core/All)}
   {:value "public"  :title (t :core/Public)}
   {:value "internal"  :title (t :core/Registeredusers)}
   {:value "private" :title (t :core/Onlyyou)}])

(defn order-radio-values []
  [{:value "mtime" :id "radio-date" :label (t :core/bydate)}
   {:value "name" :id "radio-name" :label (t :core/byname)}
   {:value "issuer_content_name" :id "radio-issuer" :label (t :core/byissuername)}
   {:value "expires_on" :id "radio-expiratio" :label (t :core/byexpirationdate)}])

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
    (path-for (str "/obpv1/badge/settings/" badge-id) true)
    {:handler (fn [data]
                (swap! state assoc :badge-settings (hash-map :id badge-id
                                                             :visibility (:visibility data)
                                                             :tags (:tags data)
                                                             :evidence-url (:evidence_url data)
                                                             :rating (:rating data)
                                                             :new-tag ""))
                (m/modal! [s/settings-modal data state init-data]
                          {:size :lg}))}))

(defn badge-grid-element [element-data state]
  (let [{:keys [id image_file name description visibility expires_on revoked]} element-data
        expired? (bh/badge-expired? expires_on)
        badge-link (path-for (str "/badge/info/" id))]
    [:div {:class "col-xs-12 col-sm-6 col-md-4"
           :key id}
     [:div {:class "media grid-container"}
      [:div {:class (str "media-content " (if expired? "media-expired"))}
       (if image_file
         [:div.media-left
          [:a {:href badge-link} [:img.badge-img {:src (str "/" image_file)}]]])
       [:div.media-body
        [:div.media-heading
         [:a.heading-link {:href badge-link} name]]
        [:div.visibility-icon
         (case visibility
           "private" [:i {:class "fa fa-lock"
                          :title (t :badge/Private)}]
           "internal" [:i {:class "fa fa-group"
                           :title (t :badge/Shared)}]
           "public" [:i {:class "fa fa-globe"
                         :title (t :badge/Public)}]
           nil)]
        (if expires_on
          [:div.media-expires (if expired? (t :badge/Expiredon) (t :badge/Expireson)) ": " (date-from-unix-time (* expires_on 1000))])
        [:div.media-description
         description]]]
      [:div {:class "media-bottom"}
       (cond
         expired? [:div.expired [:i {:class "fa fa-history"}] " " (t :badge/Expired)
                   [:a {:class "bottom-link pull-right" :href "#" :on-click #(do (.preventDefault %) (show-settings-dialog id state))}
                    [:i {:class "fa fa-cog"}]
                    [:span (t :badge/Settings)]]]
         revoked [:div.expired [:i {:class "fa fa-ban"}] " " (t :badge/Revoked)
                  [:a {:class "bottom-link pull-right" :href "#" :on-click #(do (.preventDefault %) (show-settings-dialog id state))}
                   [:i {:class "fa fa-cog"}]
                   [:span (t :badge/Settings)]]]
         :else [:div
                [:a {:class "bottom-link" :href (path-for (str "/badge/info/" id))}
                 [:i {:class "fa fa-share-alt"}]
                 [:span (t :badge/Share)]]
                [:a {:class "bottom-link pull-right" :href "#" :on-click #(do (.preventDefault %) (show-settings-dialog id state))}
                 [:i {:class "fa fa-cog"}]
                 [:span (t :badge/Settings)]]])]]]))

(defn badge-grid [state]
  (let [badges (:badges @state)
        order (keyword (:order @state))
        badges (case order
                 (:mtime) (sort-by order > badges)
                 (:name :issuer_content_name) (sort-by (comp clojure.string/upper-case str order) badges)
                 (:expires_on) (->> badges
                                    (sort-by order)
                                    (partition-by #(nil? (% order)))
                                    reverse
                                    flatten)
                 badges)]
    (into [:div {:class "row"
                 :id    "grid"}]
          (for [element-data badges]
            (if (badge-visible? element-data state)
              (badge-grid-element element-data state))))))

(defn update-status [id new-status state]
  (ajax/POST
    (path-for (str "/obpv1/badge/set_status/" id) true)
    {:params  {:status new-status}
     :handler (fn []
                (let [badge (first (filter #(= id (:id %)) (:pending @state)))]
                  (swap! state assoc :pending (remove #(= badge %) (:pending @state)))
                  (if (= new-status "accepted")
                    (swap! state assoc :badges (conj (:badges @state) badge)))))}))

(defn badge-pending [{:keys [id image_file name description meta_badge meta_badge_req issuer_content_name issuer_content_url issued_on issued_by_obf verified_by_obf obf_url]} state]
  [:div.row {:key id}
   [:div.col-md-12
    [:div.badge-container-pending
     (if (or verified_by_obf issued_by_obf)
       (bh/issued-by-obf obf_url verified_by_obf issued_by_obf))
     [:div.row
      [:div.col-md-12
       [:div.media
        [:div.pull-left
         [:img.badge-image {:src (str "/" image_file)}]]
        [:div.media-body
         [:h4.media-heading
          name]
         [:div
          [:a {:href issuer_content_url :target "_blank"} issuer_content_name]]
         [:div (date-from-unix-time (* 1000 issued_on))]

         ;METABADGE
         [:div (bh/meta-badge meta_badge meta_badge_req)]

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
    [:h1.uppercase-header (t :core/WelcometoOpenBadgePassport)]
    [:div.text
     [:p (t :badge/Welcometext1) ":"]
     [:ol.welcome-text
      [:li
       (t :badge/Add) " " [:a {:href (path-for "/user/edit/profile")} (t :badge/Profilepicture)]
       "," (t :badge/Welcometextinfo1) " "
       [:a {:href (path-for (str "/user/profile/" (session/get-in [:user :id])))} "profile"] "."]
      [:li
       (t :badge/Welcometextinfo2) " "
       [:a {:href (path-for "/badge/import")} (t :badge/Importyourbages)]
       " "(t :badge/Toopenbagdgepassport) ". "
       [:b (t :badge/Rememberaddyouremail) " " [:a {:href (path-for "/user/edit/email-addresses")} (t :badge/Mailaddresses)] "."]]
      [:li
       [:a {:href (path-for "/page/mypages")} (t :badge/Createpage)]
       " "(t :badge/Welcometextinfo3) " "
       [:a {:href (path-for "/gallery/pages")} (t :badge/Ingallery)] "."]]]]])

(defn content [state]
  [:div {:id "my-badges"}
   [m/modal-window]
   (if (:initializing @state)
     [:div.ajax-message
      [:i {:class "fa fa-cog fa-spin fa-2x "}]
      [:span (str (t :core/Loading) "...")]]
     (if (and (empty? (:pending @state)) (empty? (:badges @state)))
       [welcome-text]
       [:div
        [badges-pending state]
        [badge-grid-form state]
        [badge-grid state]]))])



(defn handler [site-navi]
  (let [state (atom {:badges []
                     :pending []
                     :visibility "all"
                     :order "mtime"
                     :tags-all true
                     :tags-selected []
                     :initializing true})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
