(ns salava.extra.application.ui.application
  (:require [reagent.core :refer [atom cursor create-class]]
            [reagent.session :as session]
            [reagent-modals.modals :refer [close-modal!]]
            [reagent-modals.modals :as m]
            [clojure.string :refer [trim]]
            [clojure.walk :refer [keywordize-keys]]
            [salava.core.ui.grid :as g]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.helper :refer [path-for unique-values current-path navigate-to]]
            [reagent.session :as session]
            [salava.core.helper :refer [dump]]
            [clojure.string :as s]
            [komponentit.autocomplete :refer [multiple-autocomplete]]
            [schema.core :as sc]
            [cemerick.url :as url]
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

(defn autocomplete-search [state country]
  (ajax/GET
   (path-for "/obpv1/application/autocomplete")     
   {:params {:country  (trim country)}
    :handler (fn [data]
               (let [{:keys [tags names]} data]
                 (swap! state assoc
                        :tags  #{} ;todo: katso jos on toisessakin olemassa
                        :items (into (sorted-map) (map-indexed (fn [i v] [(inc i) (str "#" (:tag v))]) tags)))))}))

(defn fetch-badges [state]
  (let [{:keys [user-id country-selected name recipient-name issuer-name order tags]} @state
        ajax-message-atom (cursor state [:ajax-message])]
    (reset! ajax-message-atom (t :gallery/Searchingbadges))
    (ajax/GET
     (path-for (str "/obpv1/application/"))
     {:params  {:country   (trim country-selected)
                :name      (subs-hashtag name)
                :tags      (map #(subs-hashtag %) tags)
                :issuer    (trim issuer-name)
                :order     (trim order)}
      :handler (fn [data]
                 (swap! state assoc :applications (:applications data)))
      :finally (fn []
                 (do
                                        ;(navigate-to (str "/gallery/application?country=" (trim country-selected) "&name-tag=" (subs-hashtag name-tag) "&issuer=" (trim issuer-name)  "&order=" (trim order)) )
                   (ajax-stop ajax-message-atom)))})))

(defn tag-parser [tags]
  (if tags
    (s/split tags #",")))

(defn modal-content [data state] 
  (let [{:keys [image_file name info issuer_content_name tags]} data
        tags (tag-parser tags)
        country (:country-selected @state)]
    (fn []
      [:div {:id "badge-contents"}
       [:div.row
        [:div {:class "col-md-3 badge-image modal-left"}
         [:img {:src (str "/" image_file)}]
         [:h3.heading-link 
          name]
         [:p
          issuer_content_name]
         ]
        [:div {:class "col-md-9 badge-info"}
         [:div.rowcontent
                                        ;[:h1.uppercase-header name]
          [:h2.uppercase-header "How get this badge"]
          [:div.row
             [:div.col-md-12
              {:dangerouslySetInnerHTML {:__html info}}]]
          [:h2.uppercase-header "keywords"]
          [:div.row
           (if (not (empty? tags))
             (into [:div {:class "col-md-12"}]
                   (for [tag tags]
                     ;[:div tag]
                     [:a {:href "#"
                          :id "tag"
                          :on-click #(do
                                       (swap! state assoc :name tag)
                                       (fetch-badges state))
                          :data-dismiss "modal"}
                      (str "#" tag )]
                     )))
           ]

          
          
          ]]]])))


(defn badge-content-modal-render [data state]
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
        [:a " >> Apply now"]]
       [:div.pull-right [:a "to wishlist"]]]]]
    
    ]])



(defn badge-content-modal [data state]
  (create-class {:reagent-render (fn [] (badge-content-modal-render data state))
                 :component-will-unmount (fn [] (do (close-modal!)
                                                    ;(if (and init-data state) (init-data state))
                                                    ))}))

(defn open-modal [data state]
  (m/modal! [badge-content-modal data state] {:size :lg}))




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
                              (swap! state assoc :value #{}
                                                 :tags ())
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


(defn taghandler [state items value]
  (let [tags (cursor state [:tags])]
    (reset! tags (vals (select-keys @items @value)))
    (fetch-badges state)))


