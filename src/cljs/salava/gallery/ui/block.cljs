(ns salava.gallery.ui.block
  (:require [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.ui.badge-grid :refer [badge-grid-element]]
            [salava.core.i18n :refer [t]]
            [reagent.core :refer [atom cursor]]
            [salava.core.ui.page-grid :refer [page-grid-element]]
            [salava.user.ui.helper :refer [profile-link-inline-modal]]
            [reagent.session :as session]))

(defn init-grid [kind state]
  (ajax/GET
    (path-for "/obpv1/gallery/recent")
    {:params {:kind kind
              :userid (:user-id @state)}
     :handler (fn [data] (swap! state merge data))}))

(defn page-grid [pages page-small-view]
  (into [:div {:class "row wrap-grid" :id "grid"}]
        (for [element-data (if page-small-view (sort-by :mtime > pages) (take 6 (sort-by :mtime > pages)))]
          (page-grid-element element-data {:type "profile"}))))


(defn badge-grid [badges badge-small-view]
  (into [:div {:class "row wrap-grid" :id "grid"}]
        (for [element-data (if badge-small-view (sort-by :mtime > badges) (take 6 (sort-by :mtime > badges)))]
          (badge-grid-element element-data nil "profile" nil))))

(defn ^:export recentbadges
 ([data]
  (let [badge-small-view (cursor data [:badge-small-view])
        {:keys [user-id user]} data]
    (init-grid "badges" data)
   (fn []
    (if (seq (:badges @data))
       [:div#user-badges
        [:div.row.wrap-grid
         [:div.col-md-12
          [:h3 {:class ""} (t :user/Recentbadges)]
          [badge-grid (:badges @data) @badge-small-view]
          (when (< 6 (count @(cursor data [:badges])))
           [:div [:a {:href "#" :on-click #(reset! badge-small-view (if @badge-small-view false true))}  (if @badge-small-view (t :admin/Showless) (t :user/Showmore))]])]]]
     (when @(cursor data [:edit-mode]) [:div.row
                                        [:div.col-md-12
                                         [:h3 {:class ""} (t :user/Recentbadges)]]])))))
 ([data badge-type]
  (init-grid "badges" data)
  (case badge-type
     "embed" (fn []
              (when (seq (:badges @data))
               [:div#user-badges
                [:div.row.wrap-grid
                 [:div.col-md-12
                  [:h3 (t :user/Recentbadges)]
                  [:div
                   (into [:div.row.wrap-grid {:id "grid"}]
                    (for [element-data (:badges @data)]
                     (badge-grid-element element-data nil "embed" nil)))
                   #_[:div [:a {:target "_blank" :href (path-for (str "/gallery/badges/" (:user-id @data)))} (t :user/Showmore)]]]]]]))
   (recentbadges data))))



(defn ^:export recentpages
 ([data]
  (let [page-small-view (cursor data [:page-small-view])
        {:keys [user-id user]} data]
    (init-grid "pages" data)
    (fn []
     (if (seq (:pages @data))
      [:div#user-pages
       [:div.row.wrap-grid
        [:div.col-md-12
         [:h3 {:class ""} (t :user/Recentpages)]
         [page-grid (:pages @data) @page-small-view]

         (when (< 6 (count @(cursor data [:pages])))
          [:div [:a {:href "#" :on-click #(reset! page-small-view (if @page-small-view false true))}  (if @page-small-view (t :admin/Showless) (t :user/Showmore))]])]]]
      (when @(cursor data [:edit-mode])
         [:div#user-pages
          [:div.row.flip
           [:div.col-md-12
            [:h3 {:class ""} (t :user/Recentpages)]]]])))))

 ([data page-type]
  (init-grid "pages" data)
  (case page-type
   "embed" (fn []
            (when (seq (:pages @data))
             [:div#user-pages
              [:div.row.wrap-grid
               [:div.col-md-12
                [:h3 (t :user/Recentbadges)]
                [:div
                 (into [:div.row.wrap-grid {:id "grid"}]
                  (for [element-data (:pages @data)]
                   (page-grid-element element-data {:type page-type})))
                 #_[:div [:a {:target "_blank" :href (path-for (str "/gallery/pages/" (:user-id @data)))} (t :user/Showmore)]]]]]]))
   (recentpages data))))

(defn ^:export badge-recipients [params]
  (let [{:keys [gallery_id id data]} params
        state (or (atom data) (atom {}))
        expanded (atom false)]
    (when (session/get :user)
      (when (empty? @state)(ajax/GET
                            (path-for (str "/obpv1/gallery/recipients/" gallery_id))
                            {;:params {:galleryid gallery_id}
                             :handler (fn [data]
                                        (reset! state data))}))
      (fn []
       (let [{:keys [public_users private_user_count all_recipients_count]} @state
              icon-class (if @expanded "fa-chevron-circle-down" "fa-chevron-circle-right")
              title (if @expanded (t :core/Clicktocollapse) (t :core/Clicktoexpand))]
        [:div.row
         [:div.col-md-12
          [:div.panel.expandable-block ;{:style {:padding "unset"}}
           [:div.panel-heading {:style {:padding "unset"}}
            [:h2.uppercase-header (str (t :gallery/recipients) ": " all_recipients_count)]
            [:a {:href "#" :on-click #(do (.preventDefault %)
                                          (if @expanded (reset! expanded false) (reset! expanded true)))}
              ;[:h2.uppercase-header (str (t :gallery/Allrecipients) ": " all_recipients_count)]
              [:i.fa.fa-lg.panel-status-icon.in-badge {:class icon-class :title title}]]]
           (when @expanded
             [:div.panel-body {:style {:padding "unset"}}
              [:div
               (into [:div]
                     (for [user public_users
                           :let [{:keys [id first_name last_name profile_picture]} user]]
                       (profile-link-inline-modal id first_name last_name profile_picture)))
               (when (> private_user_count 0)
                 (if (> (count public_users) 0)
                   [:span "... " (t :core/and) " " private_user_count " " (t :core/more)]
                   [:span private_user_count " " (if (> private_user_count 1) (t :gallery/recipients) (t :gallery/recipient))]))]])]]])))))
