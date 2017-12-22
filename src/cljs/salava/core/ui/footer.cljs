(ns salava.core.ui.footer
  (:require [salava.core.i18n :refer [t]]))


(defn base-footer []
  [:footer.footer
   [:div.footer-container
     [:div
     [:button {:class "btn btn-primary" :on-click #(do (.preventDefault %)
                                                       (let [x (.-dir js/document)]
                                                       (js/console.log x)
                                                       (if (identical? x "rtl") (set! (.-dir js/document) "ltr") (set! (.-dir js/document) "rtl"))
                                                       ))} "RTL"]]
    [:p.text-muted
     "Open Badge Passport community edition"]
    [:p.text-muted
     [:a {:class "bottom-link" :href "http://salava.org/"}
      "Salava"]
     " | "
     [:a {:class "bottom-link" :href "https://github.com/discendum/salava" }
      "Github"]]]])


