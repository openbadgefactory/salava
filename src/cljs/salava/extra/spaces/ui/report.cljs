(ns salava.extra.spaces.ui.report
 (:require
  [reagent.core :refer [atom cursor]]
  [reagent.session :as session]
  [salava.core.ui.layout :as layout]
  [salava.core.ui.helper :refer [path-for plugin-fun js-navigate-to]]
  [salava.core.i18n :refer [t]]
  [salava.core.ui.modal :as mo]
  [reagent-modals.modals :as m]
  [salava.gallery.ui.badges :as gallery]
  [salava.core.ui.grid :as g]
  [salava.core.ui.ajax-utils :as ajax]
  [salava.user.ui.helper :refer [profile-picture]]
  [salava.core.time :refer [iso8601-to-unix-time date-from-unix-time]]))
  ;[salava.core.ui.badge-grid :refer [badge-grid-element]]))

(defn fetch-more [state]
  (let [{:keys [users badges from to space-id]} @(cursor state [:filters])
        page-count-atom (cursor state [:filters :page_count])]
   (ajax/POST
    (path-for (str "/obpv1/space/report") true)
    {:params {:users (mapv :id users)
              :badges (mapv :gallery_id badges)
              :to (if (number? to) to nil)
              :from (if (number? from) from nil)
              :space-id space-id
              :page_count @page-count-atom}
              ;:space-id space-id}
     :handler (fn [data]
                (swap! state assoc-in [:results :users] (into @(cursor state [:results :users]) (:users data)))
                (swap! page-count-atom inc)
                (swap! state assoc-in [:results :user_count] (:user_count data)))
     :finally (fn [] (reset! (cursor state [:fetching-more]) false))})))

(defn fetch-report [state]
 (let [{:keys [users badges from to space-id]} @(cursor state [:filters])
       page-count-atom (cursor state [:filters :page_count])]
  (reset! page-count-atom 0)
  (reset! (cursor state [:fetching]) true)
  (reset! (cursor state [:preview]) false)
  (reset! (cursor state [:results]) {})
  (ajax/POST
   (path-for (str "/obpv1/space/report") true)
   {:params {:users (mapv :id users)
             :badges (mapv :gallery_id badges)
             :to (if (number? to) to nil)
             :from (if (number? from) from nil)
             :space-id space-id
             :page_count 0}
    :handler (fn [data]
               (reset! (cursor state [:results]) data)
               (swap! page-count-atom inc)
               (swap! state assoc-in [:results :user_count] (:user_count data)))

    :finally (fn [] (reset! (cursor state [:fetching]) false))})))

(defn export-report [state]
  (let [{:keys [users badges to from space-id]} @(cursor state [:filters])
        users (mapv :id users)
        badges (mapv :gallery_id badges)
        to (if (number? to) to nil)
        from (if (number? from) from nil)
        url (str "/obpv1/space/report/export/" space-id "?users="users "&badges="badges "&to="to "&from="from "&space_id="space-id)]
    (js-navigate-to url)))

(defn query-params [base]
  {:country (get base :country "")
   :tags (get base :tags "")
   :badge-name (get base :badge-name "")
   :issuer-name (get base :issuer-name "")
   :order (get base :order "mtime")
   :recipient-name (get base :recipient-name "")
   :page_count 0
   :only-selfie? false
   :space-id (session/get-in [:user :current-space :id] 0)
   :fetch-private true})


(defn ajax-stop [ajax-message-atom]
  (reset! ajax-message-atom nil))

(defn init-badges [params state]
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
                   (init-badges (assoc params :country "all") state) ;;Recall init data with "all" countries if initial query returned empty coll
                   (do
                     (reset! (cursor state [:params :page_count]) 1)
                     (swap! state assoc
                            :badges badges
                            :badge_count badge_count)))))
    :finally (fn []
               (ajax-stop (cursor state [:ajax-message]))
               (fetch-report state))}))

(defn init-data [params state]
  (init-badges params state))


