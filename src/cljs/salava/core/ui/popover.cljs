(ns salava.core.ui.popover
  (:require [reagent.core :refer [create-class]]
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
                                                     :style style} [:i.fa.fa-question-circle.fa-sm]])
                   :component-did-mount (fn []
                                          (.getScript (js* "$") "/js/pop-over.js"))})))
