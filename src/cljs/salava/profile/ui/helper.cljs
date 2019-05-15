(ns salava.profile.ui.helper
  (:require [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for hyperlink private? not-activated? plugin-fun navigate-to private?]]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.badge-grid :refer [badge-grid-element]]
            [salava.core.ui.page-grid :refer [page-grid-element]]
            [salava.core.ui.share :as s]
            [reagent.session :as session]
            [reagent.core :refer [cursor atom create-class]]
            [salava.admin.ui.admintool :refer [admintool]]
            [salava.core.ui.field :as f]
            [salava.core.ui.modal :as mo :refer [open-modal]]
            [reagent-modals.modals :as m]
            [salava.profile.ui.edit :as pe]
            [salava.page.ui.helper :refer [view-page]]
            [salava.core.ui.popover :refer [info]]))



(defn toggle-visibility [visibility-atom]
  (ajax/POST
    (path-for "/obpv1/user/profile/set_visibility")
    {:params  {:visibility (if (= "internal" @visibility-atom) "public" "internal")}
     :handler (fn [new-value]
                (reset! visibility-atom new-value))}))


(defn profile-visibility-input [visibility-atom state]
  (if @(cursor state [:edit-mode]) [:div.col-xs-12
                                    [:div.checkbox
                                     [:label
                                      [:input {:name      "visibility"
                                               :type      "checkbox"
                                               :on-change #(toggle-visibility visibility-atom)
                                               :checked   (= "public" @visibility-atom)}]
                                      (t :user/Publishandshare)]]]
    [:div.visibility
     [:label (t :user/Profilevisibility) ": " @visibility-atom]]))

(def block-type-map
 [{:icon "fa-certificate" :icon-2 "fa-th-large" :text (t :page/Badgeshowcase) :value "showcase"}])


