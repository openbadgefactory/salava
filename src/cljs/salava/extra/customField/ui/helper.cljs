(ns salava.extra.customField.ui.helper
 (:require
  [reagent.session :as session]))

(defn field-enabled? [field]
 (let [fields (map :name (session/get :custom-fields nil))])
 (some #(= field %)) fields)

(defn compulsory-field? [field]
 (let [fields (session/get :custom-fields nil)]
  (true? (:compulsory? (first (filter #(= field (:name %)) fields))))))
