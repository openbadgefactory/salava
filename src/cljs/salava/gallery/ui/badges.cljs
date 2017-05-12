(ns salava.gallery.ui.badges
  (:require [reagent.core :refer [atom cursor create-class]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [clojure.string :refer [trim]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.grid :as g]
            [salava.core.ui.modal :as mo]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.i18n :refer [t]]
            [clojure.walk :refer [keywordize-keys]]
            [salava.core.helper :refer [dump]]
            [cemerick.url :as url]
            [clojure.string :as s]
            [salava.gallery.ui.badge-content :refer [badge-content-modal]]
            [komponentit.autocomplete :refer [multiple-autocomplete]]
            [salava.admin.ui.admintool :refer [admin-gallery-badge]]))


(defn open-modal ([badge-content-id messages?]
                  (open-modal badge-content-id messages? nil nil))
  ([badge-content-id messages? init-data state]
   (let [reporttool (atom {:description     ""
                           :report-type     "bug"
                           :item-id         ""
                           :item-content-id ""
                           :item-url        ""
                           :item-name       "" ;
                           :item-type       "" ;badge/user/page/badges
                           :reporter-id     ""
                           :status          "false"})]
     (ajax/GET
      (path-for (str "/obpv1/gallery/public_badge_content/" badge-content-id))
      {:handler (fn [data]
                  (do
                    (m/modal! [badge-content-modal data reporttool messages? init-data state] {:size :lg})))}))))

(defn ajax-stop [ajax-message-atom]
  (reset! ajax-message-atom nil))

(defn autocomplete-search [state country]
  (ajax/GET
     (path-for "/obpv1/gallery/badges/autocomplete")     
     {:params  {:country           (trim country)}
      :handler (fn [data]
                 (let [{:keys [tags names]} data]
                   (swap! state assoc
                          :full-tags (into (sorted-map) (map-indexed (fn [i v] [(inc i) (:badge_content_ids v)]) tags))
                          :tags  #{} ;todo: katso jos on toisessakin olemassa
                          :autocomplete-items (into (sorted-map) (map-indexed (fn [i v] [(inc i) (str "#" (:tag v))]) tags)))))})
  )

(defn hashtag? [text]
  (re-find #"^#" text))

(defn subs-hashtag [text]
  (trim text)
  (if (hashtag? text)
    (subs text 1)
    text))

(defn tag-parser [tags]
  (if tags
    (s/split tags #",")))


(defn value-helper [state tag-items]
  (let [value          (cursor state [:value])
        {:keys [tags]} @state
        items          (into (sorted-map) (map-indexed (fn [i v] [(inc i) (str "#" (:tag v) " (" (:badge_content_id_count v) ")")]) tag-items))
        new-value      (set (keys (filter (comp (set tags) last) items)))]
    (swap! state assoc
           :full-tags (into (sorted-map) (map-indexed (fn [i v] [(inc i) (:badge_content_ids v)]) tag-items))
           :value new-value
           :autocomplete-items items)
    ))

(defn fetch-badges [state]
  (let [{:keys [user-id country-selected badge-name recipient-name issuer-name tags order full-tags tags-badge-content-ids value]} @state
        ajax-message-atom                                                                                                                     (cursor state [:ajax-message])
        page-count-atom (cursor state [:page_count])]
    (reset! ajax-message-atom (t :gallery/Searchingbadges))
    (reset! page-count-atom 0)
    (ajax/GET
      (path-for (str "/obpv1/gallery/badges"))
      {:params  {:country        (trim country-selected)
                 :badge-name     (trim badge-name)
                 :tags           (map #(subs-hashtag %) tags)
                 :tags-ids       tags-badge-content-ids
                 :recipient-name (trim recipient-name)
                 :order          (trim order)
                 :issuer-name    (trim issuer-name)
                 :page_count     @page-count-atom}
       :handler (fn [data]                  
                  (value-helper state (get-in data [:tags]))
                  (swap! state assoc :badges (:badges data)
                         :page_count (inc (:page_count @state))
                         :badge_count (:badge_count data)))
       :finally (fn []
                  (ajax-stop ajax-message-atom))})))


(defn get-more-badges [state]
  (let [{:keys [user-id country-selected badge-name recipient-name issuer-name tags order full-tags tags-badge-content-ids value page_count]} @state
        ajax-message-atom                                                                                                                     (cursor state [:ajax-message])]
    ;(reset! ajax-message-atom (t :gallery/Searchingbadges))
    (ajax/GET
      (path-for (str "/obpv1/gallery/badges"))
      {:params  {:country        (trim country-selected)
                 :badge-name     (trim badge-name)
                 :tags           (map #(subs-hashtag %) tags)
                 :tags-ids       tags-badge-content-ids
                 :recipient-name (trim recipient-name)
                 :order          (trim order)
                 :issuer-name    (trim issuer-name)
                 :page_count     page_count}
       :handler (fn [data]                  
                  (value-helper state (get-in data [:tags]))
                  (swap! state assoc
                         :badges (into (:badges @state) (:badges data))
                         :page_count (inc (:page_count @state))
                         :badge_count (:badge_count data)))
       :finally (fn []
                  ;(ajax-stop ajax-message-atom)
                  )})))



(defn taghandler
  "set tag with autocomplete value and accomplish searchs"
  [state value]
  (let [tags (cursor state [:tags])
        tags-badge-content-ids (cursor state [:tags-badge-content-ids])
        autocomplete-items (cursor state [:autocomplete-items])
        autocomplete-badge-content-ids (cursor state [:full-tags])]
    (reset! tags (vals (select-keys @autocomplete-items value)))
    (reset! tags-badge-content-ids (vals (select-keys @autocomplete-badge-content-ids value)))
    (fetch-badges state)
    ))

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
          :cb        (fn [item]  (do
                                    (swap! value conj (:key item))
                                    (taghandler state @value)))
          :remove-cb (fn [x] (do
                               (swap! value disj x)
                               (taghandler state @value)))
          :search-fields   [:value]
          :items           @autocomplete-items
          :no-results-text (t :extra-application/Notfound)
          :control-class   "form-control"
          }]]])))



