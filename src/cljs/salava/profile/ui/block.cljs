(ns salava.profile.ui.block
  (:require [salava.core.ui.helper :refer [base-path path-for plugin-fun hyperlink]]
            [reagent.core :refer [atom cursor]]
            [salava.core.ui.ajax-utils :as ajax]
            [reagent.session :as session]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.user.schemas :refer [contact-fields]]
            [clojure.string :refer [blank?]]
   [salava.core.helper :refer [dump]]))

(defn update-block-value [block-atom key value]
  (swap! block-atom assoc key value))

(defn init-user-profile [user-id state edit?]
  (ajax/GET
    (path-for (str "/obpv1/profile/" user-id) true)
    {:handler (fn [data] (if edit?
                          (swap! state merge data)
                          (reset! state data)))}))


(defn profile-picture [path]
  (let [picture-fn (first (plugin-fun (session/get :plugins) "helper" "profile_picture"))]
    (when picture-fn (picture-fn path))))

(defn enabled-field? [field fields]
 (some #(= % field) fields))

(defn process-field [field state]
 (let [fields (:fields @state)]
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
      [:div.col-xs-12 [:b "Select the profile fields you want to display in this page. Your profile picture is added by default"]]]
     [:div.row
      [:div {:class "col-md-3 col-sm-3 col-xs-12"}
       [:div.profile-picture-wrapper
        [:img.profile-picture {:src (profile-picture profile_picture)}]]]
      [:div.col-md-9
       [:div.col-xs-12
        [:div.row
         [:div {:style {:margin "10px 0"}}
          [:form
           [:div
            [:input {:type "checkbox" :value "name" :on-change #(process-field (.-target.value %) state)}]
            [:span {:style {:margin "0px 10px"}}
             [:label (t :admin/Name)": "]
             [:span {:style {:margin "0px 10px"}}  (str first_name " " last_name)]]]
           (when-not (blank? about)
            [:div
             [:input {:type "checkbox" :value "about" :on-change #(process-field (.-target.value %) state)}]
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
                             [:td {:style {:padding-left "0px"}} [:input {:type "checkbox" :value key :on-change #(process-field (.-target.value %) state)}]]
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
     (when-not data (init-user-profile id state false))
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
    [:div
     (dump tips)
     (when (some #(true? %) (vals (dissoc tips :tabs-tip))) [:div.col-xs-12.pending {:style {:margin "10px 2px"}} "Your profile is not complete"])
     [:div.progress.col-xs-12
      [:div.progress-bar.progress-bar-success
       {:role "progressbar"
        :aria-valuenow (str completion_percentage)
        :style {:width (str completion_percentage "%")}
        :aria-valuemin "0"
        :aria-valuemax "100"}
       (str completion_percentage "% complete")]]

     (reduce-kv (fn [r k v]
                 (conj r (when (true? v) [:div.col-xs-12.tip {:style {:margin "10px 0" :font-size "16px"}} [:i.fa.fa-fw-fa-lg.fa-lightbulb-o.icon](case k
                                                                                                                                                   :profile-picture-tip "Upload a profile picture to complete your profile"
                                                                                                                                                   :aboutme-tip "Tell us something about yourself to complete your profile"
                                                                                                                                                   :location-tip "Why don't you put yourself on the map"
                                                                                                                                                   :tabs-tip "Did you know you can enrich your profile by add pages as tabs")])))
                [:div] tips)]))))
