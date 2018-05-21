(ns salava.badge.ui.my
  (:require
            [reagent.core :refer [atom]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [clojure.set :as set :refer [intersection]]
            [clojure.string :refer [upper-case]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :as h :refer [unique-values navigate-to path-for  not-activated?]]
            [salava.core.ui.notactivated :refer [not-activated-banner]]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.grid :as g]
            [salava.badge.ui.settings :as s]
            [salava.badge.ui.helper :as bh]
            [salava.core.helper :refer [dump]]
            [salava.extra.application.ui.helper :refer [application-plugin?]]
            [salava.core.time :refer [unix-time date-from-unix-time]]
            [salava.core.i18n :as i18n :refer [t]]))


(defn init-data [state]
  (ajax/GET
    (path-for "/obpv1/badge" true)
    {:handler (fn [data]
                (swap! state assoc :badges (filter #(= "accepted" (:status %)) data)
                                   :pending () ;(filter #(= "pending" (:status %)) data)
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

                (swap! state assoc :badge-settings data (assoc data :new-tag ""))
                (m/modal! [s/settings-modal data state init-data]
                          {:size :lg}))}))

(defn delete-badge [id state init-data]
  (ajax/DELETE
    (path-for (str "/obpv1/badge/" id))
    {:handler
      (fn []
        (init-data state)
        (m/close-modal!))}))


(defn num-days-left [timestamp]
  (int (/ (- timestamp (/ (.now js/Date) 1000)) 86400))
)

(defn delete-badge-modal [id state init-data]
  [:div
   [:div.modal-header
    [:button {:type "button"
              :class "close"
              :data-dismiss "modal"
              :aria-label "OK"}
     [:span {:aria-hidden "true"
             :dangerouslySetInnerHTML {:__html "&times;"}}]]]
   [:div.modal-body
    [:div {:class (str "alert alert-warning")}
     (t :badge/Confirmdelete)]]
   [:div.modal-footer
    [:button {:type "button"
              :class "btn btn-primary"
              :data-dismiss "modal"}
     (t :core/Cancel)]
    [:button {:type "button"
              :class "btn btn-warning"
              :on-click #(delete-badge id state init-data)}
     (t :core/Delete)]]])

(defn badge-grid-element [element-data state]
  (let [{:keys [id image_file name description visibility expires_on revoked issuer_content_name issuer_content_url]} element-data
        expired? (bh/badge-expired? expires_on)
        badge-link (path-for (str "/badge/info/" id))]
     [:div {:class "media grid-container"}
      [:div {:class (str "media-content " (if expired? "media-expired") (if revoked " media-revoked"))}
      (cond
        expired? [:div.icons
                  [:div.lefticon [:i {:class "fa fa-history"}] (t :badge/Expired)]
                  [:a.righticon {:class "righticon expired" :on-click (fn [] (m/modal! [delete-badge-modal id state init-data]
                                    {:size :lg})) :title (t :badge/Delete)} [:i {:class "fa fa-trash"}]]]
        revoked  [:div.icons
                  [:div.lefticon [:i {:class "fa fa-ban"}] (t :badge/Revoked)]
                  [:a.righticon {:class "righticon revoked" :on-click (fn [] (m/modal! [delete-badge-modal id state init-data]
                                    {:size :lg})) :title (t :badge/Delete)} [:i {:class "fa fa-trash"}]]]
        :else [:div.icons
                [:a.visibility-icon {:on-click #(do (.preventDefault %) (show-settings-dialog id state))
                :title
                (case visibility
                  "private" (str (t :badge/Badgevisibility) ": " (t :badge/Private))
                  "internal" (str (t :badge/Badgevisibility) ": " (t :badge/Shared))
                  "public" (str (t :badge/Badgevisibility) ": " (t :core/Public))
                  nil)}
             (case visibility
               "private" [:i {:class "fa fa-lock"}]
               "internal" [:i {:class "fa fa-group"}]
               "public" [:i {:class "fa fa-globe"}]
               nil)]
               (if expires_on
                [:div.righticon
                  [:i {:title (str (t :badge/Expiresin) " " (num-days-left expires_on) " " (t :badge/days))
                       :class "fa fa-hourglass-half"}]])])
       (if image_file
         [:div.media-left
          [:a {:href badge-link} [:img.badge-img {:src (str "/" image_file)
                                                  :alt name}]]])
       [:div.media-body
        [:div.media-heading
         [:a.heading-link {:href badge-link :id (str id "-heading")} name]]
        [:div.media-issuer
         [:p issuer_content_name]]
         ]]
      ]))

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
    (into [:div {:class "row wrap-grid"
                 :id    "grid"}]
          (for [element-data badges]
            (if (badge-visible? element-data state)
              (badge-grid-element element-data state))))))


(defn no-badges-text []
  [:div
   (if (application-plugin?)  [:div (t :badge/Youhavenobadgesyet) (str ". ") (t :social/Getyourfirstbadge) [:a {:href (path-for "/gallery/application") } (str " ") (t :badge/Gohere)] (str ".")] [:div (t :badge/Youhavenobadgesyet) (str ".")]) ] )


(defn content [state]
  [:div {:id "my-badges"}
   [m/modal-window]
   (if (:initializing @state)
     [:div.ajax-message
      [:i {:class "fa fa-cog fa-spin fa-2x "}]
      [:span (str (t :core/Loading) "...")]]
     [:div
      [badge-grid-form state]
      (cond
        (not-activated?) (not-activated-banner)
        (empty? (:badges @state)) [no-badges-text]
        :else [badge-grid state])

      ]
     )])



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
