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
            [salava.core.ui.helper :refer [path-for unique-values current-path navigate-to]]
            [reagent.session :as session]
            [salava.core.helper :refer [dump]]
            [clojure.string :as s]
            [komponentit.autocomplete :refer [multiple-autocomplete]]
            [schema.core :as sc]
            [cemerick.url :as url]
            [salava.extra.application.schemas :as schemas]
            [salava.core.i18n :as i18n :refer [t]]))



;;Modal

(defn ajax-stop [ajax-message-atom]
  (reset! ajax-message-atom nil))

(defn hashtag? [text]
  (re-find #"^#" text))

(defn subs-hashtag [text]
  (trim text)
  (if (hashtag? text)
    (subs text 1)
    text))

(defn fetch-badges [state]
  (let [{:keys [user-id country-selected name recipient-name issuer-name order tags show-followed-only]} @state
        ajax-message-atom (cursor state [:ajax-message])]
    (reset! ajax-message-atom (t :gallery/Searchingbadges))
    (ajax/GET
     (path-for (str "/obpv1/application/"))
     {:params  {:country  (trim country-selected)
                :name     (subs-hashtag name)
                :tags     (map #(subs-hashtag %) tags)
                :issuer   (trim issuer-name)
                :order    (trim order)
                :followed show-followed-only}
      :handler (fn [data]
                 (swap! state assoc :applications (:applications data)))
      :finally (fn []
                 (do
                                        ;(navigate-to (str "/gallery/application?country=" (trim country-selected) "&name-tag=" (subs-hashtag name-tag) "&issuer=" (trim issuer-name)  "&order=" (trim order)) )
                   (ajax-stop ajax-message-atom)))})))

(defn add-to-followed
  "set advert to connections"
  [badge-advert-id data-atom state]
  (ajax/POST
    (path-for (str "/obpv1/application/create_connection_badge_advert/" badge-advert-id))
    {:handler (fn [data]
                (if (= "success")
                  (do ;set current data-atom to followed true
                    (swap! data-atom assoc :followed 1)
                    (fetch-badges state))
                  ))}))

(defn remove-from-followed
  "remove advert from connections"
  ([badge-advert-id state]
   (remove-from-followed badge-advert-id nil state))
  ([badge-advert-id data-atom state]
   (ajax/DELETE
    (path-for (str "/obpv1/application/delete_connection_badge_advert/" badge-advert-id))
    {:handler (fn [data]
                (if (= "success")
                  (do ;set current data-atom to followed false
                    (if data-atom
                      (swap! data-atom assoc :followed 0))
                    (fetch-badges state))))})))


(defn taghandler [state value]
  (let [tags (cursor state [:tags])
        items (cursor state [:items])]
    (reset! tags (vals (select-keys @items value)))
    (fetch-badges state)))

(defn get-items-key [items tag]
  (key (first (filter #(= (str "#" tag ) (val %)) items))))

(defn set-to-autocomplete [state tag]
  (let [key (get-items-key (:items @state) tag)]
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
                        :items (into (sorted-map) (map-indexed (fn [i v] [(inc i) (str "#" (:tag v))]) tags)))))}))



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
          (bh/issuer-label-image-link issuer_content_name issuer_content_url issuer_contact issuer_image)
          [:div {:class "description"}
           description]
          (if-not (blank? criteria_url)
            [:div {:class "badge-info"}
             [:h2.uppercase-header (t :badge/Criteria)]
             [:div
              [:a {:href   criteria_url
                   :target "_blank"} (t :badge/Opencriteriapage)]]])
          [:div {:class " badge-info"}
           [:h2.uppercase-header "How get this badge"]
           [:div {:dangerouslySetInnerHTML {:__html info}}]]
           
          [:div
           (if (not (empty? tags))
             (into [:div]
                   (for [tag tags]
                     [:a {:href         "#"
                          :id           "tag"
                          :on-click     #(do
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
        [:div {:class "badge-content"}
         [:div {:class "badge-contents col-xs-12"}
          [:div.col-md-3 [:div]]
          [:div {:class "col-md-9 badge-info"}
           [:div.pull-left
            [:a  {:href (:application_url data) :target "_"} [:i.apply-now-icon {:class "fa fa-angle-double-right"}] " Get this badge"]
            ;[:a  " >> Apply now"]
            ]
           
           (if (pos? (:followed @data-atom))
             [:div.pull-right [:a {:href "#" :on-click #(remove-from-followed (:id @data-atom) data-atom state)} [:i {:class "fa fa-bookmark"}] " Remove from  wishlist" ]
              ]
             [:div.pull-right [:a {:href "#" :on-click #(add-to-followed (:id @data-atom) data-atom state)} [:i {:class "fa fa-bookmark-o"}] " Add to wishlist" ]
              ])
           ]]]]])))



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
                              (autocomplete-search state @country-atom)
                              (fetch-badges state))}
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
        items (cursor state [:items])]
    (fn []
      [:div.form-group
       [:label {:class "control-label col-sm-2" :for "autocomplete"} (str "keywords" ":")]
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
          :items           @items
          :placeholder     "search by badge name or keywords"
          :no-results-text "ei löytyny"
          :control-class   "form-control"}]
        ]])
    )
  )