(defn autocomplete [state]
  (let [tags (cursor state [:tags])
        value (cursor state [:value])
        items (cursor state [:items])]
    (fn []
      [:div.form-group
       [:label {:class "control-label col-sm-2" :for "autocomplete"} (str "keywords" ":")]
       [:div.col-sm-10
        [multiple-autocomplete
         {:value @value
          :cb    (fn [item] (do
                              (swap! value conj (:key item))
                              (taghandler state  items value)
                              ))
                                        ; FIXME: Remove-cb is called with value, not item
          :remove-cb (fn [x] (do
                               (swap! value disj x)
                               (taghandler state items value)))
          
          :search-fields   [:value]
          :items           @items
          :placeholder     "search by badge name or keywords"
          :no-results-text "ei löytyny"
          :control-class   "form-control"}]
        ]])
    )
  )

(defn gallery-grid-form [state]
  (let [show-advanced-search (cursor state [:advanced-search])]
    [:div {:id "grid-filter"
           :class "form-horizontal"}
     (if (not (:user-id @state))
       [:div
        [country-selector state]
        [autocomplete state]
        [text-field :name "Badge name" "search by badge name" state]
        [text-field :issuer-name (t :gallery/Issuer) (t :gallery/Searchbyissuer) state]
        ])
     [g/grid-radio-buttons (str (t :core/Order) ":") "order" (order-radio-values) :order state fetch-badges]]))

(defn badge-grid-element [element-data state]
  (let [{:keys [id image_file name description issuer_content_name issuer_content_url recipients badge_content_id]} element-data
        badge-id (or badge_content_id id)]
     [:div {:class "media grid-container"}
      [:div.media-content
       (if image_file
         [:div.media-left
          [:a {:href "#" :on-click #(open-modal element-data state) :title name}
           [:img {:src (str "/" image_file)
                 :alt name}]]])
       [:div.media-body
        [:div.media-heading
         [:a.heading-link {:on-click #(do (.preventDefault %)(open-modal element-data state)) :title name}
          name]]
        [:div.media-issuer
         [:a {:href issuer_content_url
              :target "_blank"
              :title issuer_content_name} issuer_content_name]]
        [:div.media-button
         [:button {:class "btn btn-default" :on-click #(do (.preventDefault %)(open-modal element-data state))
                   } "Get this badge"]]
        
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
                                   [:div {:id "badge-gallery"}
                                    [m/modal-window]
                                    [gallery-grid-form state]
                                    
                                    (if (:ajax-message @state)
                                      [:div.ajax-message
                                       [:i {:class "fa fa-cog fa-spin fa-2x "}]
                                       [:span (:ajax-message @state)]]
                                      [gallery-grid state])])
                 :component-did-mount (fn []
                                        ;(if badge_content_id (open-modal badge_content_id true))
                                        )}))

(defn init-data [state {:keys [country name issuer-name order]}]
  (ajax/GET
   (path-for "/obpv1/application/")
   {:params  {:country  (trim country)
              :name_tag (trim name)
              :issuer   (trim issuer-name)
              :order    (trim order)}
    :handler (fn [data]
               (let [{:keys [applications countries user-country]} data]
                 (swap! state assoc :applications applications
                        :countries countries
                        ;:items (simple-items 5)
                        )))}))




(defn handler [site-navi params]
  (let [user-id (:user-id params)
        query-string (keywordize-keys (:query (url/url (-> js/window .-location .-href))))
        init-values {:country  (or (:country query-string) (session/get-in [:user :country] "all"))
                     :name (or (:name query-string) "")
                     :issuer-name   (or (:issuer-name query-string) "")
                     :order (or (:order query-string) "mtime")}
        badge_content_id (:badge_content_id params)
        state (atom {:tags ()
                     :value #{}
                     :user-id user-id
                     :badges []
                     :countries []
                     :country-selected  (or (:country query-string) (session/get-in [:user :country] "all"))
                     :advanced-search false
                     :name (or (:name query-string) "")
                     :issuer-name (or (:issuer-name query-string) "")
                     :order  (or (:order query-string) "mtime")
                     :timer nil
                     :items #{}
                     :ajax-message nil})]
    (init-data state init-values)
    (autocomplete-search state (:country init-values))
    (fn []
      (if (session/get :user)
        (layout/default site-navi [content state badge_content_id])
        (layout/landing-page site-navi [content state badge_content_id]))

      )))
