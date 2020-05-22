(ns salava.extra.spaces.ui.creator
  (:require
   [reagent.core :refer [atom cursor]]
   [salava.core.i18n :refer [t]]
   [salava.core.ui.ajax-utils :as ajax]
   [salava.core.ui.helper :refer [path-for]]
   [salava.core.ui.input :refer [text-field]]
   [salava.core.ui.layout :as layout]))

(defn content [state]
  [:div.row
   [:div.col-md-12
    [:p (t :extra-spaces/Createinstructions)]]

   [:div.col-md-12
    [:div.panel.panel-default
     [:div.panel-heading]
     [:div.panel-body
      [:div.col-md-12.panel-section
       [:form.form-horizontal
        [:div.form-group
         [:label {:for "input-name"} (t :extra-spaces/Organizationname) [:span.form-required " *"]]
         [text-field
          {:name "name"
           :atom (cursor state [:space :name])
           :placeholder (t :extra-spaces/Inputorganizationname)}]]
        [:div.form-group
         [:label {:for "input-alias"} (t :extra-space/Alias) [:span.form-required " *"]]
         [text-field
          {:name "name"
           :atom (cursor state [:space :alias])
           :placeholder (t :extra-spaces/Inputalias)}]]
        [:div.form-group
         [:label {:for "input-description"} (t :extra-spaces/Description) [:span.form-required " *"]]
         [text-field
          {:name "description"
           :atom (cursor state [:space :description])
           :placeholder (t :extra-spaces/Inputdescription)}]]]]]]]])




(defn handler [site-navi]
 (let [state (atom {})]
   (prn "dadada")
   (fn []
    (layout/default site-navi [content state]))))
