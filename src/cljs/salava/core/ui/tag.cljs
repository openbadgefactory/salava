(ns salava.core.ui.tag
  (:require [clojure.string :refer [trim lower-case]]
            [salava.core.i18n :refer [t]]))

(defn add-tag [tags-atom new-tag-atom]
  (let [new-tag (trim @new-tag-atom)
        tag-exists? (some #(= (lower-case new-tag)
                              (lower-case %)) @tags-atom)]
    (when (and (not (empty? new-tag))
               (not tag-exists?))
      (reset! tags-atom (conj (vec @tags-atom) new-tag))
      (reset! new-tag-atom ""))))

(defn remove-tag [tags-atom tag-value]
  (reset! tags-atom (remove #(= % tag-value) @tags-atom)))

(defn new-tag-input [tags-atom new-tag-atom]
  [:input {:type        "text"
           :class       "form-control"
           :placeholder (t :badge/Typetag)
           :id          "newtags"
           :value       @new-tag-atom
           :on-change   #(reset! new-tag-atom (-> % .-target .-value))
           :on-key-down #(if (= (.-which %) 13)
                          (add-tag tags-atom new-tag-atom))}])

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
                   :on-click #(remove-tag tags-atom tag)}])]))))

