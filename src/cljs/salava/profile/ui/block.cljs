(ns salava.profile.ui.block
  (:require [salava.core.ui.helper :refer [base-path path-for plugin-fun hyperlink]]
            [reagent.core :refer [atom cursor]]
            [salava.core.ui.ajax-utils :as ajax]
            [reagent.session :as session]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.user.schemas :refer [contact-fields]]
            [clojure.string :refer [blank?]]
            [salava.core.helper :refer [dump]]
            [reagent.session :as session]
            [salava.user.ui.helper :as uh :refer [profile-picture]]))

(defn update-block-value [block-atom key value]
 (swap! block-atom assoc key value))

(defn init-user-profile [user-id state edit?]
  (ajax/GET
    (path-for (str "/obpv1/profile/" user-id) true)
    {:handler (fn [data] (if edit?
                          (swap! state merge (dissoc data :blocks))
                          (swap! state merge data)))}))

(defn enabled-field? [field fields]
 (some #(= % field) fields))

(defn process-field [field state]
 (let [fields @(cursor state [:fields])]
  (if (enabled-field? field fields)
   (update-block-value state :fields (vec (remove #(= % field) fields)))
   (update-block-value state :fields (conj fields field)))))

(defn ^:export editprofileinfo [state]
 (let [user-id (session/get-in [:user :id])]
  (init-user-profile user-id state true?)
  (fn []
   (let [{:keys [user profile]} @state
         {:keys [role first_name last_name about profile_picture]} user]
    [:div {:id "profile" :style {:margin "10px auto"}}
     [:div.row {:style {:padding "15px 0"}}
      [:div.col-xs-12 [:b (t :profile/Profileblockfieldsinstruction)]]]
     [:div.row.flip
      [:div {:class "col-md-3 col-sm-3 col-xs-12"}
       [:div.profile-picture-wrapper
        [:img.profile-picture {:src (profile-picture profile_picture)}]]]
      [:div.col-md-9
       [:div.col-xs-12
        [:div.row
         [:div {:style {:margin "10px 0"}}
          [:form
           [:div
            [:input {:id "name" :type "checkbox" :value "name" :on-change #(process-field (.-target.value %) state) :checked (if (enabled-field? "name" (:fields @state)) true false)}]

            [:span {:style {:margin "0px 10px"}}
             [:label (t :admin/Name)": "]
             [:span {:style {:margin "0px 10px"}}  (str first_name " " last_name)]]]
           (when-not (blank? about)
            [:div
             [:input {:type "checkbox" :value "about" :on-change #(process-field (.-target.value %) state) :checked (if (enabled-field? "about" @(cursor state [:fields])) true false)}]
             [:span {:style {:margin "0px 10px"}}
              [:label (t :user/Aboutme) ": "]
              [:span {:style {:margin "0px 10px"}} about]]])
           (when (seq profile)
               [:div.row {:style {:margin-top "20px"}}
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
                             [:td {:style {:padding-left "0px"}} [:input {:type "checkbox" :value field :on-change #(process-field (.-target.value %) state) :checked (if (enabled-field? field (:fields @state)) true false)}]]
                             [:td.profile-field (t key) ":"]
                             [:td.field-value (cond
                                               (or (re-find #"www." (str value)) (re-find #"^https?://" (str value)) (re-find #"^http?://" (str value))) (hyperlink value)
                                               (and (re-find #"@" (str value)) (= "twitter" field)) [:a {:href (str "https://twitter.com/" value) :target "_blank" } (t value)]
                                               (and (re-find #"@" (str value)) (= "email" field)) [:a {:href (str "mailto:" value)} (t value)]
                                               (and  (empty? (re-find #" " (str value))) (= "facebook" field)) [:a {:href (str "https://www.facebook.com/" value) :target "_blank" } (t value)]
                                               (= "twitter" field) [:a {:href (str "https://twitter.com/" value) :target "_blank" } (t value)]
                                               (and  (empty? (re-find #" " (str value))) (= "pinterest" field)) [:a {:href (str "https://www.pinterest.com/" value) :target "_blank" } (t value)]
                                               (and  (empty? (re-find #" " (str value))) (= "instagram" field)) [:a {:href (str "https://www.instagram.com/" value) :target "_blank" } (t value)]
                                               (= "blog" field) (hyperlink value)
                                               :else (t value))]])))]]])]]]]]]]))))



(defn ^:export userprofileinfo [id data]
   (let [state (if data data (atom {}))]
     (init-user-profile id state false)
    (fn []
      (let [{:keys [user profile fields]} @state
            {:keys [role first_name last_name about profile_picture]} user]
        [:div {:id "profile" :style {:margin "10px auto"}}
         [:div.row.flip
          [:div {:class "col-md-3 col-sm-3 col-xs-12"}
           [:div.profile-picture-wrapper
            [:img.profile-picture {:src (profile-picture profile_picture)}]]]

          [:div.col-md-9
           [:div.col-xs-12
            [:div.row
              (when (enabled-field? "name" (:fields @state)) [:div {:style {:margin "10px 0"}}
                                                              [:label (t :admin/Name)]
                                                              [:div (str first_name " " last_name)]])

             (and (when-not (blank? about) (enabled-field? "about" (:fields @state)))
                [:div {:style {:margin "15px 0"}}
                 [:label (t :user/Aboutme)]
                 [:div about]])
             (when-not (->> profile
                        (map :field)
                        (filter (fn [b] (some #(= b %) (:fields @state))))
                        empty?)
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
                            (when (and (enabled-field? field (:fields @state))(not (blank? value)))
                              [:tr
                               [:td.profile-field (t key) ":"]
                               [:td.field-value (cond
                                                 (or (re-find #"www." (str value)) (re-find #"^https?://" (str value)) (re-find #"^http?://" (str value))) (hyperlink value)
                                                 (and (re-find #"@" (str value)) (= "twitter" field)) [:a {:href (str "https://twitter.com/" value) :target "_blank" } (t value)]
                                                 (and (re-find #"@" (str value)) (= "email" field)) [:a {:href (str "mailto:" value)} (t value)]
                                                 (and  (empty? (re-find #" " (str value))) (= "facebook" field)) [:a {:href (str "https://www.facebook.com/" value) :target "_blank" } (t value)]
                                                 (= "twitter" field) [:a {:href (str "https://twitter.com/" value) :target "_blank" } (t value)]
                                                 (and  (empty? (re-find #" " (str value))) (= "pinterest" field)) [:a {:href (str "https://www.pinterest.com/" value) :target "_blank" } (t value)]
                                                 (and  (empty? (re-find #" " (str value))) (= "instagram" field)) [:a {:href (str "https://www.instagram.com/" value) :target "_blank" } (t value)]
                                                 (= "blog" field) (hyperlink value)
                                                 :else (t value))]])))]]])]]]]]))))

(defn get-profile-tips [state]
 (ajax/GET
  (path-for "/obpv1/profile/user/tips")
  {:handler (fn [data] (reset! state data))}))


(defn ^:export profiletips []
 (let [state (atom {})]
  (get-profile-tips state)
  (fn []
   (let [{:keys [tips completion_percentage]} @state]
    [:div#profiletips
     [:div.progress.col-xs-12 {:style {:margin-bottom "10px"}}
      [:div.progress-bar.progress-bar-success
       {:role "progressbar"
        :aria-valuenow (str completion_percentage)
        :style {:width (str completion_percentage "%")}
        :aria-valuemin "0"
        :aria-valuemax "100"}
       (str completion_percentage "% " (t :profile/Complete))]]
     (when (some #(true? %) (vals tips)) [:div.col-xs-12 {:style {:margin "5px 2px" :font-size "14px" :font-weight "bold"}} (t :profile/Tipstoimproveprofile)])
     (reduce-kv (fn [r k v]
                 (conj r (when (true? v)
                          [:div.col-xs-12.tip ;{:style {:margin "10px 0" :font-size "14px"}}
                           [:i.fa.fa-fw.fa-lightbulb-o.tipicon](case k
                                                                  :profile-picture-tip  [:a {:href (path-for (str "/profile/" (session/get-in [:user :id])))
                                                                                             :on-click #(do
                                                                                                         (.preventDefault %)
                                                                                                         (session/put! :edit-mode true))}  (t :profile/Profilepicturetip)]
                                                                  :aboutme-tip          [:a {:href (path-for (str "/profile/" (session/get-in [:user :id])))
                                                                                             :on-click #(do
                                                                                                         (.preventDefault %)
                                                                                                         (session/put! :edit-mode true))} (t :profile/Aboutmetip)]
                                                                  :location-tip         [:a {:href (path-for "/user/edit")}(t :profile/Locationtip)]
                                                                  :tabs-tip             [:a {:href (path-for (str "/profile/" (session/get-in [:user :id])))
                                                                                             :on-click #(do
                                                                                                         (.preventDefault %)
                                                                                                         (session/put! :edit-mode true))} (t :profile/Tabstip)]
                                                                  :showcase-tip         [:a {:href (path-for (str "/profile/" (session/get-in [:user :id])))
                                                                                               :on-click #(do
                                                                                                           (.preventDefault %)
                                                                                                           (session/put! :edit-mode true))} (t :profile/Showcasetip)])])))
                [:div] tips)]))))
