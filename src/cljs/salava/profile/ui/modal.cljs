(ns salava.profile.ui.modal
 (:require [salava.core.ui.ajax-utils :as ajax]
  [reagent.core :refer [atom cursor]]
  [reagent.session :as session]
  [salava.core.ui.error :as err]
  [salava.core.ui.helper :refer [path-for hyperlink]]
  [salava.profile.ui.block :as b]
  [salava.core.i18n :refer [t]]
  [salava.profile.schemas :refer [additional-fields]]
  [salava.profile.ui.helper :as ph]
  [salava.admin.ui.reporttool :refer [reporttool1]]))


(defn userinfoblock [state]
 (let [{badges :badges pages :pages owner? :owner? {first_name :first_name last_name :last_name profile_picture :profile_picture about :about} :user profile :profile user-id :user-id} @state
        fullname (str first_name " " last_name)]
  [:div.row
   [:div.col-xs-12
       [:h1.uppercase-header {:style {:text-align "center"}} fullname]
       [:div.row.flip
        [:div {:class "col-md-3 col-sm-3 col-xs-12"}
         [:div.profile-picture-wrapper
          [:img.profile-picture {:src (b/profile-picture profile_picture)
                                 :alt fullname}]]]
        [:div {:class "col-md-9 col-sm-9 col-xs-12"}
         (if (not-empty about)
           [:div {:class "row about"}
            [:div.col-xs-12 [:b (t :user/Aboutme) ":"]]
            [:div.col-xs-12 about]])
         (if (not-empty profile)
           [:div.row
            [:div.col-xs-12 [:b (t :profile/Additionalinformation) ":"]]
            [:div.col-xs-12
             [:table.table
              (into [:tbody]
                    (for [profile-field (sort-by :order profile)
                          :let          [{:keys [field value]} profile-field
                                         key (->> additional-fields
                                                  (filter #(= (:type %) field))
                                                  first
                                                  :key)]]
                      [:tr
                       [:td.profile-field (t key) ":"]
                       [:td.field-value (cond
                                         (or (re-find #"www." (str value)) (re-find #"^https?://" (str value)) (re-find #"^http?://" (str value))) (hyperlink value)
                                         (and (re-find #"@" (str value)) (= "twitter" field))                                                      [:a {:href (str "https://twitter.com/" value) :target "_blank" } (t value)]
                                         (and (re-find #"@" (str value)) (= "email" field))                                                        [:a {:href (str "mailto:" value)} (t value)]
                                         (and  (empty? (re-find #" " (str value))) (= "facebook" field))                                           [:a {:href (str "https://www.facebook.com/" value) :target "_blank" } (t value)]
                                         (= "twitter" field)                                                                                       [:a {:href (str "https://twitter.com/" value) :target "_blank" } (t value)]
                                         (and  (empty? (re-find #" " (str value))) (= "pinterest" field))                                          [:a {:href (str "https://www.pinterest.com/" value) :target "_blank" } (t value)]
                                         (and  (empty? (re-find #" " (str value))) (= "instagram" field))                                          [:a {:href (str "https://www.instagram.com/" value) :target "_blank" } (t value)]
                                         (= "blog" field)                                                                                          (hyperlink value)
                                         :else                                                                                                     (t value))]]))]]])]]]]))

(defn view-profile [state]
 (let [blocks (cursor state [:blocks])
       {badges :badges pages :pages owner? :owner? {first_name :first_name last_name :last_name profile_picture :profile_picture about :about} :user profile :profile user-id :user-id} @state
       fullname (str first_name " " last_name)]
   [:div {:id "profile"}
    (if-not owner?
      [:div.col-xs-12 [:div.pull-right
                       (ph/connect-user user-id)]])
    [:div#page-view
       [:div {:id (str "theme-" (or @(cursor state [:theme]) 0))
              :class "page-content"}
             [:div.panel-left
              [:div.panel-right
               [:div.panel-content
                 [:div.panel-body
                  [userinfoblock state]
                  (into [:div#profile]
                        (for [index (range (count @blocks))]
                          [:div.row
                           [:div.col-xs-12 (ph/block (cursor blocks [index]) state index)]]))]]]]]]
    [:div.col-xs-12 {:style {:margin-top "10px"}} [reporttool1 user-id fullname "user"]]]))



(defn content [state]
 (let [tab @(cursor state [:tab-content])]
  [:div#profile
   [ph/profile-navi state]
   (if (= (:active-index @state) 0) [view-profile state] tab)]))

(defn init-data [id state]
 (ajax/GET
  (path-for (str "/obpv1/profile/" id) true)
  {:handler (fn [data]
             (swap! state assoc :permission "success")
             (swap! state merge data))}))


(defn handler [params]
  (let [user-id (:user-id params)
        state (atom {:user-id (:user-id params)
                     :permission "initial"
                     :badge-small-view false
                     :pages-small-view true
                     :active-index 0})
        user (session/get :user)]
    (init-data user-id state)
    (fn []
      (cond
        (= "initial" (:permission @state)) [:div ""]
        (and user (= "error" (:permission @state)))(err/error-content)
        (= "error" (:permission @state)) (err/error-content)
        (= (:id user) (js/parseInt user-id)) (content state)
        (and (= "success" (:permission @state)) user)(content state)
        :else (content state)))))

(def ^:export modalroutes
 {:profile {:view handler
            :blocktype ph/contenttype}})
