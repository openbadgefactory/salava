(ns salava.core.ui.modal
  (:require [reagent.core :refer [create-class atom]]
            [reagent.session :as session]
            [reagent-modals.modals :as m :refer [close-modal!]]
            [salava.core.ui.helper :refer [plugin-fun]]
            [salava.core.common :as common]
            [salava.core.helper :refer [dump]]
            [clojure.string :as str]
            [dommy.core :as dommy :refer-macros [sel1 sel]]))
            ;[salava.core.ui.dispatch :refer [site-navi]]


(defn modal-navi []
  (apply merge (plugin-fun (session/get :plugins) "modal" "modalroutes")))

(def views (atom []))

(defn set-new-view [route params]
  (reset! views (conj @views [((get-in (modal-navi) route) params)])))

(defn previous-view []
  (reset! views (pop @views)))

(defn accessibility-fix []
  (-> (sel1 ".modal")
      (dommy/set-attr! :aria-label "modal content" :aria-hidden true))
  (-> (sel1 ".modal-dialog")
      (dommy/set-attr! :role "document")))

(defn modal-content []
  [:div {:id "badge-content"}
   [:div.modal-header
    [:button {:type         "button"
              :class        "close"
              :data-dismiss "modal"
              :aria-label   "OK"}
     [:span {:aria-hidden             "true"
             :dangerouslySetInnerHTML {:__html "&times;"}}]]
    (if (< 1 (count @views))
      [:div {:class "pull-left"}
       [:button {:type  "button"
                 :class "close"
                 :aria-label "OK"
                 :on-click #(do
                              (reset! views (pop @views))
                              (.preventDefault %))}
        [:span {:class "back-arrow" :aria-hidden "true"}]]])]
   [:div.modal-body
    [:div (last @views)]]
   [:div.modal-footer]])

(defn modal-init [view]
  (create-class {:component-will-mount   (fn [] (do
                                                  (reset! views [view])
                                                  (accessibility-fix)))
                 :reagent-render         (fn [] (modal-content))
                 :component-will-unmount (fn [] (do (close-modal!)
                                                    (reset! views [])
                                                    #_(if (and init-data state)
                                                        (init-data state))))}))

(defn open-modal
  ([route params]
   (if (empty? @views)
     (m/modal! [modal-init [((get-in (modal-navi) route) params)]] {:size :lg})
     (set-new-view route params)))
  ([route params opts]
   (if (empty? @views)
     (m/modal! [modal-init [((get-in (modal-navi) route) params)]] (merge opts {:size :lg}))
     (set-new-view route params))))
