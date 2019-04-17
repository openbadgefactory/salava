(ns salava.page.ui.theme
  (:require [reagent.core :refer [atom cursor]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.helper :refer [navigate-to path-for]]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.page.themes :refer [themes borders]]
            [salava.page.ui.helper :as ph]))

#_(defn theme-selection [theme-atom themes]
  [:select {:class     "form-control"
            :on-change #(reset! theme-atom (js/parseInt (.-target.value %)))
            :value     @theme-atom}
   (for [theme themes]
     [:option {:key (:id theme) :value (:id theme)} (t (:name theme))])])

(defn theme-selection [theme-atom themes]
  (reduce (fn [r theme]
            (conj r [:div {:id (str "theme-" (:id theme))}
                     [:a {:href "#" :on-click #(do
                                                 (.preventDefault %)
                                                 (reset! theme-atom (js/parseInt (:id theme)) ))
                          :alt (t (:name theme)) :title (t (:name theme))}[:div {:class (str "panel-right theme-container" (if (= @theme-atom (:id theme)) " selected"))} " " ]]])
            )[:div {:id "theme-container"}] themes))

(defn padding-selection [padding-atom]
  [:div.row
   [:div.col-xs-1
    @padding-atom]
   [:div.col-xs-11
    [:input {:type      "range"
             :value     @padding-atom
             :min       0
             :max       50
             :step      5
             :on-change #(reset! padding-atom (js/parseInt (.-target.value %)))}]]])

(defn border-selection [border-atom borders]
  (into [:div.clearfix]
        (for [border borders]
          (let [{:keys [id width style color]} border]
            [:div {:class    (str "select-border-wrapper" (if (= id (:id @border-atom)) " selected"))
                   :on-click #(reset! border-atom border)
                   :on-touch-end #(reset! border-atom border)}
             [:div {:class "select-border"
                    :style {:border-top-width width
                            :border-top-style style
                            :border-top-color color}}
              ]]))))

(defn save-theme [state next-url]
  (let [page-id (get-in @state [:page :id])
        theme-id (get-in @state [:page :theme])
        border-id (get-in @state [:page :border :id])
        padding-id (get-in @state [:page :padding])]
    (ajax/POST
      (path-for (str "/obpv1/page/save_theme/" page-id))
      {:params {:theme theme-id
                :border border-id
                :padding padding-id}
       :handler (fn [data]
                  (swap! state assoc :alert {:message (t (keyword (:message data))) :status (:status data)})
                  (js/setTimeout (fn [] (swap! state assoc :alert nil)) 3000)
                  )#_(navigate-to next-url)})))

(defn content [state]
  (let [page (:page @state)
        {:keys [id name]} page]

    [:div {:id "page-edit-theme"}
     [ph/edit-page-header (t :page/Choosetheme ": " name)]
     [ph/edit-page-buttons id :theme state (fn [next-url] (save-theme state next-url))]
     [:div {:class "panel page-panel thumbnail" :id "theme-panel"}
      [:form.form-horizontal
       [:div.form-group
        [:label.col-xs-4 {:for "select-theme"}
         (str (t :page/Selecttheme) ":")]
        [:div.col-xs-8
         [theme-selection (cursor state [:page :theme]) themes]]]
       [:div.form-group
        [:label.col-xs-4 {:for "select-padding"}
         (str (t :page/Selectpadding) ":")]
        [:div.col-xs-8
         [padding-selection (cursor state [:page :padding])]]]
       [:div.form-group
        [:label.col-xs-4 {:for "select-border"}
         (str (t :page/Selectborder) ":")]
        [:div.col-xs-8
         [border-selection (cursor state [:page :border]) borders]]]
       #_[:div.row
          [:div.col-md-12
           [:button {:class    "btn btn-primary"
                     :on-click #(do
                                  (.preventDefault %)
                                  (save-theme state (str "/profile/page/settings/" id)))}
            (t :page/Save)]]]]]
     [ph/manage-page-buttons (fn [] (save-theme state (str "/profile/page/settings/" id))) state (str "/profile/page/settings/" id) (str "/profile/page/edit/" id) false]

     [ph/view-page page]]))

(defn init-data [state id]
  (ajax/GET
    (path-for (str "/obpv1/page/" id) true)
    {:handler (fn [data]
                (swap! state assoc :page data))}))

(defn handler [site-navi params]
  (let [id (:page-id params)
        state (atom {:page {:id id :padding 0}})]
    (init-data state id)
    (fn []
      (layout/default site-navi (content state)))))
