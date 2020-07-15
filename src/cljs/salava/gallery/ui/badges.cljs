(ns salava.gallery.ui.badges
  (:require [reagent.core :refer [atom cursor create-class]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [clojure.string :refer [trim]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.grid :as g]
            [salava.core.ui.modal :as mo]
            [salava.core.ui.helper :refer [path-for plugin-fun]]
            [salava.core.i18n :refer [t]]
            [clojure.walk :refer [keywordize-keys]]
            [salava.core.helper :refer [dump]]
            [cemerick.url :as url]
            [clojure.string :as s]
            [komponentit.autocomplete :refer [multiple-autocomplete]]
            [salava.admin.ui.admintool :refer [admin-gallery-badge]]
            [salava.core.ui.badge-grid :refer [badge-grid-element]]
            [dommy.core :as dommy :refer-macros [sel1 sel]]))

(defn query-params [base]
  {:country (get base :country "")
   :tags (get base :tags "")
   :badge-name (get base :badge-name "")
   :issuer-name (get base :issuer-name "")
   :order (get base :order "mtime")
   :recipient-name (get base :recipient-name "")
   :page_count 0
   :only-selfie? false
   :space-id (get base :space-id (session/get-in [:user :current-space :id] 0))})

(defn ajax-stop [ajax-message-atom]
  (reset! ajax-message-atom nil))

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
        items          (into (sorted-map) (map-indexed (fn [i v] [(inc i) (str "#" (:tag v) " (" (:badge_id_count v) ")")]) tag-items))
        new-value      (set (keys (filter (comp (set tags) last) items)))]
    (swap! state assoc
           :full-tags (into (sorted-map) (map-indexed (fn [i v] [(inc i) (:badge_ids v)]) tag-items))
           :value new-value
           :autocomplete-items items)))

(defn fetch-badges [state]
  (let [{:keys [user-id country-selected badge-name recipient-name issuer-name tags order full-tags tags-badge-ids value]} @state
        ajax-message-atom (cursor state [:ajax-message])
        page-count-atom (cursor state [:params :page_count])
        selfie-badges-atom (cursor state [:only-selfie?])]
    (reset! ajax-message-atom (t :gallery/Searchingbadges))
    (reset! page-count-atom 0)
    (ajax/GET
     (path-for (str "/obpv1/gallery/badges"))
     {:params  (:params @state)
      #_{:country        (trim country-selected)
         :badge-name     (trim badge-name)
         :tags           (map #(subs-hashtag %) tags)
         :tags-ids       tags-badge-ids
         :recipient-name (trim recipient-name)
         :order          (trim order)
         :issuer-name    (trim issuer-name)
         :page_count     @page-count-atom}
      :handler (fn [data]
                 (value-helper state (get-in data [:tags]))
                 (swap! page-count-atom inc)
                 (swap! state assoc
                        :badges (:badges data)
                        :badge_count (:badge_count data)))
      :finally (fn []
                 (ajax-stop ajax-message-atom))})))

(defn get-more-badges [state]
  (let [{:keys [user-id country-selected badge-name recipient-name issuer-name
                tags order full-tags tags-badge-ids value]} @state
        ajax-message-atom (cursor state [:ajax-message])
        page-count-atom (cursor state [:params :page_count])]
    (ajax/GET
     (path-for (str "/obpv1/gallery/badges"))
     {:params  (:params @state)
      #_{:country        (trim country-selected)
         :badge-name     (trim badge-name)
         :tags           (map #(subs-hashtag %) tags)
         :tags-ids       tags-badge-ids
         :recipient-name (trim recipient-name)
         :order          (trim order)
         :issuer-name    (trim issuer-name)
         :page_count     page_count}
      :handler (fn [data]
                 (value-helper state (get-in data [:tags]))
                 (swap! page-count-atom inc)
                 (swap! state assoc
                        :badges (into (:badges @state) (:badges data))
                        :badge_count (:badge_count data)))
      :finally (fn [])})))

(defn taghandler
  "set tag with autocomplete value and accomplish searchs"
  [state value]
  (let [tags (cursor state [:params :tags])]
    (reset! tags (apply str (interpose "," value)))
    (fetch-badges state)))

