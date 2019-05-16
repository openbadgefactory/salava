(ns salava.profile.ui.embed
 (:require [reagent.core :refer [atom cursor]]
           [salava.profile.ui.helper :as ph]
           [salava.core.ui.ajax-utils :as ajax]
           [salava.core.ui.helper :refer [path-for plugin-fun]]
           [salava.core.ui.layout :as layout]
           [reagent.session :as session]
           [reagent-modals.modals :as m]
           [salava.core.ui.badge-grid :refer [badge-grid-element]]
           [salava.profile.ui.modal :refer [userinfoblock]]
           [salava.core.i18n :refer [t]]))

(defn recent-badges [state]
  (let [block (first (plugin-fun (session/get :plugins) "block" "recentbadges"))]
    (if block [block state "embed"] [:div ""])))

(defn recent-pages [state]
  (let [block (first (plugin-fun (session/get :plugins) "block" "recentpages"))]
    (if block [block state "embed"] [:div ""])))

(defn showcase-grid [state block-atom]
 [:div#user-badges
  [:h3 (or (:title @block-atom) (t :page/Untitled))]
  [:div#grid {:class "row"}
   (reduce (fn [r b]
            (conj r (badge-grid-element b state "embed" nil))) [:div] (:badges @block-atom))]])

(defn block [block-atom state index]
  (let [type (:type @block-atom)]
    (when-not (:hidden @block-atom)
     [:div {:key index} (case type
                          ("badges") [recent-badges state]
                          ("pages") [recent-pages state]
                          ("showcase") [showcase-grid state block-atom]
                          ("location") (into [:div]
                                        (for [f (plugin-fun (session/get :plugins) "block" "user_profile")]
                                          [f (:user-id @state)]))
                         nil)])))

(defn view-profile [state]
  (let [blocks (cursor state [:blocks])]
    [:div#profile
     (if (= 0 @(cursor state [:active-index]))
      [:div#page-view
           [:div {:id (str "theme-" (or @(cursor state [:theme]) 0))
                  :class "page-content"}
            [:div.panel
             [:div.panel-left
              [:div.panel-right
               [:div.panel-content
                [:div.panel-body
                 [userinfoblock state]
                 (into [:div#profile]
                       (for [index (range (count @blocks))]
                         (block (cursor blocks [index]) state index)))]]]]]]]
      @(cursor state [:tab-content]))]))

(defn content [state]
 (let [tab @(cursor state [:tab-content])]
  [:div
   [m/modal-window]
   [:div#profile
    [ph/profile-navi state]
    (if (= (:active-index @state) 0) [view-profile state] tab)]]))

(defn init-data [id state]
 (ajax/GET
  (path-for (str "/obpv1/profile/" id) true)
  {:handler (fn [data]
             (swap! state assoc :permission "success" :active-index 0)
             (swap! state merge data))}))

(defn handler [site-navi params]
  (let [user-id (:user-id params)
        state (atom {:user-id user-id})

        user (session/get :user)]
    (init-data user-id state)
    (fn []
      (layout/embed-page (content state)))))
