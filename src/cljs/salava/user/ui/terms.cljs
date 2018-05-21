(ns salava.user.ui.terms
  (:require
    [salava.core.i18n :refer [t]]
    [reagent.core :refer [atom cursor]]
    [reagent.session :as session]
    [salava.core.ui.layout :as layout]
    [salava.core.ui.ajax-utils :as ajax]
    [salava.core.helper :refer [dump]]
    [salava.core.ui.helper :refer [path-for current-path base-path js-navigate-to path-for private? plugin-fun]]
    #_[salava.core.ui.terms :refer [default-terms default-terms-fr]]))

(defn toggle-accept-terms [state]
  (let [ user-id (:user-id @state)
         accept-terms (:accept-terms @state)]
    (ajax/POST
      (path-for (str "/obpv1/user/accept_terms"))
      {:params {:accept_terms accept-terms :user_id user-id}
       :handler (fn [data]
                  (when (and (= "success" (:status data)) (= "accepted" (:input data)))
                    (js-navigate-to "social/stream"#_(follow-up-url))))})))

(defn accept-terms-form [state]
  [:div {:style {:text-align "center"}}
   [:fieldset {:class "checkbox"}
    [:div [:label
           [:input {:type     "checkbox"
                    :on-change (fn [e]
                                 (if (.. e -target -checked)
                                   (swap! state assoc :accept-terms "accepted")(swap! state assoc :accept-terms "declined")
                                   ))}]
           (t :user/Doyouaccept)]]]
   [:div {:style {:text-align "center"}}
    [:button {:type         "button"
              :class        "btn btn-primary"
              :disabled     (if-not (= (:accept-terms @state) "accepted") "disabled")
              :on-click #(toggle-accept-terms state)
              }
     (t :user/Login)]]])

(defn get-user-id [url]
  (if-let [match (re-find #"id=([\w-]+)" url) ]
    (second match))
  )

(defn content [state]
  [:div
   [:div
    [:div {:id "lang-buttons"}
     [:ul
      [:li [:a {:href "#" :on-click #(swap! state assoc :content (layout/terms-and-conditions) #_(default-terms))} "EN"]]
      [:li [:a {:href "#" :on-click #(swap! state assoc :content (layout/terms-and-conditions-fr)) } "FR"]]]
     ]
    ]
   [:div.panel
    (:content @state)
    (accept-terms-form state)]]
  )


(defn handler [site-navi params]
  (let [state (atom {:accept-terms "declined"
                     :user-id (js/parseInt (:user-id params))
                     :content (layout/terms-and-conditions)})]
    (fn []
      (layout/landing-page site-navi (content state)))))