(defn contenttype [{:keys [block-atom index]}]
    (let [block-type-map (if (some #(= "profile" (:type %)) @block-atom)
                           (into []  (remove #(= "profile" (:value %)) block-type-map)) block-type-map)]
      (fn []
        [:div#page-edit
         [:div#block-modal
          [:div.modal-body
           [:p.block-title (t :page/Addblock)]
           [:p (t :page/Choosecontent)]
           (reduce-kv
             (fn [r _ v]
               (let [new-field-atom (atom {:type (:value v)})]
                 (conj r
                       [:div.row
                        [:div.col-md-12
                         [:div.content-type {:style {:display "inline-table"}}
                          [:a.link {:on-click #(do
                                                  (.preventDefault %)
                                                  (if index
                                                    (f/add-field block-atom {:type (:value v)} index)
                                                    (f/add-field block-atom {:type (:value v)})))
                                               :data-dismiss "modal"}

                                   [:div
                                    [:i {:class (str "fa fa-fw " (:icon v))}]
                                    (when (:icon-2 v) [:i.fa.fa-fw {:class (:icon-2 v)}])
                                    [:span (:text v)]]]]
                         [:span {:style {:display "inline"}}
                          [info {:placement "right" :content (case (:value v)
                                                               "showcase" (t :page/Badgeshowcaseinfo)
                                                              nil)
                                 :style {:font-size "15px"}}]]]])))

             [:div.block-types] block-type-map)]
          [:div.modal-footer
           [:button.btn.btn-warning {:on-click #(do
                                                  (.preventDefault %)
                                                  (m/close-modal!))}
            (t :core/Cancel)]]]])))



(defn field-after [blocks state index initial?]
  (let [ first? (= 0 index)
         last? (= (dec (count @blocks)) index)
         block-count (count @blocks)]
    (fn []
      [:div.add-field-after
       (if (and (:toggle-move-mode @state) (not (= index (:toggled @state))))
         [:a {:href "#" :on-click #(do
                                     (f/move-field-drop blocks (:toggled @state) index)
                                     (swap! state assoc :toggle-move-mode false :toggled nil))}
          [:div.placeholder.html-block-content.html-block-content-hover
           (t :page/Clicktodrop)]]
         [:button {:class    "btn btn-success"
                   :on-click #(do
                                (.preventDefault %)
                                (if index
                                 (open-modal [:profile :blocktype] {:block-atom blocks :index index} {:size :md})
                                 (open-modal [:profile :blocktype] {:block-atom blocks :index nil})))}
          (t :page/Addblock)])])))

(defn recent-badges [state]
  (let [block (first (plugin-fun (session/get :plugins) "block" "recentbadges"))]
    (if block [block state] [:div ""])))

(defn recent-pages [state]
  (let [block (first (plugin-fun (session/get :plugins) "block" "recentpages"))]
    (if block [block state] [:div ""])))

(defn update-block-value [block-atom key value]
  (swap! block-atom assoc key value))

(defn showcase-grid [state block-atom]
 [:div#user-badges
  [:h3 (or (:title @block-atom) (t :page/Untitled))]
  [:div#grid {:class "row"}
   (reduce (fn [r b]
            (conj r (badge-grid-element b state "profile" nil))) [:div] (:badges @block-atom))]])

(defn badge-showcase [state block-atom]
  (let [badges (if (seq (:badges @block-atom)) (:badges @block-atom) [])
        new-field-atom (atom {:type "showcase" :badges badges})
        title (:title @block-atom)
        format (:format @block-atom)]
    [:div#badge-showcase
     [:div#grid {:class "row"}
      [:h3 {:style {:padding-bottom "10px"}} (t :page/Badgeshowcase)]
      [:div.form-group
       [:div
        [:label (t :page/Title)]

        [:input {:class     "form-control"
                 :type      "text"
                 :value     title
                 :default-value (t :page/Untitled)
                 :on-change #(update-block-value block-atom :title (.-target.value %))
                 :placeholder (t :page/Untitled)}]]
       [:div
        [:label (t :page/Displayinpageas)]
        [:div.badge-select
         [:select {:class "form-control"
                   :aria-label "select badge format"
                   :value (or format "short")
                   :on-change #(update-block-value block-atom :format (.-target.value %))}
          [:option {:value "short"} (t :core/Imageonly)]
          [:option {:value "long"} (t :page/Content)]]]]]
      (reduce (fn [r b]
                (conj r
                      (badge-grid-element b block-atom "showcase" {:delete! (fn [id badges] (update-block-value block-atom :badges (into [] (remove #(= id (:id %)) badges))))
                                                                   :swap! (fn [index data badges] (update-block-value block-atom :badges (into [] (assoc badges index data))))})))
              [:div]
              badges)
      [:div.addbadge
       [:a {:href "#" :on-click #(do
                                   (.preventDefault %)
                                  (open-modal [:badge :my] {:type "pickable" :block-atom block-atom :new-field-atom new-field-atom
                                                            :function (fn [f] (update-block-value block-atom :badges (conj badges f)))}))}
        [:i.fa.fa-plus.add-icon]]]]]))

(defn block-for-edit [block-atom state index]
  (let [block-toggled? (and (:toggle-move-mode @state) (= (:toggled @state) index))
        type (:type @block-atom)
        blocks (cursor state [:blocks])
        visibility-atom (cursor block-atom [:hidden])
        first? (= 0 index)
        last? (= (dec (count @blocks)) index)]
    [:div {:key index}
     [field-after blocks state index]
     [:div.field.thumbnail {:class (when block-toggled? " block-to-move")}
      [:div.field-content
       [:div.form-group
        (case type
         ("badges" "pages")[:div.checkbox
                             [:label [:input {:type "checkbox"
                                              :value @visibility-atom
                                              :on-change #(do
                                                            (.preventDefault %)
                                                            (update-block-value block-atom :hidden (not @visibility-atom)))
                                              :checked (= "true" (str @visibility-atom))}] (t :profile/Hideinprofile)]]
         nil)
        [:div.move {:on-click #(do
                                 (.preventDefault %)
                                 (cond
                                   (and first? last?) (swap! state assoc :toggle-move-mode false :toggled nil)
                                   (:toggle-move-mode @state) (swap! state assoc :toggle-move-mode false :toggled nil)
                                   :else (swap! state assoc :toggle-move-mode true :toggled index)))}
         [:span.move-block {:class (when block-toggled? " block-to-move")}  [:i.fa.fa-arrows]]]
        (case type
          ("showcase") [:div {:class "close-button"
                              :on-click #(f/remove-field blocks index)}
                        [:span {:class "remove-button" :title (t :page/Delete)}
                         [:i {:class "fa fa-trash"}]]]
          nil)]
       (case type
         ("badges") [recent-badges state]
         ("pages") [recent-pages state]
         ("showcase") [badge-showcase state block-atom]
         ("location") [:div
                        [:h3 {:style {:padding-bottom "10px"}} (t :location/Location)]
                       (into [:div {:style {:margin-top "15px" :padding-top "15px"}}]
                             (for [f (plugin-fun (session/get :plugins) "block" "user_profile")]
                               [f (:user-id @state)]))]
         nil)]]]))

(defn block [block-atom state index]
  (let [type (:type @block-atom)]
    (when-not (:hidden @block-atom)
     [:div {:key index} (case type
                          ("badges") [recent-badges state]
                          ("pages") [recent-pages state]
                          ("showcase") [showcase-grid state block-atom]
                          ("location") (into [:div]
                                        (for [f (plugin-fun (session/get :plugins) "block" "user_profile")]
                                          [f (:user-id @state)]))

                         nil)])))



(defn connect-user [user-id]
  (let [connectuser (first (plugin-fun (session/get :plugins) "block" "connectuser"))]
    (if connectuser
      [connectuser user-id]
      [:div ""])))

(defn edit-page-navi [target state]
  (let [active-tab-atom (cursor state [:edit :active-tab])
        user-id (:user-id @state)]
    [:div {:class "row flip"
           :id "buttons"}
     [:div.col-xs-12
      [:div.col-xs-12.wizard
       [:a {:class (if (= target :content) "current")
            :href "#"
            :on-click #(do
                         (.preventDefault %)
                         (reset! active-tab-atom :content))}
        [:span {:class (str "badge" (if (= target :content) " badge-inverse" ))} "1."]
        (t :page/Content)]
       [:a {:class (if (= target :theme) "current")
            :href "#"
            :on-click #(do
                        (.preventDefault %)
                        (reset! active-tab-atom :theme))}

        [:span {:class (str "badge" (if (= target :theme) " badge-inverse" ))} "2."]
        (t :page/Theme)]
       (when-not (private?)
         [:a {:class (if (= target :settings) "current")
              :href "#"
              :on-click #(do
                          (.preventDefault %)
                          (reset! active-tab-atom :settings))}
          [:span {:class (str "badge" (if (= target :settings) " badge-inverse" ))} "3."]
          (t :page/Settings)])
       [:a {:class (if (= target :preview) "current")
            :href "#"
            :on-click #(do (.preventDefault %)
                         (reset! active-tab-atom :preview))}

        [:span {:class (str "badge" (if (= target :preview) " badge-inverse" ))} "4."]
        (t  :page/Preview)]]
      #_[:div {:class "col-xs-4"
               :id "buttons-right"}
         [:a.btn.btn-primary {:href "#"
                              :on-click #(do
                                           (navigate-to (str "/profile/" user-id))
                                           (.preventDefault %)
                                           (reset! (cursor state [:edit-mode]) false))}

                             [:i.fa.fa-eye.fa-lg.fa-fw](t :page/View)]]
      [m/modal-window]]]))