#_(defn autocomplete [state]
    (let [value  (cursor state [:autocomplete :tags :value])
          autocomplete-items (cursor state [:autocomplete :tags :items])]

      (fn []
        [:div.form-group
         [:label {:class "control-label col-sm-2" :for "autocomplete"} (str (t :gallery/Keywords) ":")]
         [:div.col-sm-10
          [multiple-autocomplete
           {:value     @value
            :on-change (fn [item]
                         (swap! value conj (:key item))
                         (taghandler state @value))
            :on-remove (fn [x]
                         (swap! value disj x)
                         (taghandler state @value))
            :search-fields   [:value]
            :items           @autocomplete-items
            :no-results-text (t :gallery/Notfound)
            :control-class   "form-control"}]]])))

(defn autocomplete-accessibility-fix []
  (-> (sel1 ".autocomplete__input")
      (dommy/set-attr! :id "autocomplete" :tabIndex 0)))

(defn autocomplete [state]
  (let [value  (cursor state [:autocomplete :tags :value])
        autocomplete-items (cursor state [:autocomplete :tags :items])]
    (create-class
     {:reagent-render
      (fn []
        [:div.form-group
         [:label {:class "control-label col-sm-2" :for "autocomplete"} (str (t :gallery/Keywords) ":")]
         [:div.col-sm-10
          [multiple-autocomplete
           {:value     @value
            :on-change (fn [item]
                         (swap! value conj (:key item))
                         (taghandler state @value))
            :on-remove (fn [x]
                         (swap! value disj x)
                         (taghandler state @value))
            :search-fields   [:value]
            :items           @autocomplete-items
            :no-results-text (t :gallery/Notfound)
            :control-class   "form-control"}]]])
      :component-did-mount
      (fn [] (autocomplete-accessibility-fix))})))

(defn search-timer [state]
  (let [timer-atom (cursor state [:timer])]
    (if @timer-atom
      (js/clearTimeout @timer-atom))
    (reset! timer-atom (js/setTimeout (fn []
                                        (fetch-badges state)) 500))))

(defn text-field [key label placeholder state]
  (let [search-atom (cursor state [:params key])
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
  (let [country (cursor state [:params :country])]
    [:div.form-group
     [:label {:class "control-label col-sm-2" :for "country-selector"} (str (t :gallery/Country) ":")]
     [:div.col-sm-10
      [:select {:class     "form-control"
                :id        "country-selector"
                :name      "country"
                :value     @country
                :on-change #(do
                              (reset! country (.-target.value %))
                              (swap! state assoc :params (query-params {:country @country}))
                              (fetch-badges state))}
       [:option {:value "all" :key "all"} (t :core/All)]
       (for [[country-key country-name] (map identity (:countries @state))]
         [:option {:value country-key
                   :key country-key} country-name])]]]))

(defn order-radio-values []
  [{:value "mtime" :id "radio-date" :label (t :core/bydate)}
   {:value "name" :id "radio-name" :label (t :core/byname)}
   {:value "issuer_content_name" :id "radio-issuer-name" :label (t :core/byissuername)}
   {:value "recipients" :id "radio-recipients" :label (t :core/byrecipients)}])

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
                    [:div.autocomplete-item {:on-click #(do (reset! text ""))}
                                                          ;(pick-fn {:key item-key :value item-value})

                     item-value]))))]))))

(defn gallery-grid-form [state]
  (let [show-advanced-search (cursor state [:advanced-search])]
    [:div {:id "grid-filter"
           :class "form-horizontal"}
     (if (not (:user-id @state))
       [:div

        (if (empty? (session/get-in [:user :current-space] nil))
         [country-selector state]
         (into [:div]
          (for [f (plugin-fun (session/get :plugins) "block" "gallery_badge_space_selector")]
           (when (ifn? f) [f state (fn [] (fetch-badges state))]))))
        [:div
         [:a {:on-click #(reset! show-advanced-search (not @show-advanced-search))
              :href "#"}
          (if @show-advanced-search
            (t :gallery/Hideadvancedsearch)
            (t :gallery/Showadvancedsearch))]]
        (when @show-advanced-search
          [:div
           [autocomplete state]
           [text-field :badge-name (t :gallery/Badgename) (t :gallery/Searchbybadgename) state]
           [text-field :recipient-name (t :gallery/Recipient) (t :gallery/Searchbyrecipient) state]
           [text-field :issuer-name (t :gallery/Issuer) (t :gallery/Searchbyissuer) state]])])
     [g/grid-radio-buttons (str (t :core/Order) ":") "order" (order-radio-values) [:params :order] state fetch-badges]
     (into [:div]
      (for [f (plugin-fun (session/get :plugins) "block" "gallery_checkbox")]
       (when (ifn? f) [f state (fn [] (fetch-badges state))])))]))

