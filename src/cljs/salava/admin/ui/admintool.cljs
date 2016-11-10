(ns salava.admin.ui.admintool
  (:require [reagent.core :refer [atom]]
            [reagent-modals.modals :as m]
            [clojure.string :refer [trim]]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for current-path]]
            [salava.core.i18n :refer [t]]
            [salava.admin.ui.helper :refer [admin?]]
            [salava.core.helper :refer [dump]]
            [salava.admin.ui.admintool-content :refer [admin-modal]]))


(defn open-admintool-modal
  ([item-id item-type]
   (let [state (atom {:mail          {:subject ""
                                      :message ""}
                      :visible_area  ""
                      :item_type     item-type
                      :item_id       item-id
                      :item_owner    ""
                      :item_owner_id nil
                      :image_file    nil
                      :name          ""
                      :selected-email ""
                      :info          {:created     ""
                                      :description ""}
                      :gallery-state nil
                      :init-data nil})]
     (ajax/GET
      (path-for (str "/obpv1/admin/"item-type"/" item-id))
      {:handler (fn [data]
                  (do
                    (swap! state assoc :name (:name data)
                           :image_file (:image_file data)
                           :item_owner (:item_owner data)
                           :item_owner_id (:item_owner_id data)
                           :info (:info data))
                    )
                  (m/modal! [admin-modal state nil nil] {:size :lg}))})))

  ([item-type item-id gallery-state init-data]
   (let [ state (atom {:mail          {:subject ""
                                       :message ""}
                       :visible_area  ""
                       :item_type     item-type
                       :item_id       item-id
                       :item_owner    ""
                       :item_owner_id nil
                       :image_file    nil
                       :name          ""
                       :info          {}
                       :gallery-state gallery-state
                       :init-data init-data
                       :status ""})]
     (ajax/GET
      (path-for (str "/obpv1/admin/"item-type"/" item-id))
      {:handler (fn [data]
                  (do
                    (swap! state assoc :name (:name data)
                           :image_file (:image_file data)
                           :item_owner (:item_owner data)
                           :item_owner_id (:item_owner_id data)
                           :info (:info data))
                    (if (= "user" item-type)
                      (let [primary-email (first (filter #(:primary_address %) (get-in data [:info :emails])))]
                        (swap! state assoc :selected-email (:email primary-email)))))
                  (m/modal! [admin-modal state] {:size :lg}))}))))

(defn admintool [item-id item-type]
  (if (admin?)
    [:div
     [:div {:id "buttons"
            :class "text-right"}
      [:button {:class    "btn btn-primary text-right admin-btn"
                :on-click #(do (.preventDefault %)
                               (open-admintool-modal item-id item-type))}
      (t :admin/Admintools)]]]))

(defn admin-gallery-badge [item-id item-type state init-data]
  (if (admin?)
    [:div
     [:a {:class    "bottom-link pull-right"
          :on-click #(do (.preventDefault %)
                         (open-admintool-modal "badges" item-id state init-data))}
      [:i {:class "fa fa-wrench"}]
      ;(t :admin/Admintools)
      ]]))

(defn admintool-gallery-page [item-id item-type state init-data user-id]
  (if (admin?)
    [:div
     [:a {:class    "bottom-link pull-right"
          :on-click #(do (.preventDefault %)
                         (open-admintool-modal "page" item-id state init-data))}
      [:i {:class "fa fa-wrench"}](t :admin/Admintools)]]))


(defn admintool-admin-page [item-id item-type state init-data]

(if (admin?)
    [:div
     [:div {:id "buttons"
            :class "text-right"}
      [:button {:class    "btn btn-primary text-right"
                :on-click #(do (.preventDefault %)
                               (open-admintool-modal item-type item-id state  init-data))}
      (t :admin/Admintools)]]]))
