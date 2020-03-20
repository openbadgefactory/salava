(ns salava.gallery.ui.block
  (:require [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.ui.badge-grid :refer [badge-grid-element]]
            [salava.core.i18n :refer [t]]
            [reagent.core :refer [atom cursor create-class]]
            [salava.core.ui.page-grid :refer [page-grid-element]]
            [salava.user.ui.helper :refer [profile-link-inline-modal]]
            [reagent.session :as session]
            [salava.gallery.ui.profiles :as profiles]
            [reagent-modals.modals :as m]
            [salava.core.ui.modal :as mo]
            [salava.user.ui.helper :refer [profile-picture]]))

(defn init-grid [kind state]
  (ajax/GET
   (path-for "/obpv1/gallery/recent")
   {:params {:kind kind
             :userid (:user-id @state)}
    :handler (fn [data] (swap! state merge data))}))

(defn page-grid [pages page-small-view]
  (into [:div {:class "row wrap-grid" :id "pages-grid"}]
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
          [:div.row.wrap-grid ;{:id "grid"}
           [:div.col-md-12
            [:h2.sectiontitle (t :user/Recentbadges)]
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
                  [:div.row.wrap-grid ;{:id "grid"}
                   [:div.col-md-12
                    [:h2.sectiontitle (t :user/Recentbadges)]
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
            [:h2.sectiontitle {:class ""} (t :user/Recentpages)]
            [page-grid (:pages @data) @page-small-view]

            (when (< 6 (count @(cursor data [:pages])))
              [:div [:a {:href "#" :on-click #(reset! page-small-view (if @page-small-view false true))}  (if @page-small-view (t :admin/Showless) (t :user/Showmore))]])]]]
         (when @(cursor data [:edit-mode])
           [:div#user-pages
            [:div.row.flip
             [:div.col-md-12
              [:h2.sectiontitle {:class ""} (t :user/Recentpages)]]]])))))

  ([data page-type]
   (init-grid "pages" data)
   (case page-type
     "embed" (fn []
               (when (seq (:pages @data))
                 [:div#user-pages
                  [:div.row.wrap-grid {:id "pages-grid"}
                   [:div.col-md-12
                    [:h2.sectiontitle (t :user/Recentpages)]
                    [:div
                     (into [:div.row.wrap-grid] ;{:id "grid"}]
                           (for [element-data (:pages @data)]
                             (page-grid-element element-data {:type page-type})))
                     #_[:div [:a {:target "_blank" :href (path-for (str "/gallery/pages/" (:user-id @data)))} (t :user/Showmore)]]]]]]))
     (recentpages data))))

(defn ^:export badge-recipients [params]
  (let [{:keys [gallery_id id data]} params
        state (or (atom data) (atom {}))
        expanded (atom false)]
    (when (session/get :user)
      (when (empty? @state) (ajax/GET
                             (path-for (str "/obpv1/gallery/recipients/" gallery_id))
                             {:handler (fn [data]
                                         (reset! state data))}))
      (fn []
        (let [{:keys [public_users private_user_count all_recipients_count]} @state
              icon-class (if @expanded "fa-chevron-circle-down" "fa-chevron-circle-right")
              title (if @expanded (t :core/Clicktocollapse) (t :core/Clicktoexpand))]
          [:div.row
           [:div.col-md-12
            [:div.panel.expandable-block
             [:div.panel-heading {:style {:padding "unset"}}
              [:h2.uppercase-header (str (t :gallery/recipients) ": " all_recipients_count)]
              [:a {:href "#" :on-click #(do (.preventDefault %)
                                            (if @expanded (reset! expanded false) (reset! expanded true)))
                   :aria-label title}
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

(defn- add-or-remove [x coll]
  (if (some #(= (:id x) %) (mapv :id @coll))
    (reset! coll (->> @coll (remove #(= (:id x) (:id %))) vec))
    (reset! coll (conj @coll (select-keys x [:id :first_name :last_name :profile_picture])))))

(defn- endorsement-info-label [{:keys [request received]}]
  (cond
    (= "accepted" received) [:span.label.label-success.endorsement-info-label [:i.fa.fa-thumbs-o-up] (t :badge/Endorsedyou)]
    (= "pending" received)  [:span.label.label-info.endorsement-info-label [:i.fa.fa-thumbs-o-up] (t :badge/pendingreceived)]
    (and (complement (clojure.string/blank? request)) (= "pending" request))  [:span.label.label-info.endorsement-info-label [:i.fa.fa-hand-o-left] (t :badge/requestsent)]
    :else ""))

(defn- profile-grid-element [profile selected-users-atom type]
  (let [{:keys [id profile_picture first_name last_name common_badge_count endorsement]} profile
        pickable? (and (= "pickable" type) (every? nil? (vals endorsement)))
        current-user (session/get-in [:user :id])]
    (fn []
      [:div.col-md-4.col-sm-6.col-xs-12
       [:div.panel.panel-default.endorsement.profile-element
        [:div.panel-body {:style {:padding "4px"}}
         [endorsement-info-label endorsement]
         (when pickable? [:span.checkbox [:input {:aria-label (str "select user" first_name " " last_name) :type "checkbox" :on-change #(add-or-remove profile selected-users-atom) :checked (boolean (some #(= id (:id %)) @selected-users-atom))}]])
         [:div.row.flip.settings-endorsement
          [:div.col-md-12.media; {:style {:margin-top "2px"}}
           [:div.media-left
            [:img {:src (profile-picture profile_picture) :alt (str first_name " " last_name)}]]
           [:div.media-body
            [:a {:href "#"
                 :on-click #(mo/open-modal [:profile :view] {:user-id id})}
             (str first_name " " last_name)]
            #_[:div.common-badges
               (if (= id current-user)
                 (t :gallery/ownprofile)
                 [:span common_badge_count " " (if (= common_badge_count 1)
                                                 (t :gallery/commonbadge) (t :gallery/commonbadges))])]]]]]]])))

(defn allprofilesmodal [params]
  (let [country (session/get-in [:user :country] "all")
        filter-options (session/get :filter-options nil)

        common-badges? (if filter-options (:common-badges filter-options) true)
        {:keys [type selected-users-atom context user_badge_id selfie func]} params
        data-atom (atom {:users []
                         :selected []
                         :ajax-message nil
                         :name ""
                         :order_by "ctime"
                         :common-badges? common-badges?
                         :country-selected (session/get-in [:filter-options :country] country)
                         :user_badge_id user_badge_id
                         :context context
                         :url (if (= context "endorsement")
                               (str "/obpv1/gallery/profiles/" user_badge_id "/" context)
                               (str "/obpv1/gallery/profiles"))
                         :sent-requests []})]
    (create-class {:reagent-render (fn []
                                     [:div
                                      [:div {:id "social-tab"}
                                       [profiles/profile-gallery-grid-form data-atom true]
                                       (if (:ajax-message @data-atom)
                                         [:div.ajax-message
                                          [:i {:class "fa fa-cog fa-spin fa-2x "}]
                                          [:span (:ajax-message @data-atom)]]
                                         [:div#endorsebadge
                                          (conj
                                           [:div
                                            (reduce (fn [r user]
                                                      (conj r [profile-grid-element user selected-users-atom type]))
                                                    [:div.col-md-12.profilescontainer
                                                     (when (or (= "endorsement_selfie" context)(= "endorsement" context))
                                                       [:div.col-md-12 {:style {:font-weight "bold"}}
                                                        [:hr.line]
                                                        [:p (t :badge/Requestendorsementmodalinfo)]
                                                        [:hr.line]])

                                                     (when (= "selfie_issue" context)
                                                       [:div.col-md-12 {:style {:font-weight "bold"}}
                                                        [:hr.line]
                                                        [:p (t :badgeIssuer/Issueselfiebadgeinfo)]
                                                        [:hr.line]])]

                                                    (if (or (= context "endorsement") (= context "endorsement_selfie"))
                                                     (->> @(cursor data-atom [:users])
                                                           (remove #(= (:id %) (session/get-in [:user :id])))
                                                           (filter #(every? nil? (-> % :endorsement vals))))
                                                     @(cursor data-atom [:users])))]

                                           [:div.col-md-12.confirmusers {:style {:margin "10px auto"}}
                                            (when (or (= context "endorsement") (= context "endorsement_selfie"))
                                             [:button.btn.btn-primary {:on-click #(mo/previous-view)
                                                                       :disabled (empty? @selected-users-atom)}
                                                 (t :core/Continue)])
                                            (when (= context "selfie_issue")
                                             [:div
                                              [:button.btn.btn-primary.btn-bulky
                                               {:on-click #(mo/previous-view) #_(func)
                                                :disabled (empty? @selected-users-atom)}
                                               (t :core/Continue)
                                               #_[:span [:i.fa.fa-lg.fa-paper-plane] (t :badgeIssuer/Issuebadge)]]
                                              [:button.btn.btn-danger.btn-bulky
                                               {:on-click #(do (reset! selected-users-atom []) (mo/previous-view))}
                                               (t :core/Cancel)]])])])]])


                   :component-will-mount (fn []
                                           (ajax/POST
                                            (if (= context "endorsement")
                                              (path-for (str "/obpv1/gallery/profiles/" user_badge_id "/" context))
                                              (path-for (str "/obpv1/gallery/profiles")))
                                            {:params {:country (session/get-in [:filter-options :country] country)
                                                      :name ""
                                                      :common_badges common-badges?
                                                      :order_by "ctime"}
                                             :handler (fn [{:keys [users countries]} data]
                                                        (swap! data-atom assoc :users users
                                                               :countries countries
                                                               :country-selected (session/get-in [:filter-options :country] country)))
                                             :finally (fn []
                                                        (swap! data-atom assoc :ajax-message nil))}))})))
