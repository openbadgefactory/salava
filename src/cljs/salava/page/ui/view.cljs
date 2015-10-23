(ns salava.page.ui.view
  (:require [reagent.core :refer [atom]]
            [reagent.session :as session]
            [clojure.walk :as walk :refer [keywordize-keys]]
            [ajax.core :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.time :refer [date-from-unix-time]]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.page.ui.helper :as ph]))

(defn content [state]
  (let [{:keys [name description mtime first_name last_name blocks]} (:page @state)]
    [:div {:id "theme-10"
           :class "page-content"}
     [:div.panel
      [:div.panel-left
       [:div.panel-right
        [:div.panel-content
         [:div.row
          [:div {:class "col-md-12 page-mtime"}
           (date-from-unix-time (* 1000 mtime))]]
         [:div.row
          [:div.col-md-12
           [:h1.main-title name]]]
         [:div.row
          [:div {:class "col-md-12 author"}
           [:a {:href "#"}
            (str first_name " " last_name)]]]
         (into [:div.page-blocks]
               (for [block blocks]
                 (case (:type block)
                   "badge" (ph/badge-block block)
                   "html" (ph/html-block block)
                   "file" (ph/file-block block)
                   "heading" (ph/heading-block block)
                   nil)))]]]]]))

(defn init-data [state id]
  (ajax/GET
    (str "/obpv1/page/view/" id)
    {:handler (fn [data]
                (let [data-with-kws (keywordize-keys data)]
                  (swap! state assoc :page data-with-kws)))}))

(defn handler [site-navi params]
  (let [id (:page-id params)
        state (atom {:page {}
                     :page-id id})]
    (init-data state id)
    (fn []
      (layout/default site-navi (content state)))))
