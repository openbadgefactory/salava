(ns salava.core.ui.grid
  (:require [reagent.core :refer [atom]]
            [salava.core.i18n :refer [t]]))

(defn grid-buttons
  ([title buttons key all-key state]
   (grid-buttons title buttons key all-key state nil))
  ([title buttons key all-key state func]
   [:div.form-group
    [:legend {:class "control-label col-sm-2"} title]
    [:div.col-sm-10
     (let [all-checked? (= ((keyword all-key) @state) true)
           buttons-checked ((keyword key) @state)]
       [:div.buttons
        [:button {:class (str "btn btn-default " (if all-checked? "btn-active"))
                  :id "btn-all"
                  :on-click (fn [e]
                              (.preventDefault e)
                              (swap! state assoc (keyword key) [])
                              (swap! state assoc (keyword all-key) true)
                              (if func (func state))
                              )}
         (t :core/All)]
        (doall
          (for [button buttons]
            (let [value button
                  checked? (boolean (some #(= value %) buttons-checked))]
              [:button {:class    (str "btn btn-default " (if checked? "btn-active"))
                        :key      value
                        :on-click (fn [e]
                                    (.preventDefault e)
                                    (swap! state assoc (keyword all-key) false)
                                    (if func (func state))
                                    (if checked?
                                      (do
                                        (if (= (count buttons-checked) 1)
                                          (swap! state assoc (keyword all-key) true))
                                        (swap! state assoc (keyword key)
                                               (remove (fn [x] (= x value)) buttons-checked)))
                                      (swap! state assoc (keyword key)
                                             (conj buttons-checked value))))}
               value])))])]]))

(defn translated-grid-buttons [title buttons key all-key state plugin]
  [:div.form-group
   [:span._label.filter-opt {:class "control-label col-sm-2"} title]
   [:div.col-sm-10
    (let [all-checked? (= ((keyword all-key) @state) true)
          buttons-checked ((keyword key) @state)]
      [:div.buttons
       [:button {:class (str "btn btn-default " (if all-checked? "btn-active"))
                 :id "btn-all"
                 :on-click (fn []
                             (swap! state assoc (keyword key) [])
                             (swap! state assoc (keyword all-key) true))}
        (t :core/All)]
       (doall
          (for [button buttons]
            (let [value button
                  checked? (boolean (some #(= value %) buttons-checked))]
              [:button {:class    (str "btn btn-default " (if checked? "btn-active"))
                        :key      value
                        :on-click (fn [e]
                                    (.preventDefault e)
                                    (swap! state assoc (keyword all-key) false)
                                    (if checked?
                                      (do
                                        (if (= (count buttons-checked) 1)
                                          (swap! state assoc (keyword all-key) true))
                                        (swap! state assoc (keyword key)
                                               (remove (fn [x] (= x value)) buttons-checked)))
                                      (swap! state assoc (keyword key)
                                             (conj buttons-checked value))))}
             (t (keyword (str plugin "/" value)))])))])]])

(defn grid-search-field
  ([title field-name placeholder key state]
   (grid-search-field title field-name placeholder key state nil))
  ([title field-name placeholder key state func]
   [:div.form-group
    [:label {:class "control-label col-sm-2" :for (str "grid-search-" field-name)} title]
    [:div.col-sm-10
     [:input {:class       (str field-name " form-control")
              :id          (str "grid-search-" field-name)
              :type        "text"
              :name        field-name
              :placeholder (:content (meta placeholder) placeholder)
              :value       ((keyword key) @state)
              :on-change   (fn [x]
                             (swap! state assoc key (-> x .-target .-value))
                             (if func (func state)))}]]]))

(defn grid-select
  ([title id key options state]
   (grid-select title id key options state nil))
  ([title id key options state func]
  [:div.form-group
   [:label {:class "control-label col-sm-2" :for id} title]
   [:div.col-sm-10
    [:select {:class "form-control"
              :id id
              :name key
              :on-change (fn [x]
                           (swap! state assoc key (-> x .-target .-value))
                           (if func (func state title)))}
     (for [option options]
       [:option {:value (:value option)
                 :key (:value option)} (:title option)])]]]))

(defn grid-radio-buttons
  ([title name radio-buttons key state]
   (grid-radio-buttons title name radio-buttons key state nil))
  ([title name radio-buttons key state func]
   (let [key (if (vector? key) key [key])
         checked (get-in @state key)]
     [:fieldset.form-group
      [:legend {:class "control-label col-sm-2"} title]
      [:div.col-sm-10
       (for [button radio-buttons]
         [:label {:class "radio-inline"
                  :for   (:id button)
                  :key   (:id button)}
          [:input {:id        (:id button)
                   :type      "radio"
                   :name      name
                   :checked   (= checked (:value button))
                   :value     (:value button)
                   :on-change (fn [x] (do (swap! state assoc-in key (-> x .-target .-value))
                                          (if func
                                            (func state))))}]
          (:label button)])]])))
