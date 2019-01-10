(ns salava.social.ui.badge-message
  (:require [reagent.core :refer [atom cursor create-class dom-node props]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [clojure.string :refer [trim blank?]]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.user.ui.helper :refer [profile-picture]]
            [salava.core.ui.helper :refer [path-for current-path navigate-to input-valid? hyperlink not-activated?]]
            [salava.core.ui.notactivated :refer [not-activated-banner]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.social.ui.follow :as f]
            [salava.core.ui.modal :refer [set-new-view]]
            [salava.core.time :refer [date-from-unix-time]]))

(defn init-data
  ([state]
   (ajax/GET
     (path-for (str "/obpv1/social/messages/" (:badge_id @state) "/" (:page_count @state)))
     {:handler (fn [data]
                 (swap! state assoc
                        :messages (into (:messages @state) (:messages data))
                        :message ""
                        :page_count (inc (:page_count @state))
                        :messages_left (:messages_left data)))}))
  ([state other-ids]
   (ajax/GET
     (path-for (str "/obpv1/social/messages/" (:badge_id @state) "/" (:page_count @state)))
     {:params {:other_ids other-ids}
      :handler (fn [data]
                 (swap! state assoc
                        :messages (into (:messages @state) (:messages data))
                        :message ""
                        :page_count (inc (:page_count @state))
                        :messages_left (:messages_left data)
                        :other_ids other-ids))}
     )
   ))


(defn save-message [state reload-fn]
  (let [{:keys [message user_id badge_id]} @state]
    (ajax/POST
      (path-for (str "/obpv1/social/messages/" badge_id))
      {:response-format :json
       :keywords? true
       :params {:message message
                :user_id user_id}
       :handler (fn [data]
                  (do
                    (if (= "success" (:connected? data))
                      (do

                        (f/init-data badge_id)
                        (swap! state assoc :start-following true))
                      (swap! state assoc :start-following false))
                    (swap! state assoc
                           :messages []
                           :page_count 0)
                    (init-data state)
                    (reload-fn)
                    ))
       :error-handler (fn [{:keys [status status-text]}]
                        (dump (str status " " status-text)))})))