#_(defn init-data1 [state user-id]
  (ajax/POST
    (path-for (str "/obpv1/gallery/badges/" user-id))
    {:params {:country ""
              :badge ""
              :issuer ""
              :recipient ""}
     :handler (fn [data]
                (let [{:keys [badges countries user-country]} data]
                  (swap! state assoc :badges badges
                                     :countries countries
                                     :country-selected user-country)))}))
(defn init-data [state init-params]
  (ajax/GET
   (path-for "/obpv1/gallery/badges")
   {:params  init-params
    :handler (fn [data]
               (let [{:keys [badges countries user-country tags badge_count]} data]
                 (value-helper state tags)
                 (swap! state assoc
                        :page_count (inc (:page_count @state))
                        :badges badges
                        :badge_count badge_count
                        :countries countries
                        :country-selected user-country)))}))

#_(defn init-data [state user-id]
  (ajax/GET
   (path-for (str "/obpv1/gallery/badges/" user-id))
   
    {:params {:country ""
              :badge ""
              :issuer ""
              :recipient ""}
     :handler (fn [data]
                (let [{:keys [badges countries user-country]} data]
                  (swap! state assoc :badges badges
                                     :countries countries
                                     :country-selected user-country)))}))






(defn search-timer [state]
  (let [timer-atom (cursor state [:timer])]
    (if @timer-atom
      (js/clearTimeout @timer-atom))
    (reset! timer-atom (js/setTimeout (fn []
                                        (fetch-badges state)) 500))))

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
                              (swap! state assoc
                                     :badge-name     ""
                                     :recipient-name ""
                                     :issuer-name    ""
                                     :value #{}
                                     :tags-badge-content-ids '()
                                     :tags '()
                                     :full-tags '())
                              ;(autocomplete-search state @country-atom)
                              (fetch-badges state)
                              
                              )}
       [:option {:value "all" :key "all"} (t :core/All)]
       (for [[country-key country-name] (map identity (:countries @state))]
         [:option {:value country-key
                   :key country-key} country-name])]]]))


(defn order-radio-values []
  [{:value "mtime" :id "radio-date" :label (t :core/bydate)}
   {:value "name" :id "radio-name" :label (t :core/byname)}
   {:value "issuer_content_name" :id "radio-issuer-name" :label (t :core/byissuername)}
   {:value "recipients" :id "radio-recipients" :label "recipients"}])

(defn autocomplete-picker
  ""
  ([items pick-fn] (autocomplete-picker items pick-fn {}))
  ([items pick-fn input-opts]
   (let [text (atom "")]
     (fn [items]
       [:div#autocomplete-picker
        [:input (merge {:class        "form-control"
                        :type         "text"
                        :auto-complete "off"
                        :on-change    #(reset! text (-> % .-target .-value))
                        :value        @text}
                       input-opts)]
        (let [items-matched (filter #(and
                                      (not-empty @text)
                                      (re-find (re-pattern (.toLowerCase (str @text))) (.toLowerCase (str (val %))))) items)]
          (if (not-empty items-matched)
            (into [:div#autocomplete-items]
                  (for [[item-key item-value] items-matched]
                    [:div.autocomplete-item {:on-click #(do (reset! text "")
                                                            ;(pick-fn {:key item-key :value item-value})
                                                            )}
                     item-value]))))]))))