(defn gallery-grid-form [state]
  (let [show-advanced-search (cursor state [:advanced-search])
        show-followed-only-atom (cursor state [:show-followed-only])]
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
        (if  @show-advanced-search
          [:div
           [autocomplete state]
           [text-field :name "Badge name" "search by badge name" state]
           [text-field :issuer-name (t :gallery/Issuer) (t :gallery/Searchbyissuer) state]])
        
        ])
     [:div {:class "form-group wishlist-buttons"}
          [:label {:for   "input-email-notifications"
                   :class "col-md-2"}
           "Näytä suosikit"]
         [:div.col-md-10
          [:div.buttons
           [:button {:class (str "btn btn-default btn "(if-not @show-followed-only-atom "btn-active") )
                     :id "btn-all"
                     :on-click #(do
                                  (reset! show-followed-only-atom (if @show-followed-only-atom false true))
                                  (fetch-badges state))}
            (t :core/All)]
           [:button {:class (str "btn btn-default " (if @show-followed-only-atom "btn-active"))
                     :id "btn-all"
                     :on-click #(do
                                  (reset! show-followed-only-atom (if @show-followed-only-atom false true))
                                  (fetch-badges state))}
             [:i {:class "fa fa-bookmark"}] " wishlist"]]]]
     [g/grid-radio-buttons (str (t :core/Order) ":") "order" (order-radio-values) :order state search-timer]]))

(defn badge-grid-element [element-data state]
  (let [{:keys [id image_file name description issuer_content_name issuer_content_url recipients badge_content_id followed]} element-data
        badge-id (or badge_content_id id)]
    [:div {:class "media grid-container"}
     (if (pos? followed)
       [:a.following-icon {:href "#" :on-click #(remove-from-followed id state) :title "Remove from following"} [:i {:class "fa fa-bookmark"}]]
       )
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
        [:a {:href issuer_content_url
             :target "_blank"
             :title issuer_content_name} issuer_content_name]]
       [:div.media-button
        [:a {:class "btn btn-advert" :on-click #(do (.preventDefault %)(open-modal id state))
                  } [:i.apply-now-icon {:class "fa fa-angle-double-right"}] " Get this badge"]]
       
        [:div.media-description description]]]
     [:div.media-bottom
      
       ;(admin-gallery-badge badge-id "badges" state init-data)
       ]]))

(defn gallery-grid [state]
  (let [badges (:applications @state)
        tags (:tags @state)]
    [:div 
     [:h3 (apply str tags)]
     (into [:div {:class "row"
                  :id    "grid"}]
           (for [element-data badges]
             (badge-grid-element element-data state)))]))







(defn content [state]
  (create-class {:reagent-render (fn []
                                   [:div {:id "badge-advert"}
                                    [m/modal-window]
                                    [gallery-grid-form state]
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
                        ;:items (simple-items 5)
                        )))}))


(defn init-values []
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
                                :tags               ()
                                :value              #{}
                                :user-id            user-id
                                :badges             []
                                :countries          []
                                :country-selected   (or (:country init-values) (session/get-in [:user :country] "all"))
                                :advanced-search    false
                                :name               (or (:name init-values) "")
                                :issuer-name        (or (:issuer-name init-values) "")
                                :order              (or (:order init-values) "mtime")
                                :timer              nil
                                :items              #{}
                                :ajax-message       nil})]
    (init-data state init-values)
    (autocomplete-search state (:country init-values))
    (fn []
      (if (session/get :user)
        (layout/default site-navi [content state badge_content_id])
        (layout/landing-page site-navi [content state badge_content_id]))

      )))
