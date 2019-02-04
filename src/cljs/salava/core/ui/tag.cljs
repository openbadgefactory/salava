(ns salava.core.ui.tag
  (:require [clojure.string :refer [trim lower-case]]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.helper :refer [plugin-fun path-for]]
            [reagent.session :as session]
            [reagent.core :refer [cursor atom]]
            [salava.core.ui.ajax-utils :as ajax]
            ))

(defn update-settings [badge-id state]
  (ajax/GET
    (path-for (str "/obpv1/badge/settings/" badge-id) true)
    {:handler (fn [data]
                (swap! state assoc :badge-settings data (assoc data :new-tag "")))}))

(defn save-settings [state]
  (let [{:keys [id visibility tags rating]} @(cursor state [:badge-settings])]
    (ajax/POST
      (path-for (str "/obpv1/badge/save_settings/" id))
      {:params  {:visibility   visibility
                 :tags         tags
                 :rating       (if (pos? rating) rating nil)}
       :handler (fn []
                  (update-settings id state))})))

(defn add-tag
  ([tags-atom new-tag-atom]
   (let [new-tag (trim @new-tag-atom)
         tag-exists? (some #(= (lower-case new-tag)
                               (lower-case %)) @tags-atom)]
     (when (and (not (empty? new-tag))
                (not tag-exists?))
       (reset! tags-atom (conj (vec @tags-atom) new-tag))
       (reset! new-tag-atom ""))))
  ([tags-atom new-tag-atom state]
   (do
     (add-tag tags-atom new-tag-atom)
     (save-settings state)
     )))

(defn remove-tag [tags-atom tag-value]
  (reset! tags-atom (remove #(= % tag-value) @tags-atom)))

(defn new-tag-input
  ([tags-atom new-tag-atom]
   [:input {:type        "text"
            :class       "form-control"
            :placeholder (t :badge/Typetag)
            :id          "newtags"
            :value       @new-tag-atom
            :on-change   #(reset! new-tag-atom (-> % .-target .-value))
            :on-key-down #(if (= (.-which %) 13)
                            (add-tag tags-atom new-tag-atom))}])

  ([tags-atom new-tag-atom state reload-fn]
   [:input {:type        "text"
            :class       "form-control"
            :placeholder (t :badge/Typetag)
            :id          "newtags"
            :value       @new-tag-atom
            :on-change   #(reset! new-tag-atom (-> % .-target .-value))
            :on-key-down #(if (= (.-which %) 13)
                            (add-tag tags-atom new-tag-atom state))}]))

(defn tags
  ([tags-atom] (tags tags-atom true))
  ([tags-atom show-remove-link?]
   (into [:div.tag-labels]
         (for [tag @tags-atom]
           [:span {:class "label label-default"}
            tag
            (if show-remove-link?
              [:a {:class "remove-tag"
                   :dangerouslySetInnerHTML {:__html "&times;"}
                   :on-click #(remove-tag tags-atom tag)}])])))
  ([tags-atom state reload-fn]
   (into [:div.tag-labels]
         (for [tag @tags-atom]
           [:span {:class "label label-default"}
            tag
            [:a {:class "remove-tag"
                 :dangerouslySetInnerHTML {:__html "&times;"}
                 :on-click #(do
                              (.preventDefault %)
                              (remove-tag tags-atom tag)
                              (save-settings state))}]]))))
