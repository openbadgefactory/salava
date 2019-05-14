(ns salava.page.ui.embed
  (:require [reagent.core :refer [atom cursor]]
            [ajax.core :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.helper :refer [navigate-to path-for]]
            [salava.page.ui.helper :as ph]
            [salava.core.ui.badge-grid :refer [badge-grid-element]]
            [salava.core.time :refer [date-from-unix-time]]))

(defn tag-block [block-atom]
 (let [{:keys [tag badges format sort]} block-atom
       container (case format
                  "short" [:div#grid {:class "row"}]
                  "long" [:div.tag-block])]

  [:div#user-badges
   [:div [:label (t :page/Tag ":")] (str " " tag)]
   [:div
     (let [sorted-badges (case sort
                           "name" (sort-by :name < badges)
                           "modified" (sort-by :mtime > badges)
                           badges)]
      (into container
       (for [badge sorted-badges]
         (if (= format "short")
          (badge-grid-element badge nil "embed" nil)
          (ph/badge-block (assoc badge :format "long"))))))]]))

(defn showcase-block [block-atom]
 (let [{:keys [badges format title]} block-atom
        container (case format
                    "short" [:div#grid {:class "row"}]
                    "long" [:div.tag-block])]
   [:div
    [:div.heading-block
     [:h2 title]]
    [:div#user-badges
     [:div
      (doall (reduce (fn [r badge]
                       (conj r (if (= format "short")
                                   (badge-grid-element badge nil "embed" nil)
                                   (ph/badge-block (assoc badge :format "long")))))
                     container badges))]]]))

(defn view-page [page]
  (let [{:keys [id name description mtime user_id first_name last_name blocks theme border padding visibility qr_code]} page]
    [:div {:id    (str "theme-" (or theme 0))
           :class "page-content"}
     (if id
       [:div.panel
        [:div.panel-left
         [:div.panel-right
          [:div.panel-content
           (if (and qr_code (= visibility "public"))
             [:div.row
              [:div {:class "col-xs-12 text-center"}
               [:img#print-qr-code {:src (str "data:image/png;base64," qr_code)}]]])
           (if mtime
             [:div.row
              [:div {:class "col-md-12 page-mtime"}
               (date-from-unix-time (* 1000 mtime))]])
           [:div.row
            [:div {:class "col-md-12 page-title"}
             [:h1 name]]]
           [:div.row
            [:div {:class "col-md-12 page-author"}
             [:a {:href "#" :on-click #(navigate-to (str "/profile/" user_id)) } (str first_name " " last_name)]]]
           [:div.row
            [:div {:class "col-md-12 page-summary"}
             description]]
           (into [:div.page-blocks]
                 (for [block blocks]
                   [:div {:class "block-wrapper"
                          :style {:border-top-width (:width border)
                                  :border-top-style (:style border)
                                  :border-top-color (:color border)
                                  :padding-top (str padding "px")
                                  :margin-top (str padding "px")}}
                    (case (:type block)
                      "badge" (ph/badge-block block)
                      "html" (ph/html-block block)
                      "file" (ph/file-block block)
                      "heading" (ph/heading-block block)
                      "tag" (tag-block block)
                      "showcase" (showcase-block block)
                      "profile" (ph/profile-block block)
                      nil)]))]]]])]))

(defn page-content [page state]
  (let [show-link-or-embed-atom (cursor state [:show-link-or-embed-code])]
    [:div {:id "page-view"}
     [view-page page]]))

(defn content [state]
  (let [page (:page @state)]
    [:div {:id "page-container"}
       [page-content page state]]))

(defn init-data [state id]
  (ajax/GET
    (path-for (str "/obpv1/page/view/" id))
    {:response-format :json
     :keywords?       true
     :handler         (fn [data]
                        (swap! state assoc :page (:page data) :ask-password (:ask-password data)))
     :error-handler   (fn [{:keys [status status-text]}])}))


(defn handler [site-navi params]
  (let [id (:page-id params)
        state (atom {:page {}
                     :page-id id
                     :show-link-or-embed-code nil})]
    (init-data state id)
    (fn []
      (layout/embed-page (content state)))))
