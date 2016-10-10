(ns salava.social.ui.stream
   (:require [salava.core.ui.layout :as layout]
             [salava.core.i18n :as i18n :refer [t]]
             [salava.core.ui.ajax-utils :as ajax]
             [salava.social.ui.badge-message-modal :refer [badge-message-stream-link]]
             [reagent-modals.modals :as m]
             [salava.core.ui.helper :as h :refer [unique-values navigate-to path-for]]))

(defn stream-item [subject verb object]
  [:div {:class "media" :id "ticket-container"}
   [:div.media-left
    [:a {:href "#"}
     [:img {:src "/file/6/7/4/9/6749ffef1b0fd701335769a5930874d6dd1c018b22d99cb4b2c572c548fdbdea.svg"} ]]]
   [:div.media-body
    [:h4 {:class "media-heading"}
     [:a {:href "#"} subject]
     (str  " " verb " ")
     object] 
    ]])

(defn content [state]
  [:div {:class "my-badges pages"}
   [m/modal-window]
   (stream-item "badge" "has" [badge-message-stream-link {:new-messages 4}  "3368c6f212f79433f2e0c912edb82ae36cf980fa1608fb22d2467769b980b9eb"] )
   ])

(defn init-data [state]
  (ajax/GET
    (path-for "/obpv1/page" true)
    {:handler (fn [data]
                (swap! state assoc :pages data))}))

(defn handler [site-navi]
  (let [state (atom "")]
    ;(init-data state)
    (fn []
      (layout/default site-navi (content state)))))
