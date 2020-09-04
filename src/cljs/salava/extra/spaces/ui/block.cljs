(ns salava.extra.spaces.ui.block
 (:require
  [salava.core.ui.ajax-utils :as ajax]
  [salava.core.ui.helper :refer [path-for js-navigate-to current-route-path navigate-to]]
  [salava.core.ui.layout :as layout]
  [salava.core.i18n :refer [t translate-text]]
  [reagent.session :as session]
  [reagent.core :refer [cursor atom]]
  [reagent-modals.modals :as m]
  [dommy.core :as dommy :refer-macros [sel sel1]]
  [salava.extra.spaces.ui.helper :refer [space-card upload-modal]]
  [cemerick.url :as url]
  [clojure.walk :refer [keywordize-keys]]
  [salava.core.ui.modal :as mo]))


(defn stylyze-element [element color]
  (when-let [$ element] (dommy/set-style! $ :background "none" :background-color color)))

(defn stylyze-element-color [element color]
  (when-let [$ element] (dommy/set-style! $ :color "none" :color color)))

(defn stylyze-links [selector color]
  (doall
   (doseq [a (remove (fn [e] (some #(= e %) (sel :.btn))) selector)]
      (dommy/set-style! a :color color))))

(defn stylyze-element-multi [elements color]
 (when elements
  (doall
   (doseq [$ elements]
     (dommy/set-style! $ :background-color "transparent" :background (str color))))))

(defn stylyze []
  (let [{:keys [p-color s-color t-color]} (session/get-in [:user :current-space :css])]
   (doall
    [(stylyze-element (sel1 ".welcome-block") p-color)
     (stylyze-element (sel1 ".title-row") p-color)
     #_(stylyze-element (sel1 [:#theme-0 :.panel-right]) p-color)
     #_(stylyze-element (sel1 [:#theme-0 :.panel-left]) p-color)
     #_(stylyze-links (sel :a) p-color)
     #_(stylyze-links (sel [:.help :a :p]) p-color)
     #_(stylyze-links (sel [:#dashboard :.block :.title]) p-color)
     #_(stylyze-links (sel [:#dashboard :.block :.icon]) p-color)
     (stylyze-links (sel [:#badge-info :a]) p-color)
     #_(stylyze-element-multi (sel [:.button]) p-color)])))

(defn init-spaces [state]
  (ajax/POST
    (path-for (str "/obpv1/spaces/user") true)
    {:handler (fn [data]
                (swap! state assoc :spaces data
                                   :selected (session/get-in [:user :current-space :name] nil))
                #_(when-not (empty? (session/get-in [:user :current-space :css] nil))
                    (stylyze))
                (when (:error @state)
                  (m/modal! [upload-modal {:status "error" :message "extra-spaces/Invitelinkeerror" }] {})))}))


(defn leave-organization [id state current-space?]
  [:a {:href "#" :on-click #(do (.preventDefault %)
                                (ajax/POST
                                 (path-for (str "/obpv1/spaces/user/leave/" id) true)
                                 {:params {:current-space current-space?}
                                  :handler (fn [data]
                                             (when (= "success" (:status data))
                                               (init-spaces state)
                                               (when current-space? (js-navigate-to (current-route-path)))))}))}
    [:i.fa.fa-user-times.fa-fw] (str " " (t :extra-spaces/Leavespace))])

(defn default-btn [space state]
 (let [{:keys [id default_space]} space]
   (if (pos? default_space)
     [:span [:i.fa.fa-bookmark.fa-fw] (t :extra-spaces/default)]
     [:a {:href "#" :on-click #(do
                                 (.preventDefault %)
                                 (ajax/POST
                                  (path-for (str "/obpv1/spaces/user/default/" id))
                                  {:handler (fn [data]
                                              (when (= "success" (:status data))
                                                 (init-spaces state)))}))}
       [:span [:i.fa.fa-bookmark.fa-fw] (t :extra-spaces/Setasdefault)]])))


(defn manage-spaces [state]
  (let [spaces (sort-by :name @(cursor state [:spaces]))
        current-space (session/get-in [:user :current-space])]
   [:div
    [m/modal-window]
    (if (seq spaces)
      [:div#badge-stats
       [:h1.uppercase-header (t :extra-spaces/Spaces)]
       [:p (t :extra-spaces/Spaceconnectioninfo)]
       [:div.expandable-block {:class "panel issuer-panel panel-default"}
        [:div.panel-heading
         [:h2 (str (t :extra-spaces/Spaces) " (" (count spaces) ")")]]
        [:div.panel-body.table-responsive
         [:table {:class "table"}
          [:thead
           [:tr
            [:th {:style {:display "none"}}  "Logo"]
            [:th {:style {:display "none"}} (t :badge/Name)]
            [:th {:style {:display "none"}} "role"]
            [:th {:style {:display "none"}} "Action"]]]
          (into [:tbody]
                (for [space spaces
                      :let [{:keys [space_id name logo role default_space status]} space
                            current-space? (= (:id current-space) space_id)]]
                  [:tr
                   [:td {:style {:text-align "left" :width "40%"}} [:a {:href "#"
                                                                        :on-click #(do
                                                                                     (mo/open-modal [:space :info] {:id space_id} {:hide (fn [] (init-spaces state))})
                                                                                     (.preventDefault %))}
                                                                       (if logo [:img.badge-icon {:src (str "/" logo) :alt (str name " icon")}]
                                                                                [:span [:i.fa.fa-building.fa-3x {:style {:margin-right "10px"}}]]) name]]
                   [:td (if (= status "pending") [:span.label.label-info (t :extra-spaces/pendingmembership)] [:span.label {:class (if (= role "admin") "label-danger" "label-success")} (translate-text (str "extra-spaces/" role))])]
                   (if-not (= status "pending") [:td {:style {:min-width "150px"}} "" (default-btn space state)] [:td {:style {:min-width "150px"}} " "])
                   [:td.action "" (leave-organization space_id state current-space?)]]))]]]]

     [:div.well.well-sm (t :extra-spaces/Notjoinedanyorg)])]))

(defn next-url [space]
  (let [current-path (current-route-path)
        admin? (= (:role space) "admin")]
    (if admin?
      current-path
      (if (clojure.string/starts-with? current-path "/space") "/social" current-path))))


(defn ^:export space-list []
 (let [state (atom {:selected  nil :spaces (session/get-in [:user :spaces])})
       user-id (session/get-in [:user :id])
       current-space (session/get-in [:user :current-space])]
  (init-spaces state)
  (fn []
   (let [spaces (->> @(cursor state [:spaces]) (remove #(= "pending" (:status %))))
         default-space (->> spaces (filter #(pos? (:default_space %))) first)]
     (when (seq spaces) #_(seq @(cursor state [:spaces]))
       [:div#space-list
        [:div.dropdown
         [:a.dropdown-toggle {:href "#"
                              :data-toggle "dropdown"
                              :role "button"
                              :aria-haspopup true
                              :aria-expanded false}
            (if current-space
             [:div.selected-space
              [:img.space-img {:src (str "/" (or (:logo current-space) (:logo default-space))) :alt " "}]
              [:div.name (or @(cursor state [:selected]) (:name default-space) (t :extra-spaces/Switchorganization))] [:span.caret]]
             [:div.selected-space
              [:div.logo-image.system-image-url {:style {:display "inline-block"}}]
              [:div.name (session/get :site-name)] [:span.caret]])]
         (conj
           (conj (reduce
                   (fn [r space]
                    (let [selected? (= @(cursor state [:selected])  (:name space))]
                     (conj r
                       ^{:key (:space_id space)}[:li [:a {:href "#" :on-click #(do
                                                                                 (reset! (cursor state [:selected]) (:name space))
                                                                                 (ajax/POST
                                                                                  (path-for (str "/obpv1/spaces/switch/" (:id space))true)
                                                                                  {:handler (fn [data]
                                                                                              (js-navigate-to (next-url space)))}))}

                                                        [:img.space-img {:src (str "/" (:logo space)) :alt " "}](if selected? [:b [:i (:name space)]] (:name space))]])))
                  [:ul.dropdown-menu];.pull-left]
                  (sort-by :name spaces))
                 [:li.divider {:role "seperator"}])
           [:li
            [:a {:href "#" :on-click #(do
                                        (reset! (cursor state [:selected]) (session/get :site-name))
                                        (ajax/POST
                                         (path-for (str "/obpv1/spaces/reset_switch") true)
                                         {:handler (fn [data]
                                                    (js-navigate-to (current-route-path)))}))}
             [:div.logo-image.system-image-url] (if current-space (str (t :extra-spaces/backto) " " (session/get :site-name)) [:b (session/get :site-name)])]])]])))))


(defn manage-spaces-handler [site-navi]
  (let [error (:error (-> js/window .-location .-href url/url :query keywordize-keys))
        state (atom {:error (if (= "true" error) true false)})]
   (init-spaces state)
   (fn []
     (layout/default site-navi (manage-spaces state)))))

(defn ^:export space_list_dashboard []
  (let [spaces (session/get-in [:user :spaces])
        current-space (session/get-in [:user :current-space])
        state (atom {:spaces []})]
   (init-spaces state)
   (fn []
    (let [spaces (->> @(cursor state [:spaces]) (remove #(= "pending" (:status %))))
          default-space (->> spaces (filter #(pos? (:default_space %))) first)]
      (when (seq spaces)
       [:div.box.col-md-4.col-sm-12.space-list-block
        [:div#box_6
         [:div.col-md-12.block
          [:div.row_2
           [:div.heading_1
            [:i.fa.fa-th-large.icon]
            [:a {:href (str (path-for "/connections/spaces"))}
             [:span.title (t :extra-spaces/Spaces)]]
            [:span.small.icon]]
           [:div.content
            [:p {:style {:font-size "medium"}} (t :extra-spaces/Selectspacebelow)]
            [:div#space-list
             (conj
              (reduce
               (fn [r space]
                (let [selected? (= @(cursor state [:selected])  (:name space))
                      p-color (get-in space [:css :p-color])]
                  (conj r
                   [:div.space-list-item {:class (when selected? "selected-space") :style {:border-left (str "4px solid " p-color)}};

                    [:a
                     {:on-click #(do
                                  (reset! (cursor state [:selected]) (:name space))
                                  (ajax/POST
                                   (path-for (str "/obpv1/spaces/switch/" (:id space))true)
                                   {:handler (fn [data]
                                               (js-navigate-to (next-url space)))}))}
                     [:div.media
                      [:div.media-left
                       [:img.space-img {:src (str "/" (:logo space)) :alt " "}]]
                      [:div.media-body
                       (if selected? [:b [:i (:name space)]] (:name space))
                       (when (= (:id space) (:id default-space)) [:span.default [:i.fa.fa-bookmark] " " [:b (t :extra-spaces/default)]])
                       (when selected? [:span.current [:i.fa.fa-thumb-tack] " " [:b (t :extra-spaces/currentspace)]])]]]])))
               [:div]
               (sort-by :name spaces))

              [:div.space-list-item ;{:class (when selected? "selected-space")}
               [:a {:href "#" :on-click #(do
                                           (reset! (cursor state [:selected]) (session/get :site-name))
                                           (ajax/POST
                                            (path-for (str "/obpv1/spaces/reset_switch") true)
                                            {:handler (fn [data]
                                                       (js-navigate-to (current-route-path)))}))}
                [:div.media
                  [:div.media-left
                   [:div.logo-image.system-image-url]]
                  [:div.media-body
                    (str (t :extra-spaces/backto) " " (session/get :site-name))
                    (when (empty? default-space) [:span.default [:i.fa.fa-bookmark] " " (t :extra-spaces/default)])
                    (when (empty? current-space) [:span.current [:i.fa.fa-thumb-tack] " " [:b (t :extra-spaces/currentspace)]])]]]])]]]]]])))))

(defn ^:export space_info []
 (fn []
  (when-let [current-space (session/get-in [:user :current-space] nil)]
    [:div.content-container (when (get-in current-space [:css :p-color] nil) {:style {:border-color (get-in current-space [:css :p-color])}})
     [:div.navbar.navbar-default
      [:div.container-fluid
       [:div.navbar-header.banner-container
        (when (:banner current-space)
           {:style {:background-image (str "url(" (session/get :site-url) "/" (:banner current-space) ")")}})
        [:div.media
         [:div.media-left;.navbar-brand
          [:img.logo { :src (str "/" (:logo current-space))}]]
         [:div.media-body {:style {:vertical-align "middle"}}
          [:h3.media-heading {:style {:font-weight "600"}} (:name current-space)]
          [:div.space-description {:style {:max-width "300px"}}(:description current-space)]]]]]]])))

(defn ^:export spaces_stats_dropdown [state]
 (let [v (atom {:spaces [] :selected 0})]
   (ajax/GET
    (path-for "/obpv1/spaces/")
    {:handler (fn [data]
                (swap! v assoc :spaces data))})
   (fn []
    (when (seq @(cursor v [:spaces]))
     [:div.row
      [:div.col-md-12
       [:div.form-group {:style {:margin "20px 0"}}
        [:label {:for "space-select"}
         (t :extra-spaces/Selectforstats)]
        (reduce
         (fn [r space]
           (conj r
            [:option
             {:value (:id space)}
             (:name space)]))
         [:select#space-select.form-control
           {:on-change (fn [x]
                         (do
                          (reset! (cursor v [:selected]) (-> x .-target .-value))
                          (if (pos? @(cursor v [:selected]))
                            (ajax/POST
                             (path-for (str "/obpv1/space/stats/" @(cursor v [:selected])))
                             {:handler (fn [data]
                                         (reset! state (assoc data :visible "graphic" :space-id @(cursor v [:selected]))))})
                            (ajax/GET
                             (path-for "/obpv1/admin/stats")
                             {:handler (fn [data]
                                         (reset! state (assoc data :visible "graphic"))
                                         (ajax/GET
                                          (path-for "/obpv1/stats/social_media")
                                          {:handler (fn [data]
                                                      (reset! (cursor state [:social_media_stats]) data))}))}))))}


           [:option {:value 0} (session/get :site-name)]]

         (:spaces @v))]]]))))

(defn ^:export gallery_badge_space_selector [state fetch-fn]
  (let [params (cursor state [:params])
        space (cursor state [:params :space-id])
        spaces-atom (atom [])
        country (cursor state [:params :country])]0
    (if (= (session/get-in [:user :role]) "admin")
     (ajax/GET
      (path-for "/obpv1/spaces/")
      {:handler (fn [data]
                  (reset! spaces-atom data))})
                  ;(swap! spaces-atom assoc :spaces data))})
     (ajax/POST
       (path-for (str "/obpv1/spaces/user") true)
       {:handler (fn [data]
                   (reset! spaces-atom data))}))
    (fn []
     (let [spaces @spaces-atom]
      [:div.form-group
       [:label {:class "control-label col-sm-2" :for "space-selector"} (str (t :extra-spaces/Space) ":")]
       [:div.col-sm-10
        [:select {:class     "form-control"
                  :id        "space-selector"
                  :name      "space"
                  :value     @space
                  :on-change #(do
                                (reset! space (.-target.value %))
                                (reset! params (merge (:params @state) {:country "all" :space-id @space}))
                                (fetch-fn))}
         [:option {:value 0 :key "all"} (t :core/All)]
         (for [s spaces]
           [:option {:key (:id s) :value (:id s)} (:name s)])]
        (when (pos? @space)
         [:p.help-block.text-muted
          [:i.fa.fa-fw.fa-lg.fa-info-circle]
          [:b (t :extra-spaces/Badgeselectorinfo)]])]]))))

(defn ^:export gallery_page_space_select [state fetch-fn]
 (let [space (cursor state [:space])
       spaces-atom (atom [])
       ;spaces [(session/get-in [:user :current-space])]
       country (cursor state [:country-selected])]
    (if (= (session/get-in [:user :role]) "admin")
     (ajax/GET
      (path-for "/obpv1/spaces/")
      {:handler (fn [data]
                  (reset! spaces-atom data))})
                  ;(swap! spaces-atom assoc :spaces data))})
     (ajax/POST
       (path-for (str "/obpv1/spaces/user") true)
       {:handler (fn [data]
                   (reset! spaces-atom data))}))
   (fn []
    (let [spaces @spaces-atom]
      [:div.form-group
       [:label {:class "control-label col-sm-2" :for "space-selector-pages"} (str (t :extra-spaces/Space) ":")]
       [:div.col-sm-10
        [:select#space-selector-pages.form-control
         {:name "space-selector"
          :value @space
          :on-change #(do
                        (.preventDefault %)
                        (reset! space (.-target.value %))
                        (reset! country "all")
                        (fetch-fn))}
         [:option {:value 0 :key "all"} (t :core/All)]
         (for [p spaces]
           [:option {:key (:id p) :value (:id p)} (:name p)])]
        (when (pos? @space)
         [:p.help-block.text-muted
          [:i.fa.fa-fw.fa-lg.fa-info-circle]
          [:b (t :extra-spaces/Pageselectorinfo)]])]]))))


(defn ^:export gallery_profiles_space_select [state fetch-fn]
 (let [space (cursor state [:space])
       spaces-atom (atom [])
       current-space [(session/get-in [:user :current-space])]
       country (cursor state [:country-selected])]
   (when (= (session/get-in [:user :role]) "admin")
    (ajax/GET
     (path-for "/obpv1/spaces/")
     {:handler (fn [data]
                 (reset! spaces-atom data))})
                 ;(swap! spaces-atom assoc :spaces data))})
    #_(ajax/POST
        (path-for (str "/obpv1/spaces/user") true)
        {:handler (fn [data]
                    (reset! spaces-atom data))}))
   (fn []
    (let [spaces (if (= (session/get-in [:user :role]) "admin") @spaces-atom current-space)]
      [:div.form-group
       [:label {:class "control-label col-sm-2" :for "space-selector-profiles"} (str (t :extra-spaces/Space) ":")]
       [:div.col-sm-10
        [:select#space-selector-profiles.form-control
         {:name "space-selector"
          :value @space
          :on-change #(do
                        (.preventDefault %)
                        (reset! space (.-target.value %))
                        ;(reset! country "all")
                        (fetch-fn))}
         [:option {:value 0 :key "all"} (t :core/All)]
         (for [p (if (= (session/get-in [:user :role]) "admin") @spaces-atom current-space)]
           [:option {:key (:id p) :value (:id p)} (:name p)])]
        (when (pos? @space)
         [:p.help-block.text-muted
          [:i.fa.fa-fw.fa-lg.fa-info-circle]
          [:b (t :extra-spaces/Profileselectorinfo)]])]]))))
