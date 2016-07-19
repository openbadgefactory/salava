(ns salava.admin.ui.tickets
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [clojure.string :refer [trim]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.grid :as g]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]))


(defn media [element-data state]
  (let [{:keys [item-type item-id item-name item-owner date reporter description]} element-data
        url (str "/" item-type (cond
                                 (= item-type "badge") "/info/"
                                 (= item-type "page") "/view/"
                                 :else "/profile/" ) item-id )]
    [:div {:class "media" :id "ticket-container"}
     [:div.media-body
      [:div.media-heading
       [:h4 {:class "media-heading"}
        [:a {:href (path-for url) :target "_blank"}
         (if (= item-name item-owner)
           (str item-type " - " item-owner)
           (str item-type  " - " item-name " - " item-owner))]]
       ]
      [:div.media-descriprtion
       date
       [:div [:label "ilmoittaja"] ": " reporter]
       [:div [:label "Kuvaus"] ": " description]]]])
  )


(defn content [state]
  (let [tickets (:tickets @state)]
    [:div
     (into [:div {:class "row"}]
           (for [element-data tickets]
             (media element-data state)))
     ])
  )

(defn init-state [state]
  (ajax/GET "/obpv1/hello/counter"
            {:handler (fn [data]
                        (let [counter (get data "value" 0)]
                          (reset! state counter)))}))


(defn handler [site-navi]
  (let [state (atom {:tickets [{:item-type   "page"
                                :item-id     "2039"
                                :item-name   "Törky sivu"
                                :item-owner  "Hullu jaakko"
                                :date        "25.3.2015"
                                :reporter    "Leo Vainio"
                                :description "Tuhmia kuvia!"
                                }
                               {:item-type   "badge"
                                :item-id     "15026"
                                :item-name   "NUFLEF EN TORTOR"
                                :item-owner  "hullu jaakko"
                                :date        "25.3.2015"
                                :reporter    "Leo Vainio"
                                :description "Ruma badge!"
                                }
                               {:item-type   "user"
                                :item-id     "6080"
                                :item-name   "Leo Vainio"
                                :item-owner  "Leo Vainio"
                                :date        "25.3.2015"
                                :reporter    "Leo Vainio"
                                :description "Sopimatonta tekstiä!"
                                 }]})]
    (fn []
      (layout/default site-navi (content state)))))