(defn- add-or-remove [x coll]
   (if (some #(= x %) @coll)
     (reset! coll (->> @coll (remove #(= x %)) vec))
     (reset! coll (conj @coll x))))

(defn badge-grid-element [badge state]
 (let [{:keys [recipients image_file name id selfie_id badge_id gallery_id issuer_content_name description]} badge
       badge-filters (cursor state [:filters :badges])]
  [:div {:class "media grid-container" :style {:position "relative"}}
   [:input.pull-right
    {:id (str "checkbox-"gallery_id)
     :type "checkbox"
     :name (str "input-checkbox-"gallery_id)
     ;:value (some #(= gallery_id (:gallery_id %))  @badge-filters)
     :checked (if (some #(= gallery_id (:gallery_id %))  @badge-filters) true false)
     :on-change #(add-or-remove badge badge-filters)}]
   [:a {:href "#"
        :on-click #(do
                     (.preventDefault %)
                     (mo/open-modal [:gallery :badges] {:badge-id badge_id :gallery-id gallery_id} {:hidden (fn [] (reset! (cursor state [:select-all]) false))}))

        :title name}
    [:div.media-content
     (if image_file
       [:div.media-left
        [:img {:src (str "/" image_file)
               :alt (str (t :badge/Badge) " " name)}]])
     [:div.media-body
      [:div.media-heading
       [:p.heading-link name]]
      [:div.media-issuer
       [:p (when-not (clojure.string/blank? selfie_id)
            [:i.fa.fa-user.fa-fw.fa-lg {:title (str (t :badgeIssuer/Createdandissued) " " (session/get :site-name))
                                        :aria-label (str (t :badgeIssuer/Createdandissued) " " (session/get :site-name))}])
        issuer_content_name]]]]]]))



(defn gallery-grid-form [state]
  (let [show-advanced-search (cursor state [:advanced-search])]
    [:div {:id "grid-filter"
           :class "form-horizontal"}
     [:div
      #_[gallery/country-selector state]
      [:div
       [:a {:on-click #(reset! show-advanced-search (not @show-advanced-search))
            :href "#"}
        (if @show-advanced-search
          (t :gallery/Hideadvancedsearch)
          (t :gallery/Showadvancedsearch))]]
      (when @show-advanced-search
        [:div
         [gallery/autocomplete state]
         [gallery/text-field :badge-name (t :gallery/Badgename) (t :gallery/Searchbybadgename) state]
         [gallery/text-field :recipient-name (t :gallery/Recipient) (t :gallery/Searchbyrecipient) state]
         [gallery/text-field :issuer-name (t :gallery/Issuer) (t :gallery/Searchbyissuer) state]])]
     [g/grid-radio-buttons (str (t :core/Order) ":") "order" (gallery/order-radio-values) [:params :order] state gallery/fetch-badges]
     (into [:div]
      (for [f (plugin-fun (session/get :plugins) "block" "gallery_checkbox")]
       (when (ifn? f) [f state (fn [] (gallery/fetch-badges state))])))]))

(defn load-more [state]
  (if (pos? @(cursor state [:results :user_count]))
     [:div {:style {:margin "10px 0"}}
      (if @(cursor state [:fetching-more])
        [:span [:i.fa.fa-cog.fa-spin.fa-lg.fa-fw] (str (t :core/Loading) "...")]
        [:span [:a {:href     "#"
                    :id    "loadmore"
                    :on-click #(do
                                 (reset! (cursor state [:fetching-more]) true)
                                 (fetch-more state)
                                 ;(init-data state)
                                 (.preventDefault %))}

                 (str (t :social/Loadmore) " (" @(cursor state [:results :user_count])) " " (t :admin/rowleft) ")"]])]))

(defn gallery-grid [state]
  (let [badges (:badges @state)]
    [:div#badges (into [:div {:class "row wrap-grid"
                              :id    "grid"}]
                       (for [element-data badges]
                         (badge-grid-element element-data state))) ;"pickable" gallery/fetch-badges)))
     (gallery/load-more state)]))

(defn badges-modal [state]
 (fn []
  [:div#badge-gallery
   [:div.col-md-12
    [gallery-grid-form state]
    [:div {:style {:background-color "ghostwhite" :margin "10px auto" :padding "8px"}}
       ;[:p [:b (t :admin/selectallbadgesinstruction)]]
       [:label
         [:input
          {:style {:margin "0 5px"}
           :type "checkbox"
           :default-checked @(cursor state [:select-all-badges])
           :on-change #(do
                         (reset! (cursor state [:filters :badges]) [])
                         (reset! (cursor state [:select-all-badges]) (not @(cursor state [:select-all-badges])))
                         (when @(cursor state [:select-all-badges])
                           (reset! (cursor state [:filters :badges])  (:badges @state))))}]
           ;:disabled (pos? @(cursor state [:badge_count]))}]
         [:b (t :admin/Selectallvisiblebadges)]]]

    (if (:ajax-message @state)
      [:div.ajax-message
       [:i {:class "fa fa-cog fa-spin fa-2x "}]
       [:span (:ajax-message @state)]]
      [gallery-grid state])

    [:div.well.well-sm.text-center
     [:button.btn.btn-primary.btn-bulky
      {:aria-label (t :core/Continue) :data-dismiss "modal"}
      (t :core/Continue)]]]]))


