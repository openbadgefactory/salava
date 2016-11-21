(ns salava.registerlink.ui.register-link
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [clojure.string :refer [trim]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.helper :refer [path-for]]
            [cljs-uuid-utils.core :refer [make-random-uuid uuid-string]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]))

(defn random-key []
  (-> (make-random-uuid)
      (uuid-string)))

(defn make-register-url []
  (str (session/get :site-url) "/user/register/token/" (random-key)))

(defn content [state]
  (let [url-atom (cursor state [:url])
        active-atom (cursor state [:active])]
    [:div {:class "col-xs-12"}
     [:h2 (t :admin/Register-link)]
     (if @active-atom
       [:div.form-group
        [:label 
         (str (t :admin/url) ":")]
        [:input {:class    "form-control"
                 :value    @url-atom
                 :onChange #(reset! url-atom (.-target.value %))
                 :disabled true}]])

     [:div.checkbox
      [:label
       [:input {:name      "visibility"
                :type      "checkbox"
                :on-change #(reset! active-atom (if @active-atom false true))
                :checked   @active-atom}]
       (t :user/Publishandshare)]]

     
     [:button {:class "btn btn-primary"
               :on-click #(do
                            (reset! url-atom (make-register-url))
                            (.preventDefault %))}
     (t :admin/Reset)]
     
     
     ]))

(defn init-data [state]
  (ajax/GET 
   (path-for "/obpv1/admin/stats")
   {:handler (fn [data]
               (reset! state data))}))

(defn handler [site-navi]
  (let [state (atom {:url (make-register-url)
                     :active false})]
    ;(init-data state)
    (fn []
      (layout/default site-navi (content state)))))
