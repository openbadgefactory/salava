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
            [salava.core.ui.modal :refer [open-modal]]
            [reagent-modals.modals :as m]))



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

#_(def block-type-map
    [;{:icon "fa-header" :text (t :page/Heading) :value "heading"}
      ;{:icon "fa-header" :text (t :page/Subheading) :value "sub-heading"}
      ;{:icon "fa-file-code-o" :text (t :page/Texteditor) :value "html"}
      ; {:icon "fa-file" :text (t :page/Files) :value "file"}
      ;{:icon "fa-certificate" :text (t :page/Badge) :value "badge"}
      ;{:icon "fa-tags" :text (t :page/Badgegroup) :value "tag"}
      {:icon "fa-superpowers" :text (t :page/Badgeshowcase) :value "showcase"}])
      ;{:icon "fa-user" :text "Profile information" :value "profile"}


#_(defn contenttype [{:keys [block-atom index]}]
    (let [block-type-map (if (some #(= "profile" (:type %)) @block-atom)
                           (into []  (remove #(= "profile" (:value %)) block-type-map)) block-type-map)]
      (fn []
        [:div#page-edit
         [:div#block-modal
          [:div.modal-body
           [:p.block-title (t :page/Addblock)]
           [:p (t :page/Choosecontent)]
           (reduce-kv
             (fn [r k v]
               (let [new-field-atom (atom {:type (:value v)})]
                 (conj r
                       [:div.row
                        [:div.col-md-12
                         [:div.content-type {:style {:display "inline-table"}} [:a.link {:on-click #(do
                                                                                                      (.preventDefault %)
                                                                                                      (case (:value v)
                                                                                                        "badge" (open-modal [:badge :my] {:type "pickable" :new-field-atom new-field-atom  :block-atom block-atom  :index (or index nil)})
                                                                                                        (if index
                                                                                                          (f/add-field block-atom {:type (:value v)} index)
                                                                                                          (f/add-field block-atom {:type (:value v)}))))
                                                                                         :data-dismiss (case (:value v)
                                                                                                         ("badge") nil
                                                                                                         "modal")}

                                                                                [:div

                                                                                 [:i {:class (str "fa icon " (:icon v))}]
                                                                                 [:span (:text v)]]]]
                         [:span {:style {:display "inline"}}
                          [info {:placement "right" :content (case (:value v)
                                                               "badge" (t :page/Badgeinfo)
                                                               "tag" (t :page/Badgegroupinfo)
                                                               "heading" (t :page/Headinginfo)
                                                               "sub-heading" (t :page/Subheadinginfo)
                                                               "file" (t :page/Filesinfo)
                                                               "html" (t :page/Htmlinfo)
                                                               "showcase" (t :page/Badgeshowcaseinfo)
                                                               "profile" (t :profile/Addprofileinfo))

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
                                (if index (open-modal [:page :blocktype] {:block-atom blocks :index index} {:size :md}) (open-modal [:page :blocktype] {:block-atom blocks :index nil})))}
          (t :page/Addblock)])])))

(defn recent-badges [state]
  (let [block (first (plugin-fun (session/get :plugins) "block" "recentbadges"))]
    (if block [block state] [:div ""])))

(defn recent-pages [state]
  (let [block (first (plugin-fun (session/get :plugins) "block" "recentpages"))]
    (if block [block state] [:div ""])))

(defn update-block-value [block-atom key value]
  (swap! block-atom assoc key value))

(defn badge-showcase [state block-atom]
  (let [badges (if (seq (:badges @block-atom)) (:badges @block-atom) [])
        new-field-atom (atom {:type "showcase" :badges badges})
        title (:title @block-atom)
        format (:format @block-atom)]
    [:div#badge-showcase
     [:div#grid {:class "row"}
      [:div.form-group
       [:div.col-md-12
        [:label (t :page/Title)]

        [:input {:class     "form-control"
                 :type      "text"
                 :value     title
                 :default-value (t :page/Untitled)
                 :on-change #(update-block-value block-atom :title (.-target.value %))
                 :placeholder (t :page/Untitled)}]]
       [:div.col-md-12
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
                      (badge-grid-element b block-atom "showcase" (fn [id badges] (update-block-value block-atom :badges (into [] (remove #(= id (:id %)) badges)))))))
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
        [:div.checkbox
         [:label [:input {:type "checkbox"
                          :value (if (= "true" @visibility-atom) "false" "true")
                          :on-change #(do
                                        (.preventDefault %)
                                        (update-block-value block-atom :hidden (.-target.value %)))
                          :checked (= "true" @visibility-atom)}] (t :profile/Hideinprofile)]]
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
         nil)]]]))

(defn block [block-atom state index]
  (let [type (:type @block-atom)]
    (when-not (:hidden @block-atom)
     [:div {:key index} (case type
                          ("badges") [recent-badges state]
                          ("pages") [recent-pages state]
                          nil)])))



(defn connect-user [user-id]
  (let [connectuser (first (plugin-fun (session/get :plugins) "block" "connectuser"))]
    (if connectuser
      [connectuser user-id]
      [:div ""])))


(defn save-profile [state f]
  (let [{:keys [profile_visibility about profile_picture]} (:user @state)
        profile-fields (->> (:profile @state)
                            (filter #(not-empty (:field %)))
                            (map #(select-keys % [:field :value])))
        blocks (:blocks @state)]
    (ajax/POST
      (path-for "/obpv1/user/profile")
      {:params  {:profile_visibility profile_visibility
                 :about              about
                 :profile_picture    profile_picture
                 :fields             profile-fields
                 :blocks blocks}
       :handler (fn [] (when f (f)) #_(js-navigate-to (str "/user/profile/" (:user_id @state))))})))

(defn button-logic [state]
  {:content {:previous nil
             :current :content
             :next :theme}
   :theme {:previous :content
           :current :theme
           :next :settings}})



(defn edit-page-navi [target state]
  (let [active-tab-atom (cursor state [:edit :active-tab])
        user-id (:user-id @state)]


    [:div {:class "row flip"
           :id "buttons"}
     [:div.col-xs-12
      [:div.col-xs-8.wizard
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
      [:div {:class "col-xs-4"
             :id "buttons-right"}
       [:a.btn.btn-primary {:href "#"
                            :on-click #(do
                                         (navigate-to (str "/profile/" user-id))
                                         (.preventDefault %)
                                         (reset! (cursor state [:edit-mode]) false))}

                           (t :user/Viewprofile)]]
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
           [s/share-buttons (str (session/get :site-url) (path-for "/user/profile/") user-id) fullname (= "public" @visibility-atom) false link-or-embed-atom]]
          [:div.edit-btn
           (if-not @(cursor state [:edit-mode])

             (if-not (not-activated?)
               [:a.btn.btn-primary {:href "#" #_(path-for "/user/edit/profile")
                                    :on-click #(do
                                                 (.preventDefault %)
                                                 (reset! (cursor state [:edit-mode]) true))}


                                   (t :user/Editprofile)])
             [:a.btn.btn-primary {:href "#"
                                  :on-click #(do
                                               (navigate-to (str "/profile/" user-id))
                                               (.preventDefault %)
                                               (reset! (cursor state [:edit-mode]) false))}

                                 (t :user/Viewprofile)])]]
         [edit-page-navi @(cursor state [:edit :active-tab]) state])]

      [:div
       (connect-user user-id)
       (admintool user-id "user")])))

(def additional-fields
  [{:type "email" :key :user/Emailaddress}
   {:type "phone" :key :user/Phonenumber}
   {:type "address" :key :user/Address}
   {:type "city" :key :user/City}
   {:type "state" :key :user/State}
   {:type "country" :key :user/Country}
   {:type "facebook" :key :user/Facebookaccount}
   {:type "linkedin" :key :user/LinkedInaccount}
   {:type "twitter" :key :user/Twitteraccount}
   {:type "pinterest" :key :user/Pinterestaccount}
   {:type "instagram" :key :user/Instagramaccount}
   {:type "blog" :key :user/Blog}])
