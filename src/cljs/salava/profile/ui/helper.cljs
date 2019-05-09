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
            [salava.page.ui.helper :refer [view-page]]))



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
        (case type
         ("badges" "pages")[:div.checkbox
                             [:label [:input {:type "checkbox"
                                              :value @visibility-atom;(if (= "true" @visibility-atom) "false" "true")
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

(defn add-page [state]
   (when (:edit-mode @state) [:li.nav-item.dropdown [:a.dropdown-toggle {:data-toggle "dropdown"} [:i.fa.fa-plus-square]]
                                [:ul.dropdown-menu
                                 [:li [:a {:href "#" :on-click #(do
                                                                 (.preventDefault %)
                                                                 (mo/open-modal [:page :my] {:tab-atom (cursor state [:tabs])} {:hidden (fn [] (pe/save-profile state nil false) #_(prn @(cursor state [:tabs])))}))}

                                       (t :profile/Addexistingpage)]]
                                 [:li [:a {:href "#" :on-click #(do
                                                                 (.preventDefault %)
                                                                 (as-> (first (plugin-fun (session/get :plugins) "my" "create_page")) f (when f (f)) ))} (t :profile/Createnewpage)]]]]))

(def placeholder
  (doto (. js/document (createElement "li"))
    (set! -className "placeholder")))

(defn split-after [pred coll]
  (let [[l r] (split-with pred coll)]
    [(concat l [(first r)]) (rest r)]))

(defn move-before [coll placement item target]
  (let [split (partial (if (= placement :before)
                         split-with
                         split-after)
                       #(not= target %))
        join #(apply conj (first %) item (last %))]
    (->> coll
         (remove #(= item %))
         split
         (map vec)
         join)))

(defn get-page [page-id state]
 (ajax/GET
      (path-for (str "/obpv1/page/view/" page-id) true)
      {:handler (fn [data]
                 (swap! state assoc :show-manage-buttons false :tab-content [view-page (:page data)]))}

      (fn [] (swap! state assoc :permission "error"))))

(defn list-component [state]
  (let [tabs (cursor state [:tabs])
        dragged (atom nil)
        over (atom nil)
        node-placement (atom nil)
        content (atom nil)

        start-drag (fn [e]
                     (println "start dragging")
                     (reset! dragged (.-currentTarget e))
                     (set! (.. e -dataTransfer -effectAllowed) "move")
                     (. (.-dataTransfer e) (setData "text/html" (.-currentTarget e))))

        end-drag (fn [e]
                   (println "stop dragging")
                   (set! (.. @dragged -style -display) "block")
                   ;(. (.. @dragged -parentNode) (removeChild placeholder))
                   (swap! tabs
                          move-before @node-placement (.. @dragged -dataset -id) (.. @over -dataset -id)))

        drag-over (fn [e]
                    (println "dragging over")
                    (.preventDefault e)
                    (set! (.. @dragged -style -display) "none")
                    (when-not (= (.. e -target -className) "placeholder")
                      (reset! over (.-target e))
                      (let [rel-y (- (.-clientY e) (.-offsetTop @over))
                            height (/ (.-offsetHeight @over) 2)
                            parent (.. e -target -parentNode)]
                        (if (> rel-y height)
                          (do
                            (reset! node-placement :after)
                            (.insertBefore parent placeholder (.. e -target -nextElementSibling)))
                          (do
                            (reset! node-placement :before)
                            (.insertBefore parent placeholder (.. e -target)))))))]

    (fn []
     [:div.profile-navi
      [:ul.nav.nav-tabs {:on-drag-over drag-over}
           [:li.nav-item {:class (if (= 0 (:active-index @state)) "active")}
                         [:a.nav-link {:on-click #(swap! state assoc :active-index 0 :show-manage-buttons true)}
                                      (t :user/Myprofile)]]
       (doall (for [tab @tabs
                    :let [index (.indexOf @tabs tab)
                          next-tab (some-> (nthnext @tabs (inc index)) first)
                          previous-tab (when (pos? index) (nth @tabs (dec index)))]]

               ^{:key (:id tab)} [:li.nav-item {:data-id (:id tab)
                                                :draggable true
                                                :on-drag-start start-drag
                                                :on-drag-end end-drag
                                                :class (if (= (:id tab) (:active-index @state)) "active")}
                                  (when @(cursor state [:edit-mode]) [:span.close {:href "#" :on-click #(do
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
 [list-component state]
 )
