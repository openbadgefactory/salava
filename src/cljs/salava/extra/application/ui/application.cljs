(ns salava.extra.application.ui.application
  (:require [reagent.core :refer [atom cursor create-class]]
            [reagent.session :as session]
            [reagent-modals.modals :refer [close-modal!]]
            [reagent-modals.modals :as m]
            [clojure.string :refer [trim blank?]]
            [clojure.walk :refer [keywordize-keys]]
            [salava.core.ui.grid :as g]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.badge.ui.helper :as bh]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.helper :refer [path-for unique-values current-path navigate-to not-activated?]]
            [reagent.session :as session]
            [salava.core.helper :refer [dump]]
            [clojure.string :as s]
            [komponentit.autocomplete :refer [multiple-autocomplete]]
            [schema.core :as sc]
            [cemerick.url :as url]
            [salava.extra.application.schemas :as schemas]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.core.ui.modal :as mo]
            [medley.core :refer [distinct-by]]
            [salava.extra.application.ui.issuer :as i]))


(defn hashtag? [text]
  (re-find #"^#" text))

(defn subs-hashtag [text]
  (trim text)
  (if (hashtag? text)
    (subs text 1)
    text))

(defn fetch-badge-adverts [state]
  (let [{:keys [user-id country-selected name issuer-name order tags show-followed-only]} @state]
    (if (or (not= order "mtime") (not (empty? tags)) show-followed-only (not (blank? name)))
      (swap! state assoc :show-featured false) (swap! state assoc :show-featured true))
    (ajax/GET
      (path-for (str "/obpv1/application/"))
      {:params  {:country  (trim country-selected)
                 :name     (subs-hashtag name)
                 :tags     (map #(subs-hashtag %) tags)
                 :issuer   (trim issuer-name)
                 :order    (trim order)
                 :followed show-followed-only}
       :handler (fn [data]
                  (swap! state assoc :applications (:applications data)))})))

(defn add-to-followed
  "set advert to connections"
  [badge-advert-id data-atom state]
  (ajax/POST
    (path-for (str "/obpv1/application/create_connection_badge_application/" badge-advert-id))
    {:handler (fn [data]
                (if (= "success")
                  (do ;set current data-atom to followed true
                    (swap! data-atom assoc :followed 1)
                    (fetch-badge-adverts state))))}))

(defn remove-from-followed
  "remove advert from connections"
  ([badge-advert-id state]
   (remove-from-followed badge-advert-id nil state))
  ([badge-advert-id data-atom state]
   (ajax/DELETE
     (path-for (str "/obpv1/application/delete_connection_badge_application/" badge-advert-id))
     {:handler (fn [data]
                 (if (= "success")
                   (do ;set current data-atom to followed false
                     (if data-atom
                       (swap! data-atom assoc :followed 0))
                     (fetch-badge-adverts state))))})))


(defn taghandler
  "set tag with autocomplete value and accomplish searchs"
  [state value]
  (let [tags (cursor state [:tags])
        autocomplete-items (cursor state [:autocomplete-items])]
    (reset! tags (vals (select-keys @autocomplete-items value)))
    (fetch-badge-adverts state)))

(defn get-items-key
  ""[autocomplete-items tag]
  (key (first (filter #(= (str "#" tag ) (val %)) autocomplete-items))))

(defn set-to-autocomplete [state tag]
  (let [key (get-items-key (:autocomplete-items @state) tag)]
    (if key
      (do
        (swap! state assoc :value #{key})
        (taghandler state #{key})
        ))))

(defn autocomplete-search [state country]
  (swap! state assoc :value #{}
         :tags ())
  (ajax/GET
    (path-for "/obpv1/application/autocomplete")
    {:params {:country  (trim country)}
     :handler (fn [data]
                (let [{:keys [tags names]} data]
                  (swap! state assoc
                         :tags  #{} ;todo: katso jos on toisessakin olemassa
                         :autocomplete-items (into (sorted-map) (map-indexed (fn [i v] [(inc i) (str "#" (:tag v))]) tags)))))}))

(defn open-modal [id state]
  (ajax/GET
    (path-for (str "/obpv1/application/public_badge_advert_content/" id))
    {:handler (fn [data]
                (do
                  (mo/open-modal [:application :badge] {:id (:init-id @state) :state state :data data})))}))


(defn search-timer [state]
  (let [timer-atom (cursor state [:timer])]
    (if @timer-atom
      (js/clearTimeout @timer-atom))
    (reset! timer-atom (js/setTimeout (fn []
                                        (fetch-badge-adverts state)) 500))))

(defn text-field [key label placeholder state]
  (let [search-atom (cursor state [key])
        field-id (str key "-field")]
    [:div.form-group
     [:label {:class "control-label col-sm-2" :for field-id} (str label ":")]
     [:div.col-sm-10
      [:input {:class       (str "form-control")
               :id          field-id
               :type        "text"
               :placeholder placeholder
               :value       @search-atom
               :on-change   #(do
                               (reset! search-atom (.-target.value %))
                               (search-timer state))}]]]))

(defn country-selector [state]
  (let [country-atom (cursor state [:country-selected])]
    [:div.form-group
     [:label {:class "control-label col-sm-2" :for "country-selector"} (str (t :gallery/Country) ":")]
     [:div.col-sm-10
      [:select {:class     "form-control"
                :id        "country-selector"
                :name      "country"
                :value     @country-atom
                :on-change #(do
                              (reset! country-atom (.-target.value %))
                              (autocomplete-search state @country-atom)
                              (swap! state assoc :grid-badges true)
                              (fetch-badge-adverts state))}
       [:option {:value "all" :key "all"} (t :core/All)]
       (for [[country-key country-name] (map identity (:countries @state))]
         [:option {:value country-key
                   :key country-key} country-name])]]]))

(defn order-radio-values []
  [{:value "mtime" :id "radio-date" :label (t :core/bydate)}
   {:value "name" :id "radio-name" :label (t :core/byname)}
   {:value "issuer_content_name" :id "radio-issuer-name" :label (t :core/byissuername)}])

(defn simple-items [n]
  (into (sorted-map)
        (map (fn [i]
               [i (str  i " Option "  (rand-int 20))])
             (range n))))


(defn autocomplete [state]
  (let [tags  (cursor state [:tags])
        value (cursor state [:value])
        autocomplete-items (cursor state [:autocomplete-items])]
    (fn []
      [:div.form-group
       [:label {:class "control-label col-sm-2" :for "autocomplete"} (str (t :extra-application/Keywords) ":")]
       [:div.col-sm-10
        [multiple-autocomplete
         {:value     @value
          :cb        (fn [item] (do
                                  (swap! value conj (:key item))
                                  (taghandler state @value)))
          :remove-cb (fn [x] (do
                               (swap! value disj x)
                               (taghandler state @value)))
          :search-fields   [:value]
          :items           @autocomplete-items
          :no-results-text (t :extra-application/Notfound)
          :control-class   "form-control"}]]])))


(defn follow-grid-item [show-followed-only-atom state]
  [:div {:class "form-group wishlist-buttons"}
   [:label {:for   "input-email-notifications"
            :class "col-md-2"}
    (str (t :core/Show) ":")]
   [:div.col-md-10
    [:div.buttons
     [:button {:class (str "btn btn-default btn "(if-not @show-followed-only-atom "btn-active") )
               :id "btn-all"
               :on-click #(do
                            (reset! show-followed-only-atom (if @show-followed-only-atom false true))
                            (fetch-badge-adverts state))}
      (t :core/All)]
     [:button {:class (str "btn btn-default " (if @show-followed-only-atom "btn-active"))
               :id "btn-all"
               :on-click #(do
                            (reset! show-followed-only-atom (if @show-followed-only-atom false true))
                            (fetch-badge-adverts state))}
      [:i {:class "fa fa-bookmark"}] (str " " (t :extra-application/Favourites))]]]])

(defn gallery-grid-form [state]
  (let [show-advanced-search (cursor state [:advanced-search])
        show-followed-only-atom (cursor state [:show-followed-only])]
    [:div {:id "grid-filter"
           :class "form-horizontal"}
     (if (not (:user-id @state))
       [:div
        [country-selector state]
        [:div
         [autocomplete state]
         [text-field :name  (t :gallery/Badgename) (t :gallery/Searchbybadgename) state]]])

     [follow-grid-item show-followed-only-atom state]
     [g/grid-radio-buttons (str (t :core/Order) ":") "order" (order-radio-values) :order state fetch-badge-adverts]
     [i/issuer-info-grid state]]))

(defn badge-grid-element [element-data state]
  (let [{:keys [id image_file name  issuer_content_name issuer_content_url recipients badge_content_id followed issuer_tier]} element-data
        badge-id (or badge_content_id id)]
    [:div {:class "media grid-container"}
     (if (pos? followed)
       [:a.following-icon {:href "#" :on-click #(remove-from-followed id state) :title (t :extra-application/Removefromfavourites)} [:i {:class "fa fa-bookmark"}]])
     #_(if (= "pro" issuer_tier)
       [:span.featured {:title (t :extra-application/Featuredbadge)} [:i.fa.fa-star]])

      [:div.media-content
       [:a {:href "#" :on-click #(do (.preventDefault %) (mo/open-modal [:application :badge] {:id id  :state state})) }
       (if image_file
         [:div.media-left
          [:img {:src (str "/" image_file)
                 :alt name}]])
       [:div.media-body
        [:div {:class "media-heading"}
         [:span name]]
        [:div.media-issuer
         [:p issuer_content_name]]
        [:div.media-getthis
         [:span [:i.apply-now-icon {:class "fa fa-angle-double-right"}] (str " " (t :extra-application/Getthisbadge))]]]]
      [:div.media-bottom]]]))

(defn str-cat [a-seq]
  (if (empty? a-seq)
    ""
    (let [str-space (fn [str1 str2]
                      (str str1 " " str2))]
      (reduce str-space a-seq))))


(defn shuffle-featured-badges [n state]
  (let [badges (filter #(= (:issuer_tier %) "pro") (:applications @state))]
    (take n (shuffle badges))))

(defn gallery-grid [state]
  (let [badges (:applications @state)
        tags (:tags @state)
        show-issuer-info-atom (cursor state [:show-issuer-info])
        show-featured (cursor state [:show-featured])
        featured-badges (shuffle-featured-badges 4 state)
        featured (if (= 1 (count badges)) badges (if (< (count featured-badges) 4) (into featured-badges (take (- 4 (count featured-badges)) (shuffle badges))) featured-badges))
        grid-badges (if (and @show-featured (not @show-issuer-info-atom)) (remove (fn [app] (some #(identical? % app) featured)) badges) badges)]
    [:div
     (when (and (not (empty? badges)) (not @show-issuer-info-atom) @show-featured)
       [:div.panel {:class "row wrap-grid"
                          :id    "grid"
                          }
              [:button.close {:aria-label "OK"
                              :on-click #(do
                                           (.preventDefault %)
                                           (swap! state assoc :show-featured false))}
               [:span {:aria-hidden "true"
                       :dangerouslySetInnerHTML {:__html "&times;"}}]]
              [:h3.panel-heading (t :extra-application/Featured)]
              [:hr]
       (into [:div.adcontainer]
             (for [element-data featured]
               (badge-grid-element element-data state)))])


     [:h3 (str-cat tags)]
     (into [:div {:class "row wrap-grid"
                  :id    "grid"}]
           (for [element-data grid-badges]
             (badge-grid-element element-data state)))]))



(defn content [state]
  (create-class {:reagent-render (fn []
                                   [:div {:id "badge-advert"}
                                    [m/modal-window]
                                    [gallery-grid-form state]
                                    [gallery-grid state]])
                 :component-did-mount (fn []
                                        (if (:init-id @state) (open-modal (:init-id @state) state)))}))

(defn init-all-issuers [state init-params]
  (ajax/GET
    (path-for "/obpv1/application/")
    {:params  (assoc init-params :country "all")
     :handler (fn [data]
                (swap! state assoc :all-applications (:applications data)))}))

(defn init-data [state init-params]
  (ajax/GET
    (path-for "/obpv1/application/")
    {:params  init-params
     :handler (fn [data]
                (let [{:keys [applications countries user-country]} data]
                  (swap! state assoc :applications applications
                         :countries countries)
                  (init-all-issuers state init-params)))}))


(defn init-values
  "take url params"[]
  (let [{:keys [country issuer-name order id name]} (keywordize-keys (:query (url/url (-> js/window .-location .-href))))
        query-params (keywordize-keys (:query (url/url (-> js/window .-location .-href))))]
    (-> query-params
        (assoc :country (if id "all" (or country (session/get-in [:user :country] "all")))))))


(defn handler [site-navi params]
  (let [user-id          (:user-id params)
        init-values      (init-values)
        badge_content_id (:badge_content_id params)
        state            (atom {:init-id            (:id init-values)
                                :show-followed-only false
                                :show-issuer-info false
                                :tags               ()
                                :value              #{}
                                :user-id            user-id
                                :badges             []
                                :countries          []
                                :country-selected   (:country init-values) #_(or (:country init-values) (session/get-in [:user :country] "all"))
                                :advanced-search    false
                                :name               (or (:name init-values) "")
                                :issuer-name        (or (:issuer-name init-values) "")
                                :order              (or (:order init-values) "mtime")
                                :timer              nil
                                :autocomplete-items              #{}
                                :ajax-message       nil
                                :issuer-content {:name (t :core/All)}
                                :issuer-search false
                                :search-result []
                                :show-featured true})]
    (init-data state init-values)
    (autocomplete-search state (:country init-values))
    (fn []
      (if (session/get :user)
        (layout/default site-navi [content state badge_content_id])
        (layout/landing-page site-navi [content state badge_content_id])))))
