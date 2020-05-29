(ns salava.extra.spaces.ui.modal
  (:require
    [clojure.string :refer [blank?]]
    [reagent.core :refer [cursor atom]]
    [reagent-modals.modals :as m]
    [salava.core.ui.ajax-utils :as ajax]
    [salava.core.ui.modal :as mo]
    [salava.core.ui.helper :refer [path-for]]
    [salava.core.i18n :refer [t]]
    [salava.core.time :refer [date-from-unix-time]]
    [salava.extra.spaces.ui.creator :as creator]))


(defn init-data [id state]
  (ajax/GET
    (path-for (str "/obpv1/spaces/"id))
    {:handler (fn [data]
                (swap! state assoc :space data))}))

(defn space-logo [state]
  (let [{:keys [logo name]} @(cursor state [:space])]
    [:div.text-center {:class "col-md-3" :style {:margin-bottom "20px"}}
     [:div
      (if-not (blank? logo)
        [:img.space-img {:src (str "/" logo)
                         :alt name}]
        [:i.fa.fa-building-o.fa-5x {:style {:margin-top "10px"}}])]]))

(defn space-banner [state]
  (let [{:keys [banner]} @(cursor state [:space])]
    (when banner
      [:div.banner-container {:style {:max-width "640px" :max-height "120px"}}
        [:img {:src (str "/" banner)}]])))

(defn view-space [state]
  (let [{:keys [name description ctime status alias css]} @(cursor state [:space])
        {:keys [p-color s-color t-color]} css]
    [:div
      [space-banner state]
      [:h1.uppercase-header name]
      [:p [:b description]]
      [:div [:span._label (str (t :extra-spaces/Alias) ":  ")] alias]
      [:div [:span._label (str (t :extra-spaces/Createdon) ":  ")] (date-from-unix-time (* 1000 ctime))]
      [:div [:span._label (str (t :extra-spaces/Status) ": ")] status]
      (when css
        [:div
          [:div [:span._label (str (t :extra-spaces/Primarycolor) ":  ")] [:span.color-span {:style {:background-color p-color}}]]
          [:div [:span._label (str (t :extra-spaces/Secondarycolor) ":  ")] [:span.color-span {:style {:background-color s-color}}]]
          [:div [:span._label (str (t :extra-spaces/tertiarycolor) ":  ")] [:span.color-span {:style {:background-color t-color}}]]])]))



(defn edit-space [state]
   [creator/modal-content state])


(defn delete-space [state]
  [:div.col-md-9])

(defn manage-space [state]
  [:div.col-md-9])



(defn space-navi [state]
  [:div.row.flip-table
   [:div.col-md-3]
   [:div.col-md-9
     [:ul {:class "nav nav-tabs wrap-grid"}
       [:li.nav-item {:class  (if (or (nil? (:tab-no @state)) (= 1 (:tab-no @state))) "active")}
        [:a.nav-link {:href "#" :on-click #(swap! state assoc :tab [view-space state] :tab-no 1)}
         [:div  [:i.nav-icon.fa.fa-info-circle.fa-lg] (t :metabadge/Info)]]]
       [:li.nav-item {:class  (if (or (nil? (:tab-no @state)) (= 2 (:tab-no @state))) "active")}
        [:a.nav-link {:href "#" :on-click #(swap! state assoc :tab [edit-space state]  :tab-no 2)}
         [:div  [:i.nav-icon.fa.fa-edit.fa-lg] (t :core/Edit)]]]
       #_[:li.nav-item {:class  (if (or (nil? (:tab-no @state)) (= 3 (:tab-no @state))) "active")}
          [:a.nav-link {:href "#" :on-click #(swap! state assoc :tab [edit-space state]  :tab-no 3)}
           [:div  [:i.nav-icon.fa.fa-cog.fa-lg] (t :extra-spaces/Manage)]]]
       [:li.nav-item {:class  (if (or (nil? (:tab-no @state)) (= 4 (:tab-no @state))) "active")}
        [:a.nav-link {:href "#" :on-click #(swap! state assoc :tab [manage-space state]  :tab-no 4)}
         [:div  [:i.nav-icon.fa.fa-cogs.fa-lg] (t :extra-spaces/Managespace)]]]
       [:li.nav-item {:class  (if (or (nil? (:tab-no @state)) (= 5 (:tab-no @state))) "active")}
        [:a.nav-link {:href "#" :on-click #(swap! state assoc :tab [delete-space state] :tab-no 5)}
         [:div  [:i.nav-icon {:class "fa fa-trash fa-lg"}] (t :core/Delete)]]]]]])

(defn space-content [state]
  [:div#space
   [space-navi state]
   [:div.col-md-12
    [space-logo state]
    [:div.col-md-9
     (or
       (:tab @state)
       (case (:tab-no @state)
         2 [edit-space state]
         4 [manage-space state]
         5 [delete-space state]
         [view-space state]))]]])



(defn handler [params]
  (let [id (:id params)
        state (atom {:id id
                     :tab-no 1})]
    (init-data id state)
    (fn []
      [space-content state])))


(def ^:export modalroutes
  {:space {:info handler}})
