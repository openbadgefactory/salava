(ns salava.core.ui.layout
  (:require [reagent.session :as session]
            [clojure.string :as s]
            [ajax.core :as ajax]
            [salava.core.helper :refer [dump]]
            [salava.core.ui.helper :refer [current-path navigate-to js-navigate-to path-for base-path current-route-path plugin-fun route-path]]
            [salava.user.ui.helper :refer [profile-picture]]
            [salava.core.ui.footer :refer [base-footer]]
            [salava.social.ui.helper :refer [social-plugin?]]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.terms :refer [default-terms default-terms-fr]]))

(defn navi-parent [path]
  (let [path (s/replace-first (str path) (re-pattern (base-path)) "")
        sections (s/split path #"/")]
    (second sections)))

(defn filtered-navi-list [navi key-list type]
  (let [map-fn (fn [[tr nv]]
                 (assoc nv :target tr :active (or (= (current-path) tr) (and (= "top" type) (= (first (re-seq #"\w+" (route-path (str tr)))) (first (re-seq #"\w+" (current-route-path))))))))
        navi-list (sort-by :weight (map map-fn (select-keys navi key-list)))]
    (if (and (not= "sub-subnavi" type) (not= "top" type) (= 1 (+  (count (re-seq #"\w+" (current-route-path))))))
      (assoc-in (vec navi-list) [0 :active] true)
      navi-list)))


(defn top-navi-list [navi]
  (let [key-list (filter #(get-in navi [% :top-navi]) (keys navi))]
    (filtered-navi-list navi key-list "top")))

(defn top-navi-landing-list [navi]
  (let [key-list (filter #(get-in navi [% :top-navi-landing]) (keys navi))]
    (filtered-navi-list navi key-list "top")))

(defn sub-navi-list [parent navi type]
  (let [key-list (filter #(and (get-in navi [% :site-navi]) (= parent (navi-parent %))) (keys navi))]
    (when parent
      (filtered-navi-list navi key-list type))))

(defn current-heading [parent headings]
  (str (get headings parent)))

(defn logout []
  (ajax/POST
    (path-for "/obpv1/user/logout")
    {:handler (fn [] (js-navigate-to "/user/login"))}))

(defn return-to-admin []
  (ajax/POST
    (path-for "/obpv1/admin/return_to_admin")
    {:handler (fn [] (js-navigate-to "/admin/userlist"))}))

(defn navi-link [{:keys [target title active]}]
  [:li {:class (when active "active")
        :key target}
   [:a {:href target} title]])

(defn navi-dropdown [{:keys [target title active items]}]
  (let [subitems (sub-navi-list (navi-parent (current-path)) items "sub-subnavi")
        subitemactive  (some :active subitems)]
    [:li {:key target}
     [:a {:class "dropdown collapsed" :data-toggle "collapse" :data-target (str "#"(hash target))}  title]
     [:ul {:id (hash target) :class (if subitemactive "collapse in side-dropdown-links" "collapse side-dropdown-links")}
      (doall (for [i subitems]
               (navi-link i)))]]))

(defn logo []
 [:a {:class "logo pull-left"
      :title (session/get :site-name)
      :aria-label "to index"
      :href  (if (session/get-in [:user :first_name]) (path-for (if (social-plugin?) "/social" "/badge")) "#")
      :on-click #(if (not (session/get-in [:user :first_name])) (set! (.-location.href js/window) (session/get :site-url))  "")}
     [:div {:class "logo-image logo-image-url hidden-xs hidden-sm hidden-md"
            :title "OBP logo"
            :aria-label "OBP logo"}]
     [:div {:class "logo-image logo-image-icon-url visible-xs visible-sm  visible-md"}]])

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
    (if (session/get-in  [:user :real-id])
     [:li [:a {:href     "#"
               :on-click #(return-to-admin)}
           [:i {:class "fa fa-caret-right"}]
           (t :admin/Returntoadmin)]]
     [:li [:a {:href     "#"
               :on-click #(logout)}
           [:i {:class "fa fa-caret-right"}]
           (t :user/Logout)]])]

   [:div.userpic
    [:a {:href (path-for (str "/profile/" (session/get-in [:user :id])))}
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
                (navi-link i)))
        [:li.usermenu [:a {:href (path-for "/user/edit")}
                       (t :user/Myaccount)]]
        [:li.usermenu [:a {:href     "#"
                           :on-click #(logout)}
                       (t :user/Logout)]]]]]]))


(defn top-navi-embed []
  [:nav {:class "navbar"}
   [:div {:class "container-fluid"}
    [:div {:class "navbar-header"}
     [:a {:class "logo pull-left"
          :href  (if  (session/get-in [:user :first_name]) (path-for (if (social-plugin?) "/social" "/badge")) "#")
          :on-click #(if (not (session/get-in [:user :first_name])) (set! (.-location.href js/window) (session/get :site-url)) "")
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
       (when-not (:no-login site-navi)
         [:a {:id "login-button" :class "btn btn-primary" :href (path-for "/user/login")}
          (t :user/Login)])
       (when (not-empty items)
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
  (let [footer (first (plugin-fun (session/get :plugins) "block" "footer"))]
    (if footer
      (footer)
      (base-footer))))

(defn terms-and-conditions []
  (let [terms (first (plugin-fun (session/get :plugins) "block" "terms"))]
    (if terms
      (terms)
      (default-terms))))

(defn terms-and-conditions-fr []
  (let [terms-fr (first (plugin-fun (session/get :plugins) "block" "terms_fr"))]
    (if terms-fr
      (terms-fr)
      (default-terms-fr))))

(defn sidebar [site-navi]
  (let [items (sub-navi-list (navi-parent (current-path)) (:navi-items site-navi) "subnavi")]
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
   [:div {:id "navbar"}
    (top-navi top-items)]
   (if-not (empty? heading)
     [:div {:class "title-row"}
      [:div {:class "container"}
       [:h2 heading]]])
   [:div {:class "container main-container"}
    [:div {:class "row flip"}
     [:div {:class "col-md-3"} (sidebar sub-items)]
     [:div {:class "col-md-9"} content]]]])



(defn default [site-navi content]
  [:div {:role "main"}
   (if (session/get-in  [:user :real-id])
     (let [current-user (session/get :user)]
       [:div {:class "alert alert-warning text-center"} (str  (t :admin/Loggedas) " " (:first_name current-user) " " (:last_name current-user) ". ") [:a {:href "#" :on-click #(return-to-admin)} (t :admin/Returntoadmin)]]))
   [:div {:id "navbar"}
    (top-navi site-navi)]
   [:img {:id "print-logo" :src "/img/logo.png"}]
   [:div {:class "title-row"}
    [:div {:class "container"}
     (breadcrumb site-navi)]]
   [:div {:class "container main-container"}
    [:div {:class "row flip"}
     [:div {:class "col-md-2 col-sm-3"} (sidebar site-navi)]
     [:div {:class "col-md-10 col-sm-9" :id "content"} content]]]
   (footer site-navi)])

(defn default-no-sidebar [site-navi content]
  [:div {:role "main"}
   [:div {:id "navbar"}
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
   [:div {:id "navbar"}
    (top-navi-landing site-navi)]
   [:div {:class "container main-container"}
    [:div {:id "content"}
     content]]
   (footer site-navi)])

(defn dashboard [site-navi content]
  [:div {:role "main"}
   [:div {:id "navbar"}
    (top-navi site-navi)]
   #_[:div {:class "title-row"}
      [:div {:class "container"}
       (breadcrumb site-navi)]]
   [:div#dashboard {:class "container-fluid main-container"}
    [:div {:class "row"}
     [:div {:class "col-md-12" :id "content"} content]]]
   (footer site-navi)])


(defn embed-page [content]
  [:div
   content])
