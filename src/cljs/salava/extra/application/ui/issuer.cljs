(ns salava.extra.application.ui.issuer
  (:require [reagent.core :refer [atom cursor create-class]]
            [reagent-modals.modals :as m :refer [close-modal!]]
            [clojure.string :refer [trim blank?]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.helper :refer [dump]]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.core.ui.modal :as mo]
            [medley.core :refer [distinct-by]]))

(defn hashtag? [text]
  (re-find #"^#" text))

(defn subs-hashtag [text]
  (trim text)
  (if (hashtag? text)
    (subs text 1)
    text))

(defn create-favourite-list [favourites all]
 (sort-by :issuer_content_name (distinct-by :issuer_content_id (filter (fn [app] (some #(= (:id %) (:issuer_content_id app)) favourites)) all))))

(defn get-favourites [state]
  (ajax/GET
    (path-for (str "/obpv1/social/connections_issuer"))
    {:handler (fn [data]
                (swap! state assoc :favourites (create-favourite-list data (:all-applications @state)))
                )}))

(defn init-issuer-applications [state]
  (let [{:keys [user-id country-selected name recipient-name issuer-name order tags show-followed-only]} @state]
    (ajax/GET
      (path-for (str "/obpv1/application/"))
      {:params {:country (trim country-selected)
                :name (subs-hashtag name)
                :tags (map #(subs-hashtag %) tags)
                :issuer ""
                :order (trim order)
                :followed show-followed-only}
       :handler (fn [data]
                  (swap! state assoc :all-issuer-applications (:applications data)
                                     :show-favourite-issuers false)
                  (get-favourites state))})))

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

(defn init-issuer-connection [issuer-id state]
  (ajax/GET
    (path-for (str "/obpv1/social/issuer_connected/" issuer-id))
    {:handler (fn [data]
                (swap! state assoc-in [:issuer-content :connected] data)
                )}))

(defn add-issuer-to-favourites [issuer-id state]
  (ajax/POST
    (path-for (str "/obpv1/social/create_connection_issuer/" issuer-id))
    {:handler (fn []
                (init-issuer-connection issuer-id state))}))


(defn remove-issuer-from-favourites [issuer-id state init-fn]
  (ajax/POST
    (path-for (str "/obpv1/social/delete_connection_issuer/" issuer-id))
    {:handler (fn []
                (init-issuer-connection issuer-id state))}))

(defn issuer-applications [issuer-name state]
  (swap! state assoc :issuer-name issuer-name)
  (fetch-badge-adverts state))

(defn issuer-applications-count [issuer-name state]
  (count (filter #(= issuer-name (:issuer_content_name %)) (:all-issuer-applications @state))))

(defn- issuer-image [path]
  (when (not-empty path)
    [:img.profile-picture
     {:src (if (re-find #"^file/" path) (str "/" path) path)
      :style {:width "50px" :padding-right "10px"}}]))



(defn text-field [key label placeholder state]
  (let [applications (cursor state [:all-applications])
        search-atom (cursor state [key])
        favourites (cursor state [:favourites])
        show-favourites-atom (cursor state [:show-favourite-issuers])
        current-application-list (if @show-favourites-atom favourites applications)]
    [:div.form-group
     ;[:label {:class "control-label col-sm-2" :for field-id} (str label ":")]
     [:div.col-sm-10
      [:input {:class       (str "form-control")
               :type        "text"
               :placeholder placeholder
               :value       @search-atom
               :on-change   #(do
                               (reset! search-atom (.-target.value %))
                               (reset! current-application-list (filter (fn [s]
                                         (->> s (:issuer_content_name) (re-find (re-pattern (str "(?i)" @search-atom))))) @current-application-list))
                               (when (blank? @search-atom) (issuer-applications "" state)))

               #_(search-timer state)}]]]))


(defn issuer-info-grid [state]
  (let [show-issuer-info-atom (cursor state [:show-issuer-info])
        issuer-content (cursor state [:issuer-content])
        {:keys [id name image url banner connected]} @issuer-content]
    ;https://openbadgefactory.com/c/download/9ce0fe80b799923f3a02395aa918d6602bdf03f4eb854a6f35f3ac6221fa1976.png
    (if @show-issuer-info-atom
      [:div.row.issuer-grid
       [:div.col-xs-12
        (if banner
          [:img.img-responsive
           {:src "https://openbadgefactory.com/c/download/9ce0fe80b799923f3a02395aa918d6602bdf03f4eb854a6f35f3ac6221fa1976.png" #_"https://openbadgefactory.com/c/download/c3eb37b1114f38b3183eca5add6a9682d77e3cdff16467539dc9877be0bd6b2d.png"}])
         [:div.col-xs-12.info-block
          [:div.col-xs-12
           (when-not banner
             [:h2.uppercase-header.pull-left
              (issuer-image image)
              " "
              name])]
          [:div.col-xs-12.footer
           [:div.pull-left [:a {:href "#" :on-click #(do
                                                       (.preventDefault %)
                                                       (mo/open-modal [:badge :issuer] id))} (t :admin/Showmore)]]
           [:div.pull-right
            (if-not connected
              [:a {:href "#" :on-click #(add-issuer-to-favourites id state)} [:i {:class "fa fa-bookmark-o"}] (str " " (t :badge/Addtofavourites))]
              [:a {:href "#" :on-click #(remove-issuer-from-favourites id state nil)} [:i {:class "fa fa-bookmark"}] (str " " (t :badge/Removefromfavourites))]
              )]]]]])))

(defn issuer-filter-grid [show-favourites-atom state]
  [:div#grid-filter {:class "form-horizontal" :style {:margin-bottom "10px"}}
   [:div {:class "form-group wishlist-buttons"}
       #_[:label {:for   "input-email-notifications"
            :class "col-md-2"}
    (str (t :core/Show) ":")]
    [:div.col-md-10
    [:div.buttons
     [:button {:class (str "btn btn-default " (if-not @show-favourites-atom "btn-active"))
               :id "btn-all"
               :on-click #(reset! show-favourites-atom (if @show-favourites-atom false true))}
      (t :core/All)]
     [:button {:class (str "btn btn-default " (if @show-favourites-atom "btn-active"))
               :id "btn-all"
               :on-click #(reset! show-favourites-atom (if @show-favourites-atom false true))}
      [:i {:class "fa fa-bookmark"}] (str " " (t :extra-application/Favourites))]]]]
   [:div {:style {:margin-top "5px"}}
    [text-field :issuer  nil "search issuer" state]]])

(defn favourites-block [favourites state]
  (if-not (empty? @favourites)
    [:div
     (doall
       (for [app @favourites
             :let [{:keys [issuer_content_name issuer_image issuer_content_url issuer_content_id]} app
                   applications-count (issuer-applications-count issuer_content_name state)
                   ;testing
                   banner (if (even? (count issuer_content_name)) true false)]]
         [:a { :key issuer_content_id
               :data-dismiss "modal"
               :on-click #(do
                            (.preventDefault %)
                            (init-issuer-connection issuer_content_id state)
                            (swap! state assoc :show-issuer-info true
                                   :issuer-content {:id issuer_content_id :name issuer_content_name :image issuer_image :url issuer_content_url :banner banner})
                            (issuer-applications issuer_content_name state))}

          [:div {:style {:padding "5px"}} (if issuer_image
                                            [:img.badge-icon {:style {:width "30px" :padding-right "10px"} :src (str "/" issuer_image)}])
           (str issuer_content_name "  ("  applications-count ")")]]

         ))]
    [:div]))

(defn issuer-block [applications state]
  (into
    [:div
     [:a {:data-dismiss "modal"
          :on-click #(do
                       (swap! state assoc :show-issuer-info false :issuer-content {:name (t :core/All)})
                       (issuer-applications "" state))} [:div.all (t :core/All)]]]
    (doall
      (for [app (sort-by :issuer_content_name (distinct-by :issuer_content_name @applications))
            :let [{:keys [issuer_content_name issuer_image issuer_content_url issuer_content_id]} app
                  applications-count (issuer-applications-count issuer_content_name state)
                  ;testing
                  banner (if (even? (count issuer_content_name)) true false)]]
        ^{:key app}[:a { :data-dismiss "modal"
              :on-click #(do
                           (.preventDefault %)
                           (init-issuer-connection issuer_content_id state)
                           (swap! state assoc :show-issuer-info true
                                  :issuer-content {:id issuer_content_id :name issuer_content_name :image issuer_image :url issuer_content_url :banner banner})
                           (issuer-applications issuer_content_name state))}

         [:div {:style {:padding "5px"}} (if issuer_image
                                           [:img.badge-icon {:style {:width "30px" :padding-right "10px"} :src (str "/" issuer_image)}])
          (str issuer_content_name "  ("  applications-count ")")]]))))

(defn issuer-content-modal [state]
  (let [applications (cursor state [:all-applications])
        issuer-name (cursor state [:issuer-content :name])
        favourites (cursor state [:favourites])
        show-favourites-atom (cursor state [:show-favourite-issuers])]
    (fn []
      [:div#badge-content
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
        [:div.issuer-list
         [issuer-filter-grid show-favourites-atom state]
         (if @show-favourites-atom [favourites-block favourites state] [issuer-block applications state])]]
       [:div.modal-footer]])))


(defn open-issuer-modal [state]
  (create-class {:reagent-render (fn [] (issuer-content-modal state))
                 :component-will-unmount (fn [] (do (close-modal!)))}))


(defn select-issuer [state]
  (let [applications (cursor state [:all-applications])
        issuer-name (cursor state [:issuer-content :name])]
    [:div.form-group
     [:label {:class "control-label col-sm-2" :for "select-issuer"} (str (t :gallery/Searchbyissuer) ":")]
     [:div.col-sm-10
      [:button.issuer-button {:class (str "btn form-control btn-active")
                              :id "btn-all"
                              :on-click #(do
                                           (m/modal! [open-issuer-modal state] {:size :md :shown (fn []  (init-issuer-applications state)) }))} (str @issuer-name)]]]))