(defn badge-filter-section [state]
 (let [badge-filters (cursor state [:filters :badges])]
  [:div.badge-section.col-md-6
   [:a
    {:href "#"
     :on-click #(do
                  (.preventDefault %)
                  (mo/open-modal [:space :badges] state {:hidden (fn [] (fetch-report state))}))
     :aria-label "add badge"}

    [:span [:i.fa.fa-fw.fa-certificate.fa-lg] (t :admin/Addbadge)]]
   (when (seq @badge-filters)
    [:div.row
     [:div.col-md-12
      [:a.pull-right {:href "#" :on-click #(do
                                             (reset! (cursor state [:select-all-badges]) false)
                                             (reset! (cursor state [:filters :badges]) []) :role "button"
                                             (fetch-report state))}
        [:b (t :social/clearall)]]]
     [:div#admin-report.col-md-12 {:style {:max-height "500px" :overflow "auto" :margin "10px auto"}}
      (reduce
        #(conj %1
          ^{:key (:gallery_id %2)}[:a.list-group-item
                                   [:div.inline
                                    [:button.close
                                     {:type "button"
                                      :aria-label (t :core/Delete)
                                      :on-click (fn []
                                                  (reset! badge-filters (remove (fn [b] (= (:gallery_id b) (:gallery_id %2))) @badge-filters))
                                                  (fetch-report state))}
                                     [:span {:aria-hidden "true"
                                             :dangerouslySetInnerHTML {:__html "&times;"}}]]
                                    [:img.logo {:src (str "/" (:image_file %2))}]
                                    [:span.name (:name %2)]]])

        [:div.list-group]
        @badge-filters)]])]))

(defn user-filter-section [state]
  (let [user-filters (cursor state [:filters :users])]
    [:div.user-section.col-md-6
     [:a
      {:href "#"
       :on-click #(do
                    (.preventDefault %)
                    (mo/open-modal [:gallery :profiles] {:space (session/get-in [:user :current-space :id] 0)
                                                         :type "pickable"
                                                         :selected-users-atom user-filters
                                                         :context "report_space"} {:hidden (fn [] (fetch-report state))}))
       :aria-label "add user"}

      [:span [:i.fa.fa-fw.fa-user.fa-lg] (t :admin/Adduser)]]
     (when (seq @user-filters)
      [:div.row
       [:div.col-md-12
        [:a.pull-right {:href "#" :on-click #(do
                                               ;(reset! (cursor state [:select-all]) false)
                                               (reset! (cursor state [:filters :users]) []) :role "button"
                                               (fetch-report state))}
          [:b (t :social/clearall)]]]
       [:div#admin-report.col-md-12 {:style {:max-height "500px" :overflow "auto" :margin "10px auto"}}
        (reduce
          #(conj %1
            ^{:key (:gallery_id %2)}[:a.list-group-item
                                     [:div.inline
                                      [:button.close
                                       {:type "button"
                                        :aria-label (t :core/Delete)
                                        :on-click (fn []
                                                    (reset! user-filters (remove (fn [u] (= (:id u) (:id %2))) @user-filters))
                                                    (fetch-report state))}
                                       [:span {:aria-hidden "true"
                                               :dangerouslySetInnerHTML {:__html "&times;"}}]]
                                      [:img.logo {:src (profile-picture (:profile_picture %2)) :alt (str (:first_name %2) " " (:last_name %2))}]
                                      [:span.name (str (:first_name %2) " " (:last_name %2))]]])
          [:div.list-group]
          @user-filters)]])]))

