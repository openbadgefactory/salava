(ns salava.location.ui.modal
  (:require [salava.core.ui.modal :as mo]))


(defn user-handler [params]
  (fn []
    [:table.table
     (->> params
          :users
          (sort-by :first_name)
          (map (fn [{:keys [id user_image first_name last_name]}]
                 [:tr
                  [:td {:width 50} [:img {:src user_image :width 40 :height 40}]]
                  [:td  [:p {:style {:padding-top "10px"}}
                         [:a {:href "#" :on-click #(mo/open-modal [:user :profile] {:user-id id})} first_name " " last_name]]]]))
          (into [:tbody]))
      ]))

(defn badge-handler [params]
  (fn []
    [:table.table
     (->> params
          :badges
          (reduce (fn [coll v] (assoc coll (:badge_id v) v)) {})
          vals
          (sort-by :badge_name)
          (map (fn [{:keys [badge_id badge_image badge_name]}]
                 [:tr
                  [:td {:width 50} [:img {:src badge_image :width 40 :height 40}]]
                  [:td  [:p {:style {:padding-top "10px"}}
                         [:a {:href "#" :on-click #(mo/open-modal [:gallery :badges] {:badge-id badge_id})} badge_name]]]]))
          (into [:tbody]))
      ]))

(def ^:export modalroutes
  {:location {:badgelist badge-handler
              :userlist  user-handler}})
