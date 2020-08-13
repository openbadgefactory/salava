(ns salava.extra.customField.ui.helper
 (:require
  [reagent.core :refer [atom]]
  [reagent.session :as session]
  [salava.core.ui.helper :refer [input-valid?]]
  [salava.extra.customField.schemas :as schemas]
  [salava.core.i18n :refer [t translate-text]]))


(defn field-enabled? [field]
 (let [fields (map :name (session/get :custom-fields nil))]
   (some #(= field %) fields)))

(defn compulsory-field? [field]
 (let [fields (session/get :custom-fields nil)]
  (true? (:compulsory? (first (filter #(= field (:name %)) fields))))))

(defn valid-input? [field value error-atom]
  (let [schema (if (compulsory-field? field)
                 (case field
                  "gender" schemas/gender*
                  "organization" schemas/organization*)
                 (case field
                  "gender" schemas/gender
                  "organization" schemas/organization
                  nil))]
    (when (and (not (input-valid? schema value)) error-atom)
      (reset! error-atom (t (keyword (str "extra-customField/" field "fielderror")))))
    (input-valid? schema value)))

(defn upload-modal [{:keys [status message reason]}]
  [:div
   [:div.modal-header
    [:button {:type "button"
              :class "close"
              :data-dismiss "modal"
              :aria-label "OK"}
     [:span {:aria-hidden "true"
             :dangerouslySetInnerHTML {:__html "&times;"}}]]
    (when reason [:h4.modal-title (translate-text reason)])]
   [:div.modal-body
    [:div {:class (str "alert " (if (= status "error")
                                  "alert-warning"
                                  "alert-success"))}
     (translate-text message)]]
   [:div.modal-footer
    [:button {:type "button"
              :class "btn btn-primary"
              :data-dismiss "modal"}
     "OK"]]])
