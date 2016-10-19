(ns salava.admin.ui.reporttool
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [clojure.string :refer [trim]]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.grid :as g]
            [salava.core.ui.helper :refer [path-for current-path navigate-to input-valid?]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.admin.schemas :as schemas]
            [salava.admin.ui.helper :refer [valid-item-type? valid-item-id?]]))

(defn save-report [state status]
  (let [{:keys [description report-type item-id item-url item-name item-type reporter-id item-content-id]} @state]
    (ajax/POST
     (path-for (str "/obpv1/admin/ticket"))
     {:response-format :json
      :keywords? true
      :params {:description description
               :report_type report-type
               :item_content_id  item-content-id
               :item_id  item-id
               :item_url item-url
               :item_name item-name
               :item_type item-type
               :reporter_id reporter-id}
      :handler (fn [data]
                 (reset! state {:status "sent"
                                :description ""
                                :report-type "bug"}))
      :error-handler (fn [{:keys [status status-text]}]
                       )})))

(defn open-reportform-button [closed? status]
  [:a {:class "reportform-button"
       :id "open-reportform-button"
       :href "#"
       :on-click #(do
                    (.preventDefault %)
                    (reset! status (if (= "true" @status) "false" "true")))}  (if closed? (t :admin/Reportproblem) (t :core/Close))])

(defn reportform [state status]
  (let [description-atom (cursor state [:description]) 
        report-type-atom (cursor state [:report-type])]
    [:div {:class "row report-form"}
     (open-reportform-button false status)
     [:div {:class "col-xs-12" :id "reportform"}
      [:h4 (t :admin/Reportproblem)]
      [:div.form-group
       (t :admin/Reportinstructions)]
      [:br]
       [:div.form-group
        [:label
         (str (t :admin/Problemconcerns) ":")]
        [:div.radio
         [:label
          [:input {:type     "radio"
                   :name     "visibility"
                   :checked  (= @report-type-atom "inappropriate")
                   :onChange #(reset! report-type-atom "inappropriate")
                   }]
          (t :admin/inappropriate)]]
        [:div.radio
         [:label
          [:input {:type     "radio"
                   :name     "visibility"
                   :checked  (= @report-type-atom "mistranslation")
                   :onChange #(reset! report-type-atom "mistranslation")}]
          (t :admin/mistranslation)]]
        [:div.radio
         [:label
          [:input {:type     "radio"
                   :name     "visibility"
                   :checked  (= @report-type-atom "bug")
                   :onChange #(reset! report-type-atom "bug")}]
          (t :admin/bug)]] 
        [:div.radio
         [:label
          [:input {:type     "radio"
                   :name     "visibility"
                   :checked  (= @report-type-atom "fakebadge")
                   :onChange #(reset! report-type-atom "fakebadge")
                   }]
          (t :admin/fakebadge)]]
        [:div.radio
         [:label
          [:input {:type     "radio"
                   :name     "visibility"
                   :checked  (= @report-type-atom "other")
                   :onChange #(reset! report-type-atom "other")
                   }]
          (t :admin/other)]]]
       [:div.form-group
        [:label 
         (str (t :admin/Description) ":")]
        [:textarea {:class    "form-control"
                    :rows     "5"
                    :value    @description-atom
                    :onChange #(reset! description-atom (.-target.value %))}]]   
       [:div.form-group
        [:button {:class    "btn btn-primary"
                  :on-click #(do
                               (.preventDefault %)
                               (save-report state status)
                               )}
         (t :admin/Send)]]]]))


(defn confirmedtext []
  [:div {:id "reportform"}
   [:div (t :admin/Confirmedtext)]])

(defn url-creator [item-type id]
  (cond
    (= item-type "badges") (path-for (str "/gallery/badgeview/" id))  
    (= item-type "page") (path-for (str "/page/view/" id))
    :else (current-path)))

(defn reporttool [id item-name item-type state]
  (let [status (cursor state [:status])
        item-url (url-creator item-type id)
        reporter-id (session/get-in [:user :id])
        item-content-id (if (= item-type "badges") id nil)
        item-id (if (= item-type "badges") nil (js/parseInt id))]
    
    (swap! state assoc
           :item-id item-id
           :item-content-id item-content-id
           :item-name item-name
           :item-type item-type
           :item-url item-url
           :reporter-id reporter-id)
    
    (if reporter-id
      [:div
       (cond
         (= @status "false") (open-reportform-button true status)
         (= @status "true")  (reportform state status)
         (= @status "sent")  (confirmedtext) 
         :else               "")]
      "")))
