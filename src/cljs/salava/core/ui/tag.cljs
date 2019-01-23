(ns salava.core.ui.tag
  (:require [clojure.string :refer [trim lower-case]]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.helper :refer [plugin-fun]]
            [reagent.session :as session]))

(defn add-tag
  ([tags-atom new-tag-atom]
   (let [new-tag (trim @new-tag-atom)
         tag-exists? (some #(= (lower-case new-tag)
                               (lower-case %)) @tags-atom)]
     (when (and (not (empty? new-tag))
                (not tag-exists?))
       (reset! tags-atom (conj (vec @tags-atom) new-tag))
       (reset! new-tag-atom ""))))
  ([tags-atom new-tag-atom state reload-fn]
   (let [save-settings (first (plugin-fun (session/get :plugins) "settings" "save_settings"))]
     (do
       (add-tag tags-atom new-tag-atom)
       (save-settings state reload-fn "share")
       ))))

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
                            (add-tag tags-atom new-tag-atom state reload-fn))}]))

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
   (let [save-settings (first (plugin-fun (session/get :plugins) "settings" "save_settings"))]
     (into [:div.tag-labels]
           (for [tag @tags-atom]
             [:span {:class "label label-default"}
              tag
              [:a {:class "remove-tag"
                   :dangerouslySetInnerHTML {:__html "&times;"}
                   :on-click #(do
                                (.preventDefault %)
                                (remove-tag tags-atom tag)
                                (save-settings state reload-fn "share"))}]])))))
