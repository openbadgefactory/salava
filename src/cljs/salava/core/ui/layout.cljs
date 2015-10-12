(ns salava.core.ui.layout)

(defn navi-link [{:keys [target title on-click]}]
  [:a {:href target :onClick on-click} title])


(defn top-navi [items]
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
      (for [i @items]
        ^{:key (:target i)} [:li (navi-link i)])]
     [:div {:id "main-header-right"
            :class "nav navbar-nav navbar-right"}
      [:ul {:id "secondary-menu-links"
            :class "clearfix"}
       [:li [:a {:href "#"} "My account"]]
       [:li [:a {:href "#"} "Log out"]]]
      [:div.userpic
       [:a {:href "#"}
        [:img {:src "/img/user.png"}]]]]]]])


(defn sidebar [items]
  [:ul {:class "side-links"}
   (for [i @items]
     ^{:key (:target i)} [:li (navi-link i)])] )


(defn default [top-items sub-items content]
  [:div
   [:header {:id "navbar"}
    (top-navi top-items)]
   [:div {:class "container main-container"}
    [:div {:class "row"}
     [:div {:class "col-md-3"} (sidebar sub-items)]
     [:div {:class "col-md-9"} content]]]])
