(ns salava.core.ui.layout)

(defn navi-link [{:keys [target title on-click]}]
  [:a {:href target :onClick on-click} title])


(defn top-navi [items]
  [:nav {:class "navbar navbar-inverse navbar-fixed-top"}
   [:div {:class "container"}
    [:div {:class "navbar-header"}
     [:button {:type "button" :class "navbar-toggle collapsed" :data-toggle "collapse" :data-target "#navbar"}
      [:span {:class "icon-bar"}]
      [:span {:class "icon-bar"}]
      [:span {:class "icon-bar"}]]
     [:a {:class "navbar-brand" :href "/"} "Open Badge Passport"]]
    [:div {:id "navbar" :class "navbar-collapse collapse"}
     [:ul {:class "nav navbar-nav"}
      (for [i @items]
        ^{:key (:target i)} [:li (navi-link i)])]]]] )


(defn sidebar [items]
  [:ul {:class="sidebar-nav" }
   (for [i @items]
     ^{:key (:target i)} [:li (navi-link i)])] )


(defn default [top-items sub-items content]
  [:div
   (top-navi top-items)
   [:div {:class "container main"}
    [:div {:class "row"}
     [:div {:class "col-md-3"} (sidebar sub-items)]
     [:div {:class "col-md-9"} content]]]])
