(ns salava.badge.ui.stats
  (:require [reagent.core :refer [atom cursor]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.i18n :refer [t]]
            [salava.core.time :refer [date-from-unix-time]]))

(defn views-panel [views visible-area-atom]
  (let [panel-identity :views
        total-views (reduce #(+ %1 (:reg_count %2) (:anon_count %2)) 0 views)]
    [:div.panel
     [:div.panel-heading
      [:h3
       [:a {:href "#" :on-click #(do (.preventDefault %) (reset! visible-area-atom panel-identity))} (t :badge/Badgeviews) ":"] " (" total-views ")"]]
     (if (= @visible-area-atom panel-identity)
       [:div.panel-body
        [:table.table
         [:thead
          [:tr
           [:th]
           [:th]
           [:th (t :badge/Loggedinusers)]
           [:th (t :badge/Anonymoususers)]
           [:th (t :badge/Latestview)]]]
         (into [:tbody]
               (for [badge-views views
                     :let [{:keys [id name image_file reg_count anon_count latest_view]} badge-views]]
                 [:tr
                  [:td [:img.badge-icon {:src (str "/" image_file)}]]
                  [:td.name [:a {:href (path-for (str "/badge/info/" id))} name]]
                  [:td reg_count]
                  [:td anon_count]
                  [:td (if latest_view (date-from-unix-time (* 1000 latest_view)))]]))]])]))

(defn congratulations-panel [congratulations visible-area-atom]
  (let [panel-identity :congratulations
        total-congratulations (->> congratulations (map :congratulation_count) (reduce +))]
    [:div.panel
     [:div.panel-heading
      [:h3
       [:a {:href "#" :on-click #(do (.preventDefault %) (reset! visible-area-atom panel-identity))} (t :badge/Congratulations) ":"] " (" total-congratulations ")"]]
     (if (= @visible-area-atom panel-identity)
       [:div.panel-body
        [:table.table
         [:thead
          [:tr
           [:th]
           [:th]
           [:th (t :badge/Congratulations)]
           [:th (t :badge/Latestcongratulation)]]]
         (into [:tbody]
               (for [badge-congrats congratulations
                     :let [{:keys [id name image_file congratulation_count latest_congratulation]} badge-congrats]]
                 [:tr
                  [:td [:img.badge-icon {:src (str "/"  image_file)}]]
                  [:td.name [:a {:href (path-for (str "/badge/info/" id))} name]]
                  [:td congratulation_count]
                  [:td (if latest_congratulation (date-from-unix-time (* 1000 latest_congratulation)))]]))]])]))

(defn issuers-panel [issuers visible-area-atom]
  (let [panel-identity :issuers]
    [:div.panel
     [:div.panel-heading
      [:h3
       [:a {:href "#" :on-click #(do (.preventDefault %) (reset! visible-area-atom panel-identity))} (t :badge/Issuers) ":"] " (" (count issuers) ")"]]
     (if (= @visible-area-atom panel-identity)
       (into [:div.panel-body]
             (for [issuer issuers
                   :let [{:keys [issuer_content_name issuer_content_url badges]} issuer]]
               [:div.issuer-badges-wrapper
                [:h4 [:a {:href issuer_content_url :target "_blank"} issuer_content_name] ": " (count badges)]
                (into [:div.issuer-badges]
                      (for [badge badges
                            :let [{:keys [id image_file name]} badge]]
                        [:a {:href (path-for (str "/badge/info/" id))}
                         [:img.badge-icon {:src (str "/"  image_file) :title name :alt name}]]))])))]))

(defn content [state]
  (let [{:keys [badge_count expired_badge_count badge_views badge_congratulations badge_issuers]} @state
        visible-area-atom (cursor state [:visible_area])]
    [:div {:id "badge-stats"}
     [:h1.uppercase-header
      (t :badge/Badgestatistics)]
     [:h3 (t :badge/Totalbadges) ": " badge_count " " (t :badge/Expired) ": " expired_badge_count]
     [views-panel badge_views visible-area-atom]
     [congratulations-panel badge_congratulations visible-area-atom]
     [issuers-panel badge_issuers visible-area-atom]]))

(def initial-state {:visible_area :views
                    :badge_count nil
                    :expired_badge_count nil
                    :badge_views []
                    :badge_congratulations []
                    :badge_issuers []})

(defn init-data [state]
  (ajax/GET
    (path-for "/obpv1/badge/stats" true)
    {:handler (fn [data]
                (reset! state (merge initial-state data)))}))

(defn handler [site-navi]
  (let [state (atom {})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))