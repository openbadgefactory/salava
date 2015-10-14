(ns salava.core.ui.layout
  (:require [clojure.string :as str]
            [salava.core.helper :refer [dump]]
            [salava.core.ui.helper :refer [current-path]]))

(defn navi-parent [path]
  (let [sections (str/split path #"/")]
      (second sections)))

(defn filtered-navi-list [navi key-list]
  (let [map-fn (fn [[tr nv]]
                 (assoc nv :target tr))]
    (sort-by :weight (map map-fn (select-keys navi key-list)))))

(defn top-navi-list [navi]
  (let [key-list (filter #(<= (count (str/split % #"/")) 2) (keys navi))]
    (filtered-navi-list navi key-list)))

(defn sub-navi-list [parent navi]
  (let [parent-filter #(and (not= (str "/" parent "/") %) (= parent (navi-parent %)))
        key-list (filter parent-filter (keys navi))]
    (when parent
      (filtered-navi-list navi key-list))))

(defn current-heading [parent headings]
  (str (get headings parent)))


(defn navi-link [{:keys [target title]}]
  [:a {:href target} title])


(defn top-navi [site-navi]
  (let [items (top-navi-list (:navi-items site-navi))]
    [:nav {:class "navbar"}
     [:div {:class "container-fluid"}
      [:div {:class "navbar-header"}
       [:a {:class "logo pull-left"
            :href  "/"
            :title "Open Badge Passport"}
        [:img {:src   "/img/logo.png"
               :class "logo-main"}]
        [:img {:src "/img/logo_icon.png" :class "logo-icon"}]]
       [:button {:type "button" :class "navbar-toggle collapsed" :data-toggle "collapse" :data-target "#navbar-collapse"}
        [:span {:class "icon-bar"}]
        [:span {:class "icon-bar"}]
        [:span {:class "icon-bar"}]]]
      [:div {:id "navbar-collapse" :class "navbar-collapse collapse"}
       [:ul {:class "nav navbar-nav"}
        (for [i items]
          ^{:key (:target i)} [:li (navi-link i)])]
       [:div {:id "main-header-right"
              :class "nav navbar-nav navbar-right"}
        [:ul {:id "secondary-menu-links"
              :class "clearfix"}
         [:li [:a {:href "#"} "My account"]]
         [:li [:a {:href "#"} "Log out"]]]
        [:div.userpic
         [:a {:href "#"}
          [:img {:src "/img/user.png"}]]]]]]]))


(defn sidebar [site-navi]
  (let [items (sub-navi-list (navi-parent (current-path)) (:navi-items site-navi))]
    [:ul {:class "side-links"}
     (for [i items]
       ^{:key (:target i)} [:li (navi-link i)])]) )

(defn breadcrumb [site-navi]
  [:h2 (current-heading (navi-parent (current-path)) (:headings site-navi))])


(defn default-0 [top-items sub-items heading content]
  [:div
   [:header {:id "navbar"}
    (top-navi top-items)]
   (if-not (empty? heading)
     [:div {:class "title-row"}
      [:div {:class "container"}
       [:h2 heading]]])
   [:div {:class "container main-container"}
    [:div {:class "row"}
     [:div {:class "col-md-3"} (sidebar sub-items)]
     [:div {:class "col-md-9"} content]]]])


(defn default [site-navi content]
  [:div
   [:header {:id "navbar"}
    (top-navi site-navi)]
   [:div {:class "title-row"}
    [:div {:class "container"}
     (breadcrumb site-navi)]]
   [:div {:class "container main-container"}
    [:div {:class "row"}
     [:div {:class "col-md-3"} (sidebar site-navi)]
     [:div {:class "col-md-9"} content]]]])

