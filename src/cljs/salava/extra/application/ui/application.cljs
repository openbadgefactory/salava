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
  (let [{:keys [user-id country-selected name recipient-name issuer-name order tags show-followed-only]} @state]
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



(defn tag-parser [tags]
  (if tags
    (s/split tags #",")))

(defn modal-content [data state]
  (let [{:keys [image_file name info issuer_content_name tags  issuer_content_name issuer_content_url issuer_contact issuer_image description criteria_url]} data
        tags (tag-parser tags)
        country (:country-selected @state)]
    (fn []
      [:div {:id "badge-contents"}
       [:div.row
        [:div {:class "col-md-3 badge-image modal-left"}
         [:img {:src (str "/" image_file)}]]
        [:div {:class "col-md-9 badge-info"}
         [:div.rowcontent
          [:h1.uppercase-header name]
          [:div.badge-stats
           (bh/issuer-label-image-link issuer_content_name issuer_content_url "" issuer_contact issuer_image)
           [:div
            description]
           (if-not (blank? criteria_url)
             [:div {:class "badge-info"}
              [:a {:href   criteria_url
                   :target "_blank"} (t :badge/Opencriteriapage)]])]
          [:div {:class " badge-info"}
           [:h2.uppercase-header (t :extra-application/Howtogetthisbadge)]
           [:div {:dangerouslySetInnerHTML {:__html info}}]]
          [:div
           (if (not (empty? tags))
             (into [:div]
                   (for [tag tags]
                     [:a {:href         "#"
                          :id           "tag"
                          :on-click     #(do
                                           (swap! state assoc :advanced-search true)
                                           (set-to-autocomplete state tag))
                          :data-dismiss "modal"}
                      (str "#" tag )])))]]]]])))


(defn badge-content-modal-render [data state]

  (let [data-atom (atom data) ]
    (fn []
      [:div {:id "badge-content"}
       [:div.modal-body
        [:div.row
         [:div.col-md-12
          [:div {:class "text-right"}

           [:button {:type         "button"
                     :class        "close"
                     :data-dismiss "modal"
                     :aria-label   "OK"}
            [:span {:aria-hidden             "true"
                    :dangerouslySetInnerHTML {:__html "&times;"}}]]]]]
        [modal-content data state]]
       [:div.modal-footer
        [:div {:class "badge-advert-footer"}
         [:div {:class "badge-contents col-xs-12"}
          [:div.col-md-3 [:div]]
          [:div {:class "col-md-9 badge-info"}
           [:div
            [:div.pull-left
             [:a  {:href (:application_url data) :target "_"} [:i.apply-now-icon {:class "fa fa-angle-double-right"}] (if (or (= "application" (:kind @data-atom)) (blank? (:application_url_label @data-atom))) (str " " (t :extra-application/Getthisbadge))  (str " " (:application_url_label @data-atom)))]
             ;[:a  " >> Apply now"]
             ]
            (if-not (not-activated?)
              (if (pos? (:followed @data-atom))
                [:div.pull-right [:a {:href "#" :on-click #(remove-from-followed (:id @data-atom) data-atom state)} [:i {:class "fa fa-bookmark"}] (str " " (t :extra-application/Removefromfavourites))]]
                [:div.pull-right [:a {:href "#" :on-click #(add-to-followed (:id @data-atom) data-atom state)} [:i {:class "fa fa-bookmark-o"}] (str " " (t :extra-application/Addtofavourites))]]))]]]]]])))



(defn badge-content-modal [data state]
  (create-class {:reagent-render (fn [] (badge-content-modal-render data state))
                 :component-will-unmount (fn [] (do (close-modal!)
                                                  ;(if (and init-data state) (init-data state))
                                                  ))}))

(defn open-modal [id state]
  (ajax/GET
    (path-for (str "/obpv1/application/public_badge_advert_content/" id))
    {:handler (fn [data]
                (do
                  (m/modal! [badge-content-modal data state] {:size :lg})))}))


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
     (if (= "pro" issuer_tier)
       [:span.featured {:title (t :extra-application/Featuredbadge)} [:i.fa.fa-star]])
     [:div.media-content
      (if image_file
        [:div.media-left
         [:a {:href "#" :on-click #(open-modal id state) :title name}
          [:img {:src (str "/" image_file)
                 :alt name}]]])
      [:div.media-body
       [:div {:class "media-heading"}
        [:a {:on-click #(do (.preventDefault %)(open-modal id state)) :title name}
         name]]
       [:div.media-issuer
        [:p issuer_content_name]]
       [:div.media-getthis
        ;(mo/open-modal [:badge :issuer] id {:hide (fn [] (init-issuer-connection id state))})
        [:a {:class "" :on-click #(do (.preventDefault %) (mo/open-modal [:app :advert] {:id id  :state state}) #_(open-modal id state))}
         [:i.apply-now-icon {:class "fa fa-angle-double-right"}] (str " " (t :extra-application/Getthisbadge))]]]]
     [:div.media-bottom
      ;(admin-gallery-badge badge-id "badges" state init-data)
      ]]))

(defn str-cat [a-seq]
  (if (empty? a-seq)
    ""
    (let [str-space (fn [str1 str2]
                      (str str1 " " str2))]
      (reduce str-space a-seq))))


(defn shuffle-pro-badges [n state]
  (let [badges (filter #(= (:issuer_tier %) "pro") (:applications @state))]
    (take n (shuffle badges))))


(defn pro-badges-grid [state]
  (let [show-issuer-info-atom (cursor state [:show-issuer-info])
        badges (shuffle-pro-badges 4 state)
        badge-count (count badges)]

    (when (and (not (empty? badges)) (not @show-issuer-info-atom))
      (into [:div.panel {:class "row wrap-grid"
                         :id    "grid"
                         :style {:padding "10px"}}
             [:h3 [:i.fa.fa-star ] (str " " (if (> badge-count 1) (t :extra-application/Featuredbadges) (t :extra-application/Featuredbadge) ))]
             [:hr]]
            (for [element-data badges]
              (badge-grid-element element-data state))))))

(defn gallery-grid [state]
  (let [badges (:applications @state)
        tags (:tags @state)]
    [:div
     [:h3 (str-cat tags)]
     (into [:div {:class "row wrap-grid"
                  :id    "grid"}]
           (for [element-data (sort-by #(= (:issuer_tier "pro")) badges)]
             (badge-grid-element element-data state)))]))



(defn content [state]
  (create-class {:reagent-render (fn []
                                   [:div {:id "badge-advert"}
                                    [m/modal-window]
                                    [gallery-grid-form state]
                                    [pro-badges-grid state]
                                    [gallery-grid state]
                                    ;(if (:ajax-message @state) [:div.ajax-message [:i {:class "fa fa-cog fa-spin fa-2x "}] [:span (:ajax-message @state)]][gallery-grid state])
                                    ])
                 :component-did-mount (fn []
                                        (if (:init-id @state) (open-modal (:init-id @state) state))
                                        )}))

(defn init-data [state init-params]
  (ajax/GET
    (path-for "/obpv1/application/")
    {:params  init-params
     :handler (fn [data]
                (let [{:keys [applications countries user-country]} data]
                  (swap! state assoc :applications applications
                         :countries countries
                         :all-applications applications ;; use initial applications list to build issuer list
                         )))}))


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
                                :issuer-content {:name (t :extra-application/Allissuers)}
                                :issuer-search false
                                :search-result []})]
    (init-data state init-values)
    (autocomplete-search state (:country init-values))
    (fn []
      (if (session/get :user)
        (layout/default site-navi [content state badge_content_id])
        (layout/landing-page site-navi [content state badge_content_id]))

      )))