(defn user-list [state]
  (let [results (cursor state [:results :users])]
    (when (seq @results)
      [:div.panel.panel-default
       [:div.panel-heading
         [:div.row
          [:div.col-md-12
            [:div.col-md-6.panel-title
             (str (t :admin/Results) " (" (count @results) "/" @(cursor state [:results :total]) ")")]
            [:div.col-md-6
              [:a.pull-right {:href "#"
                              :role "button"
                              :on-click #(reset! (cursor state [:find]) "badges")
                              :class (if (= "badges" @(cursor state [:find])) "btn-primary")}
                 [:span [:i.fa-certificate.fa.fa-lg.fa-fw] (t :admin/Findbadges)]]]]]]
       [:div.table-responsive {:style {:max-height "500px" :overflow "auto"}}
        [:table.table
          [:thead
           [:tr
            [:th (t :admin/user)]
            (when (some #(= % "gender") (map :name (session/get :custom-fields nil)))
             [:th (t :admin/gender)])
            (when (some #(= % "organization") (map :name (session/get :custom-fields nil)))
              [:th (t :admin/organization)])
            [:th (t :admin/emailaddresses)]
            [:th (t :admin/noofbadges)]
            [:th (t :admin/sharedbadges)]
            [:th (str (t :admin/completionPercentage) " (%)")]
            [:th (t :gallery/Joined)]]]

         (reduce
          (fn [r u]
           (let [{:keys [badgecount sharedbadges profile_visibility name activated profile_picture ctime completionPercentage gender organization emailaddresses]} u]
            (conj r
             [:tr.table-item
              [:td
               [:div.inline-flex
                [:img.logo {:src (profile-picture profile_picture) :alt name}]
                [:span.name name]]]
              (when (some #(= % "gender") (map :name (session/get :custom-fields nil)))
                [:td gender])
              (when (some #(= % "organization") (map :name (session/get :custom-fields nil)))
               [:td organization])
              [:td  (reduce #(conj %1 [:li {:style {:list-style "none"}} %2] ) [:div] (clojure.string/split emailaddresses #","))]
              [:td  (str badgecount)] ;" " (if (> badgecount 1) (t :badge/Badges) (t :badge/Badge)))]
              [:td  (str sharedbadges)] ;" " (if (> sharedbadges 1) (t :badge/Badges) (t :badge/Badge)))]
              [:td
               (str completionPercentage "%")
               #_[:div.progress

                  [:div.progress-bar.progress-bar-success
                   {:role "progressbar"
                    :aria-valuenow (str completionPercentage)
                    :aria-valuemin "0"
                    :aria-valuemax "100"
                    :style {:width (str completionPercentage "%")}}
                   [:span (str completionPercentage "% ")]]]]
              [:td (date-from-unix-time (* 1000 ctime))]])))

          [:tbody]

          @results)]]])))

(defn badge-list [state]
  (let [results (remove #(zero? (count (:badges %))) @(cursor state [:results :users])) #_(cursor state [:results :users])
        user-filter (cursor state [:filters :users])]
     (if (seq results)
      [:div {:style {:max-height "500px" :overflow "auto"}}
       [:div.panel.panel-default
        [:div.panel-heading
          [:div.row
           [:div.col-md-12
             [:div.col-md-6.panel-title
              (str (t :admin/Results) " (" (count results) ")")]
             [:div.col-md-6
               [:a.pull-right {:href "#"
                               :role "button"
                               :on-click #(reset! (cursor state [:find]) "users")
                               :class (if (= "users" @(cursor state [:find])) "btn-primary")}
                  [:span [:i.fa.fa-users.fa-fw.fa-lg] (t :admin/Showuserlist)]]]]]]
        (reduce
         (fn [r user]
          (let [{:keys [name profile_picture badge_count badges]} user]

           (conj r
            [:div.panel.panel-default
             [:div.panel.panel-heading
              [:div.table-item
               [:img.logo {:src (profile-picture profile_picture) :alt name}]
               [:span.name name]]]
             [:div.table-responsive
              [:table.table
               [:thead
                [:tr
                 [:th (t :admin/id)]
                 [:th (t :admin/name)]
                 [:th (t :user/Status)]
                 [:th (t :admin/badgeVisibility)]
                 [:th (t :badge/Issuedon)]
                 [:th (t :badge/Expireson)]]]
               (reduce
                (fn [v b]
                 (let [{:keys [badge_name badge_image status visibility id issued_on expires_on]} b]
                  (conj v
                   [:tr.table-item
                    [:td id]
                    [:td  [:img.logo {:src (profile-picture badge_image) :alt badge_name}]
                          [:span.name badge_name]]
                    [:td status]
                    [:td visibility]
                    [:td (when issued_on (date-from-unix-time (* 1000 issued_on)))]
                    [:td (when expires_on (date-from-unix-time (* 1000 expires_on)))]])))
                [:tbody]
                badges)]]])))
         [:div.panel-body]
         results)]])))

(defn clear-selected-dates [state]
  (reset! (cursor state [:to]) nil)
  (reset! (cursor state [:from]) nil)
  (reset! (cursor state [:filters :to]) nil)
  (reset! (cursor state [:filters :from]) nil))

(defn content [state]
  [:div#admin-report
   [m/modal-window]
   [:div.panel.panel-default
    [:div.panel-heading
     [:div.row
      [:div.col-md-6
       [:div.panel-title.weighted (t :admin/Reportbuilder)]]]]
    [:div.panel-body
     [:div.row
      [badge-filter-section state]
      [user-filter-section state]]
     [:div.row
      [:div
       [:div.col-md-9.mg10vt
        [:div.form-group {:style {:padding "5px"}}
         [:span._label (t :admin/Issuingdatebtw)]
         [:div.form-inline
          [:input#from.form-control
           {:type "date"
            :value @(cursor state [:from])
            :on-change #(do
                          (.preventDefault %)
                          (reset! (cursor state [:from]) (.-target.value %))
                          (reset! (cursor state [:filters :from]) (iso8601-to-unix-time @(cursor state [:from])))
                          (fetch-report state))}]
          [:span.mg10hr [:b "-"]]
          [:input#to.form-control
           {:type "date"
            :value @(cursor state [:to])
            :on-change #(do
                          (.preventDefault %)
                          (reset! (cursor state [:to]) (.-target.value %))
                          (reset! (cursor state [:filters :to]) (iso8601-to-unix-time @(cursor state [:to])))
                          (fetch-report state))}]
          [:button.btn.btn-primary.mg10hr
            {:on-click #(do
                          (clear-selected-dates state)
                          (fetch-report state))}
            (t :admin/Clear)]]]]]]]]
   [:div.btn-toolbar
    [:div.btn-group
     [:button.btn-primary.btn.btn-bulky
      {:on-click #(reset! (cursor state [:preview]) true)
       :aria-label (t :admin/Preview)
       :disabled (empty? @(cursor state [:results :users]))}
      (t :admin/Preview)]
     [:button.btn-primary.btn.btn-bulky
      {:on-click #(export-report state)
       :aria-label (t :admin/DownloadCSV)
       :disabled (empty? @(cursor state [:results :users]))}
      (t :admin/DownloadCSV)]]]

   [:div {:style {:margin "10px auto"}}
    (if @(cursor state [:fetching])
      [:div
        [:span [:i.fa.fa-lg.fa-cog.fa-spin] (str " " (t :core/Loading) " ...")]]
      [:div #_(if (= "badges" @(cursor state [:find]))
                  [:b (str (count (remove #(zero? (count (:badges %))) @(cursor state [:results :users]))) " " (t :admin/rowsfound))]
                  [:b (str @(cursor state [:results :total])) " " (t :admin/rowsfound)])
        [:b (str @(cursor state [:results :total])) " " (t :admin/rowsfound)]])]

   (if @(cursor state [:preview])
     [:div
      (when (= "users" @(cursor state [:find]))
       [user-list state])
      (when (= "badges" @(cursor state [:find]))
       [badge-list state])
      (load-more state)])])
      ;(when (= "users" @(cursor state [:find])) (load-more state))])])


(defn handler [site-navi]
  (let [params (query-params {:country (session/get-in [:filter-options :country] "all")})
        state (atom {:filters {:badges []
                               :users []
                               :from nil
                               :to nil
                               :space-id (session/get-in [:user :current-space :id] 0)}
                     :params params
                     :badges []
                     :countries              []
                     :country-selected       (session/get-in [:filter-options :country] "all")
                     :autocomplete           {:tags {:value #{} :items #{}}}
                     :advanced-search        true
                     :timer                  nil
                     :ajax-message           nil
                     :only-selfie? false
                     :badge_count            0
                     :results {:total 0}
                     :find "users"
                     :to nil
                     :from nil
                     :preview false
                     :select-all-badges false
                     :fetching false
                     :fetching-more false})]

   (init-data params state)
   (fn []
     (layout/default site-navi [content state]))))
