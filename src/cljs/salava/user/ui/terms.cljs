(ns salava.user.ui.terms
  (:require
    [salava.core.i18n :refer [t]]
    [reagent.core :refer [atom cursor]]
    [reagent.session :as session]
    [clojure.string :as string]
    [salava.core.ui.layout :as layout]
    [salava.core.ui.ajax-utils :as ajax]
    [salava.core.helper :refer [dump]]
    [salava.user.ui.login :refer [follow-up-url]]
    [salava.core.ui.helper :refer [path-for current-path base-path js-navigate-to path-for private? plugin-fun]]))

(defn next-url []
  (let [[service _](-> (clojure.string/split js/window.location.search #"&"))
        service (-> (clojure.string/split service #"=") second)]
    (if (and service _)
     (str (follow-up-url) js/window.location.search)
     (follow-up-url))))

(defn toggle-accept-terms []
  (ajax/POST
    (path-for (str "/obpv1/user/accept_terms"))
    {:handler (fn [data]
                (when (= "success" (:status data))
                  (js-navigate-to (next-url))))}))

(defn accept-terms-form [state]
  [:div {:style {:text-align "center"}}
   [:fieldset.accept-terms-checkbox {:class "checkbox"}
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
              :on-click #(toggle-accept-terms)
              }
     (t :user/Login)]]])

(defn content [state]
  [:div
   [:div
    [:div {:id "lang-buttons" :style (if (or (string/blank? (layout/terms-and-conditions-fr)) (string/blank? (layout/terms-and-conditions))) {:display "none"})}
     [:ul
      [:li [:a {:href "#" :on-click #(swap! state assoc :content (layout/terms-and-conditions))} "EN"]]
      [:li [:a {:href "#" :on-click #(swap! state assoc :content (layout/terms-and-conditions-fr)) } "FR"]]]]]
   [:div.panel
    (:content @state)
    (accept-terms-form state)]])


(defn handler [site-navi params]
  (let [state (atom {:accept-terms "declined"
                     :user-id (js/parseInt (:user-id params))
                     :content (layout/terms-and-conditions)})]
    (fn []
      (layout/landing-page site-navi (content state)))))