(defn gallery-grid-form [state]
  (let [show-advanced-search (cursor state [:advanced-search])]
    [:div {:id "grid-filter"
           :class "form-horizontal"}
     (if (not (:user-id @state))
       [:div
        
        [country-selector state]
        [:div
         [:a {:on-click #(reset! show-advanced-search (not @show-advanced-search))
              :href "#"}
          (if @show-advanced-search
            (t :gallery/Hideadvancedsearch)
            (t :gallery/Showadvancedsearch))]]
        (if @show-advanced-search
          [:div
           [autocomplete state]
           [text-field :badge-name (t :gallery/Badgename) (t :gallery/Searchbybadgename) state]
           [text-field :recipient-name (t :gallery/Recipient) (t :gallery/Searchbyrecipient) state]
           [text-field :issuer-name (t :gallery/Issuer) (t :gallery/Searchbyissuer) state]])])
     [g/grid-radio-buttons (str (t :core/Order) ":") "order" (order-radio-values) :order state fetch-badges]]))

(defn badge-grid-element [element-data state]
  (let [{:keys [id image_file name description issuer_content_name issuer_content_url recipients badge_content_id]} element-data
        badge-id (or badge_content_id id)]
     [:div {:class "media grid-container"}
      [:div.media-content
       (if image_file
         [:div.media-left
          [:a {:href "#" :on-click #(mo/open-modal [:gallery :badges] {:badge-content-id badge-id})
                :title name}[:img {:src (str "/" image_file)
                 :alt name}]]])
       [:div.media-body
        [:div.media-heading
         [:a.heading-link {:on-click #(do
                                        (.preventDefault %)
                                        (mo/open-modal [:gallery :badges] {:badge-content-id badge-id})
                                        ) :title name}
          name]]
        [:div.media-issuer
         [:a {:href "#" :on-click #(do
                                     (.preventDefault %)
                                     (swap! state  assoc :issuer-name issuer_content_name
                                             :advanced-search        true)
                                     (fetch-badges state)
                                     
                                     )} issuer_content_name]]
        (if recipients
          [:div.media-recipients
           recipients " " (if (= recipients 1)
                            (t :gallery/recipient)
                            (t :gallery/recipients))])
        [:div.media-description description]]]
      [:div.media-bottom
       [:div {:class "pull-left"}
        ;[:a.bottom-link {:href (path-for (str "/gallery/badgeview/" badge-id))} [:i {:class "fa fa-share-alt"}] (t :badge/Share)]
        ]
       (admin-gallery-badge badge-id "badges" state init-data)]]))

(defn load-more [state]
  (if (pos? (:badge_count @state))
    [:div {:class "media message-item"}
     [:div {:class "media-body"}
      [:span [:a {:href     "#" 
                  :id    "loadmore"
                  :on-click #(do
                               (get-more-badges state)
                               ;(init-data state)
                               (.preventDefault %)
                               )}
              (str (t :social/Loadmore) " (" (:badge_count @state) " " (t :gallery/Badgesleft) ")")]]]]))

(defn gallery-grid [state]
  (let [badges (:badges @state)]
    [:div (into [:div {:class "row"
                       :id    "grid"}]
                (for [element-data badges]
                  (badge-grid-element element-data state)))
     (load-more state)]))



(defn content [state badge_content_id]
(create-class {:reagent-render (fn []
                                 [:div {:id "badge-gallery"}
                                  [m/modal-window]
                                  [gallery-grid-form state]
                                  (if (:ajax-message @state)
                                    [:div.ajax-message
                                     [:i {:class "fa fa-cog fa-spin fa-2x "}]
                                     [:span (:ajax-message @state)]]
                                    [gallery-grid state]
                                    )])
                 :component-did-mount (fn []
                                        (if badge_content_id
                                          (open-modal badge_content_id true)
                                          )
                                        )
                 ;:component-did-update #(scroll-bottom)
                 })
  )


(defn init-values
  "take url params"[]
  (let [{:keys [country issuer-name order id badge-name recipient-name]} (keywordize-keys (:query (url/url (-> js/window .-location .-href))))
        query-params (keywordize-keys (:query (url/url (-> js/window .-location .-href))))]
    (-> query-params
        (assoc :country (if id "all" (or country (session/get-in [:user :country]) "all")))
        (assoc :page_count 0))))


(defn handler [site-navi params]
  (let [user-id          (:user-id params)
        init-values      (init-values)
        badge_content_id (:badge_content_id params)
        state            (atom {:badge_count            0
                                :user-id                user-id
                                :page_count             0
                                :badges                 []
                                :countries              []
                                :country-selected       "FI"
                                :tags                   ()
                                :tags-badge-content-ids ()
                                :full-tags              ()
                                :value                  #{}
                                :autocomplete-items     #{}
                                :advanced-search        false
                                :badge-name             ""
                                :recipient-name         ""
                                :issuer-name            ""
                                :order                  "mtime"
                                :timer                  nil
                                :ajax-message           nil})]
    (init-data state init-values)
    ;(autocomplete-search state (:country init-values))
    ;(init-data state user-id)
    (fn []
      (if (session/get :user)
        (layout/default site-navi [content state badge_content_id])
        (layout/landing-page site-navi [content state badge_content_id])))))
