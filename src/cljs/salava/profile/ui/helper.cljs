(ns salava.profile.ui.helper
  (:require [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for hyperlink private? not-activated? plugin-fun navigate-to]]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.badge-grid :refer [badge-grid-element]]
            [salava.core.ui.page-grid :refer [page-grid-element]]
            [salava.core.ui.share :as s]
            [reagent.session :as session]
            [reagent.core :refer [cursor atom]]
            [salava.admin.ui.admintool :refer [admintool]]
            [salava.core.ui.field :as f]
            [salava.core.ui.modal :refer [open-modal]]))

(defn toggle-visibility [visibility-atom]
  (ajax/POST
    (path-for "/obpv1/user/profile/set_visibility")
    {:params  {:visibility (if (= "internal" @visibility-atom) "public" "internal")}
     :handler (fn [new-value]
                (reset! visibility-atom new-value)
                )}))

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
     [:label (t :user/Profilevisibility) ": " @visibility-atom]]
    ))

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
                                (if index (open-modal [:page :blocktype] {:block-atom blocks :index index} {:size :md}) (open-modal [:page :blocktype] {:block-atom blocks :index nil})))}
          (t :page/Addblock)])])))

(defn recent-badges [state]
  (let [block (first (plugin-fun (session/get :plugins) "block" "recentbadges"))]
    (if block [block state] [:div ""])))

(defn recent-pages [state]
  (let [block (first (plugin-fun (session/get :plugins) "block" "recentpages"))]
    (if block [block state] [:div ""])))

(defn block [block-atom state type index]
  (let [block-toggled? (and (:toggle-move-mode @state) (= (:toggled @state) index))]
    [:div {:key index}
     [field-after (atom []) state index]
     [:div.field.thumbnail {:class (when block-toggled? " block-to-move")}
      [:div.field-content
       [:div.form-group
        [:div.col-xs-8
         ;[:span.block-title (some-> (filter #(= type (:value %)) block-type-map) first :text capitalize) ]
         (when (= type "badge")
           [:div.row.form-group {:style {:padding-top "10px"}}
            [:div.col-xs-8 [:select {:class "form-control"
                                     :aria-label "select blocktype"
                                     :value (get-in @block-atom [:badge :format])
                                     ;:on-change #(update-block-value block-atom :format (.-target.value %))
                                     }
                            [:option {:value "short"} (t :page/Short)]
                            [:option {:value "long"} (t :page/Long)]]]
            [:div.col-xs-4
          #_ [info {:content (t :page/Badgeformatinfo) :placement "left"}]]])
         #_[block-type block-atom]]
        [:div.checkbox
         [:label [:input {:type "checkbox" :value ""}] "Hide in profile"]

         ]
        [:div.move {:on-click #(do
                                 (.preventDefault %)
                                 (cond
                                   ;(and first? last?) (swap! state assoc :toggle-move-mode false :toggled nil)
                                   (:toggle-move-mode @state) (swap! state assoc :toggle-move-mode false :toggled nil)
                                   :else (swap! state assoc :toggle-move-mode true :toggled index)))}
         [:span.move-block {:class (when block-toggled? " block-to-move")}  [:i.fa.fa-arrows]]]
       (case type
        ("showcase") [:div {:class "close-button"
               :on-click #(f/remove-field [] #_blocks index)}
         [:span {:class "remove-button" :title (t :page/Delete)}
          [:i {:class "fa fa-trash"}]]]
         nil)]
       (case type
         ("badges") [recent-badges state]
         ("pages") [recent-pages state]
         ("showcase") []
         ;("heading" "sub-heading") [edit-block-text block-atom]
         ;("badge") [badge-block block-atom]#_[edit-block-badges block-atom badges]
         ;("tag") [edit-block-badge-groups block-atom tags badges]
         ;("file") [edit-block-files block-atom files]
         ;("html") [edit-block-html block-atom]
         ;("showcase") [badge-showcase state block-atom]
         ;("profile") [profile-block block-atom]
         nil)]]]
    )
  )



(defn connect-user [user-id]
  (let [connectuser (first (plugin-fun (session/get :plugins) "block" "connectuser"))]
    (if connectuser
      [connectuser user-id]
      [:div ""])))

(defn manage-buttons [state]
  (let [visibility-atom (cursor state [:user :profile_visibility])
        user-id (:user-id @state)
        owner? (cursor state [:owner?])
        link-or-embed-atom (cursor state [:user :show-link-or-embed-code])
        fullname (str @(cursor state [:user :first_name]) " " @(cursor state [:user :last_name]))]
    (if @owner?
      [:div.manage-buttons.row;.inline
       [:div.col-xs-12
        (if-not (or (not-activated?) (private?))
          (profile-visibility-input visibility-atom state))
        [:div.share
         [s/share-buttons (str (session/get :site-url) (path-for "/user/profile/") user-id) fullname (= "public" @visibility-atom) false link-or-embed-atom]]
        [:div.edit-btn
         (if-not @(cursor state [:edit-mode])

           (if-not (not-activated?)
             [:a.btn.btn-primary {:href "#" #_(path-for "/user/edit/profile")
                                  :on-click #(do
                                               (.preventDefault %)
                                               (reset! (cursor state [:edit-mode]) true)

                                               )
                                  } (t :user/Editprofile)])
           [:a.btn.btn-primary {:href "#" #_(path-for "/user/edit/profile")
                                :on-click #(do
                                             (navigate-to (str "/profile/" user-id))
                                             (.preventDefault %)
                                             (reset! (cursor state [:edit-mode]) false)
                                             )
                                } (t :user/Viewprofile)])]]]
      [:div
       (connect-user user-id)
       (admintool user-id "user")])))
