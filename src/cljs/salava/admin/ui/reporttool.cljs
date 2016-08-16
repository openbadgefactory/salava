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
            [salava.admin.ui.helper :refer [valid-item-type? valid-item-id? checker]]))

(def opened-atom (atom "false"))

(defn save-report [state]
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
                 (reset! opened-atom "sent")
                 (reset! state {:description ""
                                :report-type "bug"}))
      :error-handler (fn [{:keys [status status-text]}]
                       )})))

(defn open-reportform-button [closed?]
  [:a {:class "pull-right"
       :id "open-reportform-button"
       :on-click #(do
                    (.preventDefault %)
                    (reset! opened-atom(if (= "true" @opened-atom) "false" "true")))}  (if closed? (t :admin/Reportproblem) (t :admin/Close))])

(defn reportform [state]
  (let [description-atom (cursor state [:description]) 
        report-type-atom (cursor state [:report-type])]
    [:div  
     (open-reportform-button false)
     [:div {:class "col-xs-12" :id "reportform"}
      [:h4 (t :admin/Reportproblem)]
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
                               (save-report state)
                               )}
         (t :admin/Send)]]]]))

(def seconds-elapsed (atom 0))

(defn confirmedtext-timer []
  (js/setTimeout #(swap! seconds-elapsed inc) 1000)
  (if (=@seconds-elapsed 5)
    (do
      (reset! opened-atom "false")
      (reset! seconds-elapsed 0))))

(defn confirmedtext []
  [:div {:id "reportform"}
   (confirmedtext-timer)
   [:div (t :admin/Confirmedtext)]])

(defn url-creator [item-type id]
  (cond
    (= item-type "badges") (path-for (str "/gallery/badgeview/" id))  
    (= item-type "page") (path-for (str "/page/view/" id))
    :else (current-path)))

(def init-state (atom {:description ""
                       :report-type "bug"
                       :item-id ""
                       :item-content-id ""
                       :item-url   ""
                       :item-name "" ;
                       :item-type "" ;badge/user/page/badges
                       :reporter-id ""}))

(defn reporttool [id item-name item-type]
  (let [state init-state
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
    
    [:div
     (cond
       (= @opened-atom "false") (open-reportform-button true)
       (= @opened-atom "true") (reportform state)
       (= @opened-atom "sent") (confirmedtext) 
       :else "")]))
