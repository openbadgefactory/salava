(ns salava.core.ui.modal
  (:require [reagent.core :refer [create-class atom]]
            [reagent-modals.modals :as m :refer [close-modal!]]
            [salava.core.ui.dispatch :refer [site-navi]]))


(def views (atom []))


(defn set-new-view [route params]
  (reset! views (conj @views [((get-in site-navi route) params)])))





(defn modal-content [] 
  [:div {:id "badge-content"}
   [:div.modal-body
    [:div.row
     [:div.col-md-12
      (if (< 1 (count @views))
        [:div {:class "pull-left"}       
         [:button {:type  "button"
                   :class "close"
                   :aria-label "OK"
                   :on-click #(do
                                (reset! views (pop @views))
                                (.preventDefault %))}
          [:span {:aria-hidden             "true"
                  :dangerouslySetInnerHTML {:__html "&lt;"}}]]])
      [:div {:class "text-right"}       
       [:button {:type         "button"
                 :class        "close"
                 :data-dismiss "modal"
                 :aria-label   "OK"}
        [:span {:aria-hidden             "true"
                :dangerouslySetInnerHTML {:__html "&times;"}}]]]]]
    [:div (last @views)]]
   [:div.modal-footer
    
    ]])

(defn modal-init [view]
  (create-class {:component-will-mount   (fn [] (reset! views [view]))
                 :reagent-render         (fn [] (modal-content))
                 :component-will-unmount (fn [] (do (close-modal!)
                                                    (reset! views [])
                                                    #_(if (and init-data state)
                                                        (init-data state))))}))

(defn open-modal [route params]
  
  (m/modal! [modal-init [((get-in site-navi route) params)]] {:size :lg}))
