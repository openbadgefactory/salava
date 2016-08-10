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

(def opened-atom (atom "true"))

(defn save-report [state]
  (let [{:keys [description report-type item-id item-url item-name item-type reporter-id item-content-id]} @state]
    (.log js/console (str description " " report-type " " item-id " " item-url " " item-name " " item-type " " reporter-id " " item-content-id))

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
                 (.log js/console "jeee onnistu")
                 )
      :error-handler (fn [{:keys [status status-text]}]
                       (.log js/console "joitain meni mönkään"))})


    
    (reset! opened-atom "sent")
    (reset! state {:description ""
                   :report-type "bug"} ))
  )

(defn reportform [state]
  
  (let [description-atom (cursor state [:description]) 
        report-type-atom (cursor state [:report-type])
        ]
    
    [:div {:class "panel panel-default"}
     [:div {:class "panel-body"}
      [:div.form-group
       [:label {:for "page-tags"}
        "Ilmoita ongelmasta"]
       ]
      [:div
       [:label
        "Ongelma koskee"]
       [:div.radio
        [:label
         [:input {:type "radio"
                  :name "visibility"
                  :checked (= @report-type-atom "inappropriate")
                  :onChange #(reset! report-type-atom "inappropriate")
                  }]
         "Asiatonta sisältöä"]]

       [:div.radio
        [:label
         [:input {:type "radio"
                  :name "visibility"
                  :checked (= @report-type-atom "mistranslation")
                  :onChange #(reset! report-type-atom "mistranslation")
                  }]
         "Käännösvirheitä" ]]
       [:div.radio
        [:label
         [:input {:type "radio"
                  :name "visibility"
                  :checked (= @report-type-atom "bug")
                  :onChange #(reset! report-type-atom "bug")
                  }]
         "Bugiraportti"]]
       
       
       [:div.radio
        [:label
         [:input {:type "radio"
                  :name "visibility"
                  :checked (= @report-type-atom "fakebadge")
                  :onChange #(reset! report-type-atom "fakebadge")
                  }]
         "Epävirallinen merkki"]]
       [:div.radio
        [:label
         [:input {:type "radio"
                  :name "visibility"
                  :checked (= @report-type-atom "other")
                  :onChange #(reset! report-type-atom "other")
                  }]
         "Jotain muuta.."]]]


      [:div.form-group
       [:label {:for "page-password"}
        (str "Lisätietoja")]
       [:textarea {:class "form-control"
                   :rows  "5"
                   :value @description-atom
                   :onChange #(reset! description-atom (.-target.value %))
                   }]]
      
      [:div.form-group
       [:button {:class    "btn btn-primary"
                 :on-click #(do
                              (.preventDefault %)
                              (save-report state)
                              )}
        "Lähetä"]]]]
    ))



(defn confirmedtext []
  [:div
   [:div "kiitos ilmoituksesta"]
   [:button {:class    "btn btn-primary"
                 :on-click #(do
                              (.preventDefault %)
                              (reset! opened-atom "false")
                              )}
        "Sulje"]])

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
     [:a {:class "pull-right"
          :on-click #(do
                       (.preventDefault %)
                       (reset! opened-atom(if (= "true" @opened-atom) "false" "true")))} "Tee ilmoitus"]
     
     (cond 
       (= @opened-atom "true") (reportform state)
       (= @opened-atom "sent") (confirmedtext) 
       :else "")
     ]))