(defn manage-buttons [state]
  (let [visibility-atom (cursor state [:user :profile_visibility])
        user-id (:user-id @state)
        owner? (cursor state [:owner?])
        link-or-embed-atom (cursor state [:user :show-link-or-embed-code])
        fullname (str @(cursor state [:user :first_name]) " " @(cursor state [:user :last_name]))]
    (if @owner?
      [:div.manage-buttons.row;.inline
       (if-not @(cursor state [:edit-mode])
         [:div.col-xs-12
          (if-not (or (not-activated?) (private?))
            (profile-visibility-input visibility-atom state))
          [:div.share
           [s/share-buttons (str (session/get :site-url) (path-for "/profile/") user-id) fullname (= "public" @visibility-atom) false link-or-embed-atom]]
          [:div.edit-btn
            (if-not (not-activated?)
               [:a.btn.btn-primary {:href "#"
                                    :on-click #(do
                                                 (.preventDefault %)
                                                 (reset! (cursor state [:edit-mode]) true))}


                                   [:i.fa.fa-pencil-square-o.fa-lg.fa-fw](t :user/Editprofile)])]]

         [edit-page-navi @(cursor state [:edit :active-tab]) state])]

      [:div
       (connect-user user-id)
       (admintool user-id "user")])))

(defn add-page [state]
   (when (:edit-mode @state) [:li.nav-item.dropdown.addnew [:a.dropdown-toggle {:data-toggle "dropdown"} [:i.fa.fa-plus-square]]
                                [:ul.dropdown-menu
                                 [:li [:a {:href "#" :on-click #(do
                                                                 (.preventDefault %)
                                                                 (mo/open-modal [:page :my] {:tab-atom (cursor state [:tabs])} {:hidden (fn [] (pe/save-profile state nil false))}))}

                                       (t :profile/Addexistingpage)]]
                                 [:li [:a {:href "#" :on-click #(do
                                                                 (.preventDefault %)
                                                                 (as-> (first (plugin-fun (session/get :plugins) "my" "create_page")) f (when f (f)) ))} (t :profile/Createnewpage)]]]]))

