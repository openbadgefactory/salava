(ns salava.page.ui.theme
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [clojure.walk :as walk :refer [keywordize-keys]]
            [ajax.core :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.helper :refer [navigate-to]]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.page.themes :refer [themes borders]]
            [salava.page.ui.helper :as ph]))

(defn theme-selection [theme-atom themes]
  [:select {:class     "form-control"
            :on-change #(reset! theme-atom (js/parseInt (.-target.value %)))
            :value     @theme-atom}
   (for [theme themes]
     [:option {:key (:id theme) :value (:id theme)} (:name theme)])])

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
                   :on-click #(reset! border-atom border)}
             [:div {:class "select-border"
                    :style {:border-top-width width
                            :border-top-style style
                            :border-top-color color}}
              ]]))))

(defn save-theme [state]
  (let [page-id (get-in @state [:page :id])
        theme-id (get-in @state [:page :theme])
        border-id (get-in @state [:page :border :id])
        padding-id (get-in @state [:page :padding])]
    (ajax/POST
      (str "/obpv1/page/save_theme/" page-id)
      {:params {:theme theme-id
                :border border-id
                :padding padding-id}
       :handler #(navigate-to (str "/page/settings/" page-id))})))

(defn content [state]
  (let [page (:page @state)
        {:keys [id name]} page]
    [:div {:id "page-edit-theme"}
     [ph/edit-page-header (str (t :page/Choosetheme) ": " name)]
     [ph/edit-page-buttons id :theme]
     [:div {:class "panel page-panel" :id "theme-panel"}
      [:form.form-horizontal
       [:div.form-group
        [:label.col-xs-4 {:for "select-theme"}
         (t :page/Selecttheme)]
        [:div.col-xs-8
         [theme-selection (cursor state [:page :theme]) themes]]]
       [:div.form-group
        [:label.col-xs-4 {:for "select-padding"}
         (t :page/Selectpadding)]
        [:div.col-xs-8
         [padding-selection (cursor state [:page :padding])]]]
       [:div.form-group
        [:label.col-xs-4 {:for "select-border"}
         (t :page/Selectborder)]
        [:div.col-xs-8
         [border-selection (cursor state [:page :border]) borders]]]
       [:div.row
        [:div.col-md-12
         [:button {:class    "btn btn-primary"
                   :on-click #(do
                               (.preventDefault %)
                               (save-theme state))}
          (t :page/Save)]]]]]
     [ph/view-page page]]))

(defn init-data [state id]
  (ajax/GET
    (str "/obpv1/page/edit_theme/" id)
    {:handler (fn [data]
                (let [data-with-kws (keywordize-keys data)]
                  (swap! state assoc :page data-with-kws)))}))

(defn handler [site-navi params]
  (let [id (:page-id params)
        state (atom {:page {:padding 0}})]
    (init-data state id)
    (fn []
      (layout/default site-navi (content state)))))
