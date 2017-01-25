(ns salava.extra.application.ui.application
  (:require [reagent.core :refer [atom cursor create-class]]
            [reagent.session :as session]
            [reagent-modals.modals :refer [close-modal!]]
            [reagent-modals.modals :as m]
            [clojure.string :refer [trim]]
            [salava.core.ui.grid :as g]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.helper :refer [path-for unique-values]]
            [reagent.session :as session]
            [salava.core.helper :refer [dump]]
            [clojure.string :as s]
            ;[ajax.core :as ajax]
            [salava.core.i18n :as i18n :refer [t]]))

;;Modal



(defn tag-parser [tags]
  (if tags
    (let [splitted (s/split tags #",")
                                        ;list (map #(vec (re-seq #"\S+" %)) splitted)
                                        ;email-map (map #(assoc {} :email (get % 0) :primary (js/parseInt (get % 1))) list)
          ]
      splitted)))

(defn modal-content [data]
  (let [{:keys [image_file name info issuer_content_name tags]} data
        tags (tag-parser tags)]
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
                     [:a {:href "#" :id "tag"} (str "#" tag )])))
           ]

          
          
          ]]]])))


(defn badge-content-modal-render [data]
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
    [modal-content data]]
   [:div.modal-footer
    [:div {:class "badge-content"}
     [:div {:class "badge-contents col-xs-12"}
      [:div.col-md-3 [:div]]
      [:div {:class "col-md-9 badge-info"}
       [:div.pull-left
        [:a " >> Apply now"]]
       [:div.pull-right [:a "to wishlist"]]]]]
    
    ]])



(defn badge-content-modal [data]
  (create-class {:reagent-render (fn [] (badge-content-modal-render data))
                 :component-will-unmount (fn [] (do (close-modal!)
                                                    ;(if (and init-data state) (init-data state))
                                                    ))}))

(defn open-modal [data]
  (m/modal! [badge-content-modal data] {:size :lg}))




(defn ajax-stop [ajax-message-atom]
  (reset! ajax-message-atom nil))




(defn fetch-badges [state]
  (let [{:keys [user-id country-selected badge-name recipient-name issuer-name]} @state
        ajax-message-atom (cursor state [:ajax-message])]
    (reset! ajax-message-atom (t :gallery/Searchingbadges))
    (ajax/POST
      (path-for (str "/obpv1/gallery/badges/" user-id))
      {:params  {:country   (trim country-selected)
                 :badge     (trim badge-name)
                 :recipient (trim recipient-name)
                 :issuer    (trim issuer-name)}
       :handler (fn [data]
                  (swap! state assoc :badges (:badges data)))
       :finally (fn []
                  (ajax-stop ajax-message-atom))})))

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
                             (fetch-badges state))}
       [:option {:value "all" :key "all"} (t :core/All)]
       (for [[country-key country-name] (map identity (:countries @state))]
         [:option {:value country-key
                   :key country-key} country-name])]]]))

(defn order-radio-values []
  [{:value "mtime" :id "radio-date" :label (t :core/bydate)}
   {:value "name" :id "radio-name" :label (t :core/byname)}
   {:value "issuer_content_name" :id "radio-issuer-name" :label (t :core/byissuername)}])

(defn gallery-grid-form [state]
  (let [show-advanced-search (cursor state [:advanced-search])]
    [:div {:id "grid-filter"
           :class "form-horizontal"}
     (if (not (:user-id @state))
       [:div
        [country-selector state]
        [text-field :badge-name-keyword "badge name and keywords" "search by badge name and keywords" state]
        [text-field :issuer-name (t :gallery/Issuer) (t :gallery/Searchbyissuer) state]
        ])
     [g/grid-radio-buttons (str (t :core/Order) ":") "order" (order-radio-values) :order state]]))

(defn badge-grid-element [element-data state]
  (let [{:keys [id image_file name description issuer_content_name issuer_content_url recipients badge_content_id]} element-data
        badge-id (or badge_content_id id)]
     [:div {:class "media grid-container"}
      [:div.media-content
       (if image_file
         [:div.media-left
          [:a {:href "#" :on-click #(open-modal element-data) :title name} ;
           [:img {:src (str "/" image_file)
                 :alt name}]]])
       [:div.media-body
        [:div.media-heading
         [:a.heading-link {:on-click #(do (.preventDefault %)(open-modal element-data)) :title name}
          name]]
        [:div.media-issuer
         [:a {:href issuer_content_url
              :target "_blank"
              :title issuer_content_name} issuer_content_name]]
        [:div.media-button
         [:button {:class "btn btn-default"} "Get this badge"]]
        
        [:div.media-description description]]]
      [:div.media-bottom
       
       ;(admin-gallery-badge badge-id "badges" state init-data)
       ]]))

(defn gallery-grid [state]
  (let [badges (:applications @state)]
    (into [:div {:class "row"
                 :id    "grid"}]
          (for [element-data badges]
            (badge-grid-element element-data state)))))


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

(defn init-data [state]
  (ajax/GET
     (path-for "/obpv1/application/") 
     
     {:handler (fn [data]
                 (let [{:keys [applications countries user-country]} data]
                   (swap! state assoc :applications applications
                          :countries countries
                          :country-selected user-country)))})
  )


(defn handler [site-navi params]
  (let [user-id (:user-id params)
        badge_content_id (:badge_content_id params)
        state (atom {:user-id user-id
                     :badges []
                     :countries []
                     :country-selected ""
                     :advanced-search false
                     :badge-name ""
                     :recipient-name ""
                     :issuer-name ""
                     :order "mtime"
                     :timer nil
                     :ajax-message nil})]
    (dump params)
    (init-data state)
    
    (fn []
      (if (session/get :user)
        (layout/default site-navi [content state badge_content_id])
        (layout/landing-page site-navi [content state badge_content_id])))))