(defn page-content [page-id page state]
 (let [index (.indexOf (map :id @(cursor state [:tabs])) page-id)]
  [:div#profile
   [:div {:style {:text-align "center" :margin "15px 0"}}
    (when @(cursor state [:edit-mode])
     [:div
      (when-not (= page-id (some->> @(cursor state [:tabs])  first :id)) [:span.move-tab {:href "#" :on-click #(f/move-field :up (cursor state [:tabs])  index)} [:i.fa.fa-chevron-left.fa-fw.fa-lg] (t :profile/Movetableft)])
      (when-not (= page-id (some->> @(cursor state [:tabs])  last :id))[:span.move-tab {:href "#" :on-click #(f/move-field :down (cursor state [:tabs]) index)} (t :profile/Movetabright)[:i.fa.fa-chevron-right.fa-fw.fa-lg]])])]
   [view-page page]]))


(defn get-page [page-id state]
 (ajax/GET
      (path-for (str "/obpv1/page/view/" page-id) true)
      {:handler (fn [data]
                 (swap! state assoc :show-manage-buttons false :tab-content [page-content page-id (:page data) state]))}
      (fn [] (swap! state assoc :permission "error"))))

(defn profile-tabs [state]
  (let [tabs (cursor state [:tabs])]
   (fn []
     [:div.profile-navi
      [:ul.nav.nav-tabs
           ^{:key 0}[:li.nav-item {:class (if (= 0 (:active-index @state)) "active")}
                         [:a.nav-link {:on-click #(swap! state assoc :active-index 0 :show-manage-buttons true)}
                                      (t :user/Myprofile)]]
       (doall (for [tab @tabs
                    :let [index (.indexOf (mapv :id @tabs) (:id tab))
                          next-tab (some-> (nthnext @tabs (inc index)) first)
                          previous-tab (when (pos? index) (nth @tabs (dec index)))
                          in-edit-mode? @(cursor state [:edit-mode])]]

               ^{:key (str index "->" (:id tab))} [:li.nav-item {:data-id (:id tab)
                                                                 :class (if (= (:id tab) (:active-index @state)) "active")}
                                                   (when in-edit-mode? [:span.close {:href "#" :on-click #(do
                                                                                                            (f/remove-field tabs index)
                                                                                                            (cond
                                                                                                             (seq next-tab) (when-not (= 0 @(cursor state [:active-index]))
                                                                                                                             (get-page (:id next-tab) state)
                                                                                                                             (swap! state assoc :active-index (:id next-tab)))
                                                                                                             (seq previous-tab) (when-not (= 0 @(cursor state [:active-index]))
                                                                                                                                 (get-page (:id previous-tab) state)
                                                                                                                                 (swap! state assoc :active-index (:id previous-tab)))
                                                                                                             :else (swap! state assoc :active-index 0)))}

                                                                                    [:i.fa.fa-remove]])
                                                   [:a.nav-link.page-tab {:href "#" :on-click #(do
                                                                                                (get-page (:id tab) state)
                                                                                                (.preventDefault %)
                                                                                                (swap! state assoc :active-index (:id tab)))}

                                                               (:name tab)]]))
       [add-page state]]])))

(defn profile-navi [state]
 [profile-tabs state])
