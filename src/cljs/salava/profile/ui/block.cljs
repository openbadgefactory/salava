(ns salava.profile.ui.block
  (:require [salava.core.ui.helper :refer [base-path path-for plugin-fun hyperlink]]
            [reagent.core :refer [atom]]
            [salava.core.ui.ajax-utils :as ajax]
            [reagent.session :as session]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.user.schemas :refer [contact-fields]]
            [clojure.string :refer [blank?]]))

(defn init-user-profile [user-id state]
  (ajax/GET
    (path-for (str "/obpv1/profile/" user-id) true)
    {:handler (fn [data] (reset! state data))}))


(defn profile-picture [path]
  (let [picture-fn (first (plugin-fun (session/get :plugins) "helper" "profile_picture"))]
    (when picture-fn (picture-fn path))))

(defn ^:export userprofileinfo [data]
  (let [state (if data data (atom {}))
        id (session/get-in [:user :id])]
    (when-not data (init-user-profile id state))
    (fn []
      (let [{:keys [user profile]} @state
            {:keys [role first_name last_name about profile_picture]} user]
        [:div {:id "profile" :style {:margin "10px auto"}}
         [:div.row
          [:div {:class "col-md-3 col-sm-3 col-xs-12"}
           [:div.profile-picture-wrapper
            [:img.profile-picture {:src (profile-picture profile_picture)}]]]

          [:div.col-md-9
           [:div.col-xs-12
            [:div.row
              [:div {:style {:margin "10px 0"}}
               [:label (t :admin/Name)]
               [:div (str first_name " " last_name)]]

             (when-not (blank? about)
               [:div.col-xs-12
                [:label (t :user/Aboutme)]
                [:div about]])
             (when (seq profile)
                 [:div.row
                  [:div.col-xs-12 [:b (t :profile/Additionalinformation)]]
                  [:div.col-xs-12
                   [:table.table
                    (into [:tbody]
                          (for [profile-field (sort-by :order profile)
                                :let [{:keys [field value]} profile-field
                                      key (->> contact-fields
                                               (filter #(= (:type %) field))
                                               first
                                               :key)]]
                            (when-not (blank? value)
                              [:tr
                               [:td.profile-field (t key) ":"]
                               [:td (cond
                                      (or (re-find #"www." (str value)) (re-find #"^https?://" (str value)) (re-find #"^http?://" (str value))) (hyperlink value)
                                      (and (re-find #"@" (str value)) (= "twitter" field)) [:a {:href (str "https://twitter.com/" value) :target "_blank" } (t value)]
                                      (and (re-find #"@" (str value)) (= "email" field)) [:a {:href (str "mailto:" value)} (t value)]
                                      (and  (empty? (re-find #" " (str value))) (= "facebook" field)) [:a {:href (str "https://www.facebook.com/" value) :target "_blank" } (t value)]
                                      (= "twitter" field) [:a {:href (str "https://twitter.com/" value) :target "_blank" } (t value)]
                                      (and  (empty? (re-find #" " (str value))) (= "pinterest" field)) [:a {:href (str "https://www.pinterest.com/" value) :target "_blank" } (t value)]
                                      (and  (empty? (re-find #" " (str value))) (= "instagram" field)) [:a {:href (str "https://www.instagram.com/" value) :target "_blank" } (t value)]
                                      (= "blog" field) (hyperlink value)
                                      :else (t value))]])))]]])]]]]]))))