(defn delete-message [id state]
  (ajax/POST
    (path-for (str "/obpv1/social/delete_message/" id))
    {:response-format :json
     :keywords? true
     :handler (fn [data]
                (let [filtered-messages (filter #(not (= id (:id %))) (:messages @state))]
                  (swap! state assoc :messages filtered-messages)))
     :error-handler (fn [{:keys [status status-text]}]
                      (dump (str status " " status-text)))}))

(defn delete-message-button [id state]
  (let [delete-clicked (atom nil)]
    (fn []
      [:div.deletemessage
       [:button {:type       "button"
                 :class      "close"
                 :aria-label "OK"
                 :on-click   #(do
                                (reset! delete-clicked (if (= true @delete-clicked) nil true))
                                (.preventDefault %))}
        [:span {:aria-hidden "true"
                :dangerouslySetInnerHTML {:__html "&times;"}}]]
       (if @delete-clicked
         [:div
          [:div {:class "alert alert-warning"}
           (t :badge/Confirmdelete)]
          [:button {:type  "button"
                    :class "btn btn-primary"
                    :on-click #(reset! delete-clicked nil)}
           (t :badge/Cancel)]
          [:button {:type  "button"
                    :class "btn btn-warning"
                    :on-click     #(delete-message id state)}
           (t :badge/Delete)]])])))

(defn blank-reduce [a-seq]
  (let [str-space (fn [str1 str2]
                    (do
                      (let [current (conj str1 " ")]
                        (conj current str2))))]
    (reduce str-space ()  a-seq)))

(defn search-and-replace-www [text]
  (let [split-words (clojure.string/split text #" ")
        helper (fn [current item]
                 (if (or (re-find #"www." item) (re-find #"^https?://" item) (re-find #"^http?://" item))
                   (conj current (hyperlink item))
                   (conj current (str item))))]
    (blank-reduce (reduce helper () split-words))))

(defn message-list-item [{:keys [message first_name last_name ctime id profile_picture user_id]} state]
  [:div {:class "media message-item" :key id}
   [:div.msgcontent
    [:span {:class "pull-left"}
     [:img {:class "message-profile-img" :src (profile-picture profile_picture)}]]
    [:div {:class "media-body"}
     [:h4 {:class "media-heading"}
      [:a {:href "#"
           :on-click #(set-new-view [:user :profile] {:user-id user_id})} (str first_name " "last_name)]
      [:span.date (date-from-unix-time (* 1000 ctime) "minutes")]]
     (into [:div] (for [ item (clojure.string/split-lines message)]
                    (into [:p.msg] (if (or (re-find #"www." item) (re-find #"https?://" item) (re-find #"http?://" item))
                                     (search-and-replace-www item)
                                     item))))]]
   (if (or (=  user_id (:user_id @state)) (= "admin" (:user_role @state)))
     [delete-message-button id state])])

(defn message-list-load-more [state]
  (if (pos? (:messages_left @state))
    [:div {:class "media message-item"}
     [:div {:class "media-body"}
      [:span [:a {:href     "#"
                  :id    "loadmore"
                  :on-click #(do
                               (init-data state)
                               (.preventDefault %))}
              (str (t :social/Loadmore) " (" (:messages_left @state) " " (t :social/Messagesleft) ")")]]]]))


(defn scroll-bottom []
  (let [div (. js/document getElementById "message-list") ]
    (set! (. div -scrollTop) (. div -scrollHeight))))


(defn message-list [messages state]
  (create-class {:reagent-render (fn [messages]
                                   [:div {:id ""}
                                    (doall
                                      (for [item messages]
                                        (message-list-item item state)))
                                    (message-list-load-more state)])
                 ;:component-did-mount #(scroll-bottom)
                 ;:component-did-update #(scroll-bottom)
                 }))

(defn message-textarea [state reload-fn]
  (let [message-atom (cursor state [:message])]
    [:div
     [:div {:class "form-group"}
      [:textarea {:class    "form-control"
                  :rows     "5"
                  :value    @message-atom
                  :disabled (if (not-activated?) "disabled" "")
                  :onChange #(reset! message-atom (.-target.value %))} ]]
     [:div {:class "form-group"}
      [:button {:class    "btn btn-primary"
                :disabled (if (blank? @message-atom) "disabled" "")
                :on-click #(do
                             (save-message state reload-fn)
                             (.preventDefault %))}
       (t :social/Postnew)]]]))


(defn refresh-button [state]
  [:a {:href "#"
       :class "pull-right"
       :on-click #(do
                    (init-data state)
                    (.preventDefault %))} "Refresh"])

(defn start-following-alert [state]
  (let [{:keys [badge_id]} @state]
    [:div {:class (str "alert ""alert-success")}
     [:div.deletemessage
      [:button {:type       "button"
                :class      "close"
                :aria-label "OK"
                :on-click   #(do
                               (swap! state assoc :start-following false)
                               (.preventDefault %))}
       [:span {:aria-hidden "true"
               :dangerouslySetInnerHTML {:__html "&times;"}}]]]
     (str (t :social/Youstartedfollowbadge) "! ")
     [:a {:href "#"
          :on-click #(do
                       (f/unfollow-ajax-post badge_id)
                       (swap! state assoc :start-following false))}
      (t :social/Cancelfollowingbadge)]]))



(defn content [state reload-fn]
  (let [{:keys [messages start-following]} @state]
    [:div
     (if (not-activated?)
       (not-activated-banner))
     [message-textarea state reload-fn]
     (if start-following
       (start-following-alert state))
     [message-list messages state]]))


(defn badge-message-handler
  ([badge_id reload-fn]
   (let [state (atom {:messages []
                      :user_id (session/get-in [:user :id])
                      :user_role (session/get-in [:user :role])
                      :message ""
                      :badge_id badge_id
                      :show false
                      :page_count 0
                      :messages_left 0
                      :start-following false})
         reload-fn (or reload-fn (fn []))]

     (init-data state)
     (fn []
       (content state reload-fn))))
  ([badge_id reload-fn other-ids]
   (let [state (atom {:messages []
                      :user_id (session/get-in [:user :id])
                      :user_role (session/get-in [:user :role])
                      :message ""
                      :badge_id badge_id
                      :show false
                      :page_count 0
                      :messages_left 0
                      :start-following false})
         reload-fn (or reload-fn (fn []))]

     (if (empty? other-ids) (init-data state) (init-data state other-ids))
     (fn []
       (content state reload-fn)))
   ))