(defn load-more [state]
  (if (pos? (:badge_count @state))
    [:div {:class "media message-item"}
     [:div {:class "media-body"}
      [:span [:a {:href     "#"
                  :id    "loadmore"
                  :on-click #(do
                               (get-more-badges state)
                               ;(init-data state)
                               (.preventDefault %))}

              (str (t :social/Loadmore) " (" (:badge_count @state) " " (t :gallery/Badgesleft) ")")]]]]))

(defn gallery-grid [state]
  (let [badges (:badges @state)]
    [:div#badges (into [:div {:class "row wrap-grid"
                              :id    "grid"}]
                       (for [element-data badges]
                         (badge-grid-element element-data state "gallery" fetch-badges)))
     (load-more state)]))

(defn content [state]
  (fn []
    [:div {:id "badge-gallery"}
     [m/modal-window]
     [gallery-grid-form state]
     (if (:ajax-message @state)
       [:div.ajax-message
        [:i {:class "fa fa-cog fa-spin fa-2x "}]
        [:span (:ajax-message @state)]]
       [gallery-grid state])]))

#_(defn init-values
    "take url params" []
    (let [{:keys [country issuer-name order id badge-name recipient-name]} (keywordize-keys (:query (url/url (-> js/window .-location .-href))))
          query-params (keywordize-keys (:query (url/url (-> js/window .-location .-href))))
          country (session/get-in [:filter-options :country] country)]
      (-> query-params
          (assoc :country (if id "all" (or country (session/get-in [:user :country]) "all")))
          (assoc :page_count 0))))

(defn init-data [params state]
  (reset! (cursor state [:ajax-message]) (str (t :core/Loading) "..."))

  (ajax/GET
   (path-for "/obpv1/gallery/badge_countries")
   {:handler (fn [data] (swap! state assoc :countries (:countries data)))})

  (ajax/GET
   (path-for "/obpv1/gallery/badge_tags")
   {:handler (fn [data]
               (swap! state
                      assoc-in
                      [:autocomplete :tags :items]
                      (->> data :tags (map (fn [t] [t t])) (into (sorted-map)))))})

  (ajax/GET
   (path-for "/obpv1/gallery/badges")
   {:params  params
    :handler (fn [data]
               (let [{:keys [badges badge_count]} data]
                  ;(value-helper state tags)
                 (if (empty? badges)
                   (init-data (assoc params :country "all") state) ;;Recall init data with "all" countries if initial query returned empty coll
                   (do
                     (reset! (cursor state [:params :page_count]) 1)
                     (swap! state assoc
                            :badges badges
                            :badge_count badge_count)))))
    :finally (fn []
               (ajax-stop (cursor state [:ajax-message])))}))

(defn handler [site-navi params]
  (let [query (as-> (-> js/window .-location .-href url/url :query keywordize-keys) $
                    (if (:country $)
                      $
                      (assoc $ :country (session/get-in [:filter-options :country]
                                                        (session/get-in [:user :country]
                                                                        "all")))))
        params (query-params query)
        state  (atom {:params params
                      :badge_count            0
                      :badges                 []
                      :countries              []
                      :country-selected       (session/get-in [:filter-options :country] "all")
                      :autocomplete           {:tags {:value #{} :items #{}}}
                      :advanced-search        false
                      :timer                  nil
                      :ajax-message           nil
                      :only-selfie? false
                      :space-id (session/get-in [:user :current-space :id] nil)})]
    (init-data params state)
    (fn []
      (if (session/get :user)
        (layout/default site-navi [content state])
        (layout/landing-page site-navi [content state])))))
