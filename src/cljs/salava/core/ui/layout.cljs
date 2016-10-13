(ns salava.core.ui.layout
  (:require [reagent.session :as session]
            [clojure.string :as s]
            [ajax.core :as ajax]
            [salava.core.helper :refer [dump]]
            [salava.core.ui.helper :refer [current-path navigate-to path-for base-path]]
            [salava.user.ui.helper :refer [profile-picture]]
            [salava.core.ui.footer :refer [base-footer]]
            [salava.core.i18n :refer [t]]))

(defn navi-parent [path]
  (let [path (s/replace-first (str path) (re-pattern (base-path)) "")
        sections (s/split path #"/")]
    (second sections)))

(defn filtered-navi-list [navi key-list]
  (let [map-fn (fn [[tr nv]]
                 (assoc nv :target tr :active (= tr (current-path))))]
    (sort-by :weight (map map-fn (select-keys navi key-list)))))

(defn top-navi-list [navi]
  (let [key-list (filter #(get-in navi [% :top-navi]) (keys navi))]
    (filtered-navi-list navi key-list)))

(defn top-navi-landing-list [navi]
  (let [key-list (filter #(get-in navi [% :top-navi-landing]) (keys navi))]
    (filtered-navi-list navi key-list)))

(defn sub-navi-list [parent navi]
  (let [key-list (filter #(and (get-in navi [% :site-navi]) (= parent (navi-parent %))) (keys navi))]
    (when parent
      (filtered-navi-list navi key-list))))

(defn current-heading [parent headings]
  (str (get headings parent)))

(defn logout []
  (ajax/POST
    (path-for "/obpv1/user/logout")
    {:handler (fn [] (navigate-to "/user/login"))}))
 
(defn navi-link [{:keys [target title active]}]
  [:li {:class (when active "active")
        :key target}
   [:a {:href target} title]])

(defn navi-dropdown [{:keys [target title active items]}]
  (let [subitems (sub-navi-list (navi-parent (current-path)) items)
        subitemactive  (some :active subitems)]
    [:li {:key target}
     [:a {:data-toggle "collapse" :data-target (str "#"(hash target))}  title]
     [:ul {:id (hash target) :class (if subitemactive "collapse in side-dropdown-links" "collapse side-dropdown-links")}
      (doall (for [i subitems]
               (navi-link i)))]]))

(defn logo []
[:a {:class "logo pull-left"
        :href  (if (session/get :user) (path-for "/badge") (session/get :site-url))
        :title (session/get :site-name)
        :aria-label "to index"}
    [:div {:class "logo-image logo-image-url hidden-xs hidden-sm hidden-md"
          :title "OBP logo"
          :aria-label "OBP logo"}]
    [:div {:class "logo-image logo-image-icon-url visible-xs visible-sm  visible-md"}]] )

(defn top-navi-header []
  [:div {:class "navbar-header"}
   (logo)
  [:button {:type "button" :class "navbar-toggle collapsed" :data-toggle "collapse" :data-target "#navbar-collapse"}
    [:span {:class "icon-bar"}]
    [:span {:class "icon-bar"}]
    [:span {:class "icon-bar"}]]])

(defn top-navi-right []
  [:div {:id    "main-header-right"
         :class "nav navbar-nav navbar-right"}
   [:ul {:id    "secondary-menu-links"
         :class "clearfix"}
    [:li [:a {:href (path-for "/user/edit")}
          [:i {:class "fa fa-caret-right"}]
          (t :user/Myaccount)]]
    [:li [:a {:href     "#"
              :on-click #(logout)}
          [:i {:class "fa fa-caret-right"}]
          (t :user/Logout)]]]
   [:div.userpic
    [:a {:href (path-for (str "/user/profile/" (session/get-in [:user :id])))}
     [:img {:src (profile-picture (session/get-in [:user :profile_picture]))
            :alt "profile picture"}]]]])

(defn top-navi [site-navi]
  (let [items (top-navi-list (:navi-items site-navi))]
    [:nav {:class "navbar"}
     [:div {:class "container-fluid"}
      (top-navi-header)
      (top-navi-right)
      [:div {:id "navbar-collapse" :class "navbar-collapse collapse"}
       [:ul {:class "nav navbar-nav"}
       (doall (for [i items]
          (navi-link i)))]
       ]]]))

(defn top-navi-embed []
  [:nav {:class "navbar"}
   [:div {:class "container-fluid"}
    [:div {:class "navbar-header"}
     [:a {:class "logo pull-left"
          :href  (if (session/get :user) (path-for "/badge") (base-path))
          :title (session/get :site-name)}
      [:div {:class "logo-image logo-image-url hidden-xs hidden-sm hidden-md"}]
      [:div {:class "logo-image logo-image-icon-url visible-xs visible-sm  visible-md"}]]]]])

(defn top-navi-landing [site-navi]
  (let [items (top-navi-landing-list (:navi-items site-navi))]
    [:nav {:class "navbar"}
     [:div {:class "container-fluid"}
      [:div {:class "navbar-header pull-left"}
       (logo)]
      [:div {:id "main-header"
              :class "navbar-header pull-right"}
        [:a {:id "login-button" :class "btn btn-primary" :href (path-for "/user/login")}
         (t :user/Login)]
       (if (not-empty items)
         [:button {:type "button" :class "navbar-toggle collapsed" :data-toggle "collapse" :data-target "#navbar-collapse"}
          [:span {:class "icon-bar"}]
          [:span {:class "icon-bar"}]
          [:span {:class "icon-bar"}]])]
      
      [:div {:id "navbar-collapse" :class "navbar-collapse collapse"}
       [:ul {:class "nav navbar-nav"}
        (doall (for [i items]
                 (navi-link i)))]]]]))

(defn get-footer-item [navi]
  (let [key-list (filter #(get-in navi [% :footer]) (keys navi))
        footer (get navi (first key-list))]
    (:footer footer)))

(defn footer [site-navi]
  (let [footer (get-footer-item (:navi-items site-navi))]
    (if footer
      (footer)
      (base-footer))))

(defn sidebar [site-navi]
  (let [items (sub-navi-list (navi-parent (current-path)) (:navi-items site-navi))]
    [:ul {:class "side-links"}
     (doall (for [i items](if (:dropdown i)
                            (navi-dropdown i)
                            (navi-link i))))]))


(defn get-dropdown-breadcrumb [site-navi]
  (let [dropdowns  (filter #(:dropdown %) (vals (:navi-items site-navi)))
        dropdownitems (into {} (map #(:items %) dropdowns))
        matched-route (first (filter (fn [r] (re-matches (re-pattern r) (current-path))) (keys dropdownitems)))]
    [:h1 (get-in dropdownitems [matched-route :breadcrumb])]))

(defn breadcrumb [site-navi]
  (let [matched-route (first (filter (fn [r] (re-matches (re-pattern r) (current-path))) (keys (:navi-items site-navi))))]
    (if matched-route
      [:h1 (get-in site-navi [:navi-items matched-route :breadcrumb])]
      (get-dropdown-breadcrumb site-navi))))

(defn default-0 [top-items sub-items heading content]
  [:div {:role "main"}
   [:header {:id "navbar"}
    (top-navi top-items)]
   (if-not (empty? heading)
     [:div {:class "title-row"}
      [:div {:class "container"}
       [:h2 heading]]])
   [:div {:class "container main-container"}
    [:div {:class "row"}
     [:div {:class "col-md-3"} (sidebar sub-items)]
     [:div {:class "col-md-9"} content]]]
  ])


(defn default [site-navi content]
  [:div {:role "main"}
   [:header {:id "navbar"}
    (top-navi site-navi)]
   [:img {:id "print-logo" :src "/img/logo.png"}]
   [:div {:class "title-row"}
    [:div {:class "container"}
     (breadcrumb site-navi)]]
   [:div {:class "container main-container"}
    [:div {:class "row"}
     [:div {:class "col-md-2 col-sm-3"} (sidebar site-navi)]
     [:div {:class "col-md-10 col-sm-9" :id "content"} content]]]
   (footer site-navi)])

(defn default-no-sidebar [site-navi content]
  [:div {:role "main"}
   [:header {:id "navbar"}
    (top-navi site-navi)]
   [:div {:class "title-row"}
    [:div {:class "container"}
     (breadcrumb site-navi)]]
   [:div {:class "container main-container"}
    [:div {:class "row"}
     [:div {:class "col-md-12" :id "content"} content]]]
  (footer site-navi)])




(defn landing-page [site-navi content]
  [:div {:role "main"}
   [:header {:id "navbar"}
    (top-navi-landing site-navi)]
   [:div {:class "container main-container"}
    [:div {:id "content"}
     content]]
  (footer site-navi)])


(defn embed-page [content]
  [:div
   [:header {:id "navbar"}
    (top-navi-embed)]
   [:div {:class "container main-container"}
    [:div {:id "content"}
     content]]])
