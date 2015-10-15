(ns salava.core.ui.grid
  (:require [reagent.core :refer [atom]]
            [salava.core.i18n :refer [t]]))

(defn grid-buttons [title buttons key all-key state]
  [:div.form-group
   [:label {:class "control-label col-sm-2"} title]
   [:div.col-sm-10
    [:div.buttons
     (let [all-checked? (= ((keyword all-key) @state) true)]
       [:button {:class (str "btn btn-default " (if all-checked? "btn-primary"))
                 :id "btn-all"
                 :on-click (fn []
                             (swap! state assoc (keyword key) [])
                             (swap! state assoc (keyword all-key) true))
                 }
        (t :badge/All)])
     (doall
       (for [button buttons]
         (let [value button
               checked? (boolean (some #(= value %) ((keyword key) @state)))]
                [:button {:class (str "btn btn-default " (if checked? "btn-primary"))
                          :key value
                          :on-click (fn []
                                      (let []
                                        (swap! state assoc (keyword all-key) false)
                                        (if checked?
                                          (swap! state assoc (keyword key)
                                                 (remove
                                                   (fn [x] (= x value))
                                                   ((keyword key) @state)))
                                          (swap! state assoc (keyword key)
                                                 (conj ((keyword key) @state) value)))))}
                 value])))]]])

(defn grid-search-field [title field-name placeholder key state]
  [:div.form-group
   [:label {:class "control-label col-sm-2" :for "grid-search"} title]
   [:div.col-sm-10
    [:input {:class (str field-name " form-control")
             :id "grid-search"
             :type "text"
             :name field-name
             :placeholder placeholder
             :value ((keyword key) @state)
             :on-change  (fn [x]
                           (swap! state assoc key (-> x .-target .-value)))}]]])

(defn grid-select [title id key options state]
  [:div.form-group
   [:label {:class "control-label col-sm-2" :for id} title]
   [:div.col-sm-10
    [:select {:class "form-control"
              :id id
              :name key
              :on-change (fn [x]
                           (swap! state assoc key (-> x .-target .-value)))}
     (for [option options]
       [:option {:value (:value option)
                 :key (:value option)} (:title option)])
     ]]])

(defn grid-radio-buttons [title name radio-buttons key state]
  [:div.form-group
   [:label {:class "control-label col-sm-2"} title]
   [:div.col-sm-10
    (for [button radio-buttons]
      [:label {:class "radio-inline"
               :for (:id button)
               :key (:id button)}
       [:input {:id (:id button)
                :type "radio"
                :name name
                :value (:value button)
                :on-change (fn [x]
                             (swap! state assoc key (-> x .-target .-value)))}]
       (:label button)])]])

(defn badge-grid-element [element-data]
  (let [{:keys [id image_file name]} element-data]
    [:div {:class "col-xs-6 col-sm-4 grid-container"
           :key id}
     [:div.media
      (if image_file
        [:div.media-left
         [:img {:src (if-not (re-find #"http" image_file)
                       (str "/" image_file)
                       image_file)}]])
      [:div.media-body
       [:a {:href (str "/badge/info/" id)}
        name]]]]))
