(ns salava.core.ui.input
  (:require [reagent.core :refer [atom]]
            [salava.core.i18n :refer [t]]))


(defn text-field [opts]
  (let [{:keys [name atom placeholder password? error-message-atom aria-label]} opts]
    [:input
     {:class       "form-control"
      :id          (str "input-" name)
      :name        name
      :type        (if password? "password" "text")
      :placeholder placeholder
      :on-change   #(do
                      (reset! atom (.-target.value %))
                      (when error-message-atom (reset! error-message-atom (:message ""))))
      :value       @atom
      :aria-label (or aria-label (str "input " name))}]))

(defn file-input [opts]
  (let [{:keys [id upload-fn aria-label style]} opts]
    [:input
     {:type "file"
      :name "file"
      :on-change #(upload-fn)
      :accept "image/png, image/svg+xml"
      :aria-label (or aria-label (t :badge/Browse))
      :style style
      :id id}]))
