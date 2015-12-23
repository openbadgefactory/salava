(ns salava.core.ui.rate-it
  (:require [reagent.core :refer [create-class]]
            [salava.core.i18n :refer [t]]))

(defn rate-it-stars [value read-only]
  [:div {:id "rateit"
         :class "rateit"
         :data-rateit-value value
         :data-rateit-readonly read-only}])

(defn rate-it
  ([value]
   (rate-it value nil))
  ([value value-atom]
   (create-class {:reagent-render      (fn []
                                         (rate-it-stars value (nil? value-atom)))
                  :component-did-mount (fn []
                                         (.getScript (js* "$") "/js/rateit/jquery.rateit.min.js")
                                         (if value-atom
                                           (-> (js* "$('#rateit')")
                                               (.bind "rated" (fn [e new-value]
                                                                (reset! value-atom new-value)))
                                               (.bind "reset" (fn []
                                                                (reset! value-atom nil))))))})))
