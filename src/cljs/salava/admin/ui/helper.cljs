(ns salava.admin.ui.helper
  (:require
   [reagent.core :refer [atom cursor]]
   [reagent.session :as session]
   [salava.core.i18n :refer [t]]
   [salava.core.ui.helper :refer [input-valid?]]
   [salava.admin.schemas :as schemas]))



(defn valid-item-type? [item]
  (input-valid? (:item-type schemas/Url-parser) item))

(defn valid-item-id? [item]
  (input-valid? (:item-id schemas/Url-parser) (js/parseInt item))
  )

(defn checker [url]
  (let [url-list (vec(re-seq #"\w+" (str url) ))
        type (get url-list 1)
        id  (get url-list 3)]
    {:item-type (if (= type "gallery") "badges" type)
     :item-id id}))

(defn admin? []
  (let [role (session/get-in [:user :role])]
    (= role "admin")))


(defn message-form [mail]
  (let [message (cursor mail [:message])
        subject (cursor mail [:subject])]
    [:div
     [:div.form-group
      [:label 
       (str (t :admin/Subjectforitemowner) ":")]
      [:input {:class    "form-control"
               :value    @subject
               :onChange #(reset! subject (.-target.value %)) }]]
     [:div.form-group
      [:label 
       (str (t :admin/Messageforitemowner) ":")]
      [:textarea {:class    "form-control"
                  :rows     "5"
                  :value    @message
                  :onChange #(reset! message (.-target.value %))}]]]))


(defn email-select [emails email-atom]
  (let [primary-email (first (filter #(:primary_address %) emails))
        secondary-emails (filter #(not (:primary_address %)) emails)]
    (if (not (pos? (count secondary-emails)))
      [:div (:email primary-email) ]
      [:select {:class     "form-control"
                :id        "emails"
                :value     @email-atom
                :on-change #(reset! email-atom (.-target.value %))
                }
       [:optgroup {:label (str (t :admin/Primaryemail) ":")}
        [:option {:key (hash (:email primary-email)) :value (:email primary-email)} (:email primary-email) ]]
       [:optgroup {:label (str (t :admin/Secondaryemail) ":")}
        (doall
         (for [element-data secondary-emails]
           [:option {:key (hash (:email element-data)) :value (:email element-data)} (:email element-data) ]))
        ] ])))


(defn status-handler [status item_type]
  (cond
    (= "success" @status)[:div {:class "alert alert-success col-xs-6 cos-md-8"}
                         (t :admin/Messagesentsuccessfully) ]
    (= "error" @status) [:div {:class "alert alert-warning col-xs-6 cos-md-8"}
                        (t :admin/Somethingwentwrong)]
    :else ""))
