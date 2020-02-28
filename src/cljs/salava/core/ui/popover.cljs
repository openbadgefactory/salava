(ns salava.core.ui.popover
  (:require
   [clojure.string :refer [blank?]]
   [reagent.core :refer [atom create-class]]
   [salava.core.i18n :refer [t]]))

;;pop-up placement options ["top" "bottom" "right" "left"]
(defn info [opts]
  (let [{:keys [content placement style]} opts]
    (create-class {:reagent-render (fn []
                                     [:a.popup-info {:tabIndex "0"
                                                     :data-toggle "popover"
                                                     :data-trigger "focus"
                                                     :data-content content
                                                     :data-placement placement
                                                     :href "#"
                                                     :aria-label content
                                                     :style style} [:i.fa.fa-question-circle.fa-sm]])
                   :component-did-mount (fn []
                                          (.getScript (js* "$") "/js/pop-over.js"))})))

(defn- toggle-alert [vatom]
  (if @vatom (reset! vatom false) (reset! vatom true)))


(defn about-page [opts]
  (let [{:keys [content heading style]} opts
        link-visible (atom true)
        heading (if (blank? heading)(t :core/Aboutthispage) heading)
        style (-> {:position "absolute"
                   :right "5px"
                   :top "125px"}
                  (merge style))]
      (fn []
        [:div
         (when @link-visible
           [:div.row {:style style}
            [:div.pull-right
             [:a.popup-info {:title (t :core/Aboutthispage)
                             :aria-label (t :core/Aboutthispage)
                             :on-click #(do
                                         (.preventDefault %)
                                         (toggle-alert link-visible))}
              [:i.fa.fa-question-circle.fa-3x]]]])

         (when-not @link-visible
          [:div
           [:div.page-info-box.alert.alert-dismissible
            [:button.close {:type "button"
                            :aria-label "Close"
                            :on-click #(do
                                         (.preventDefault %)
                                         (toggle-alert link-visible))}
             [:span {:aria-hidden true
                     :dangerouslySetInnerHTML {:__html "&times;"}}]]
            [:div
             [:h4.alert-heading heading]
             [:hr.border]
             [:p content]]]])])))
