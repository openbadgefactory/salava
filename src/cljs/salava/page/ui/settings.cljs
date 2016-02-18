(ns salava.page.ui.settings
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.tag :as tag]
            [salava.core.ui.helper :refer [navigate-to]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.page.ui.helper :as ph]))

(defn save-settings [page]
  (let [{:keys [id tags visibility password]} page]
    (ajax/POST
      (str "/obpv1/page/save_settings/" id)
      {:params {:tags tags
                :visibility visibility
                :password password}
       :handler #(navigate-to (str "/page/preview/" id))})))

(defn content [state]
  (let [{:keys [id name]} (:page @state)
        visibility-atom (cursor state [:page :visibility])
        tags-atom (cursor state [:page :tags])
        new-tag-atom (cursor state [:new-tag])
        password-atom (cursor state [:page :password])]
    [:div {:id "page-settings"}
     [ph/edit-page-header (t :page/Settings ": " name)]
     [ph/edit-page-buttons id :settings]
     [:div {:class "panel page-panel" :id "settings-panel"}
      [:div.form-group
       [:label {:for "page-tags"}
        (t :page/Pagetags)]
       [tag/tags tags-atom]
       [tag/new-tag-input tags-atom new-tag-atom]]
      [:div
       [:label
        (t :page/Pagevisibility)]
       [:div.radio
        [:label
         [:input {:type "radio"
                  :name "visibility"
                  :checked (= @visibility-atom "private")
                  :on-click #(reset! visibility-atom "private")}]
         (t :page/Private)]]
       [:div.radio
        [:label
         [:input {:type "radio"
                  :name "visibility"
                  :checked (= @visibility-atom "password")
                  :on-click #(reset! visibility-atom "password")}]
         (t :page/Passwordprotected)]]
       [:div.radio
        [:label
         [:input {:type "radio"
                  :name "visibility"
                  :checked (= @visibility-atom "internal")
                  :on-click #(reset! visibility-atom "internal")}]
         (t :page/Forregistered)]]
       [:div.radio
        [:label
         [:input {:type "radio"
                  :name "visibility"
                  :checked (= @visibility-atom "public")
                  :on-click #(reset! visibility-atom "public")}]
         (t :page/Public)]]]
      (if (some #(= @visibility-atom %) ["public" "password"])
        [:div.form-group
         [:input {:class    "form-control"
                  :type     "text"
                  :disabled true
                  :value    (str (session/get :site-url) "/pages/" id "/view/")}]])
      (if (= @visibility-atom "password")
        [:div.form-group
         [:label {:for "page-password"}
          (t :page/Pagepassword)]
         [:input {:id "page-password"
                  :class "form-control"
                  :type "text"
                  :value @password-atom
                  :on-change #(reset! password-atom (.-target.value %))}]])
      [:div.form-group
       [:button {:class    "btn btn-primary"
                 :on-click #(do
                             (.preventDefault %)
                             (save-settings (:page @state)))
                 :disabled (and (empty? @password-atom)
                                (= @visibility-atom "password"))}
        (t :page/Save)]]]]))

(defn init-data [state id]
  (ajax/GET
    (str "/obpv1/page/settings/" id)
    {:handler (fn [data]
                (swap! state assoc :page data))}))

(defn handler [site-navi params]
  (let [id (:page-id params)
        state (atom {:page {}})]
    (init-data state id)
    (fn []
      (layout/default site-navi (content state)))))
