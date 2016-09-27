(ns salava.social.ui.badge-message-modal
  (:require [reagent.core :refer [atom create-class]]
            [reagent-modals.modals :as m]
            [reagent-modals.modals :refer [close-modal!]]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.rate-it :as r]
            [salava.core.helper :refer [dump]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for]]
            [salava.social.ui.badge-message :refer [badge-message-handler]]))



(defn modal-content [{:keys [badge public_users private_user_count]}]
  (let [{:keys [badge_content_id name image_file message_count]} badge
        all-messages (str (t :social/Messages)  " (" (:all-messages message_count) ") ")
        new-messages (if (pos? (:new-messages message_count))
                       (str (:new-messages message_count) " " (t :social/Newmessages ))
                       "")
        all-messages (str all-messages new-messages)]
    (fn []
      [:div {:id "badge-contents"}
       [:div.row
        [:div {:class "col-md-3 badge-image modal-left"}
         [:img {:src (str "/" image_file)}]
         ]
        [:div {:class "col-md-9 badge-info"}
         [:div.row
          [:h1.uppercase-header (str name " - " (t :social/Messages))]
          [badge-message-handler badge_content_id]]]]])))

(defn badge-content-modal-render [data]
  [:div {:id "badge-content"}
   [:div.modal-body
    [:div.row
     [:div.col-md-12
      [:button {:type         "button"
                :class        "close"
                :data-dismiss "modal"
                :aria-label   "OK"}
       [:span {:aria-hidden             "true"
               :dangerouslySetInnerHTML {:__html "&times;"}}]]]]
    [modal-content data]]
   [:div.modal-footer
    [:button {:type         "button"
              :class        "btn btn-primary"
              :data-dismiss "modal"}
     (t :core/Close)]]])

(defn badge-message-content-modal [modal-data]
  (create-class {:reagent-render (fn [] (badge-content-modal-render modal-data))
                 :component-will-unmount (fn [] (close-modal!))}))


(defn open-modal [badge-content-id]
  
  (ajax/GET
     (path-for (str "/obpv1/gallery/public_badge_content/" badge-content-id))
     {:handler (fn [data]
                 (do
                   (m/modal! [badge-message-content-modal data] {:size :lg})))}))




(defn badge-message-link [message-count badge-content-id]
  [:a {:href     "#"
       :on-click #(do
                    (open-modal badge-content-id)
                    (.preventDefault %) )}
   (str (t :social/Messages)  " (" (:all-messages message-count) ") "  )
   (if (pos? (:new-messages message-count))
     (str (:new-messages message-count) " " (t :social/Newmessages )))])
