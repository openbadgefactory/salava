(ns salava.core.ui.footer
  (:require [salava.core.i18n :refer [t]]))


(defn base-footer []
  [:div.footer
   [:div.footer-container
    [:p.text-muted
     "Open Badge Passport community edition"]
    [:p.text-muted
     [:a {:class "bottom-link" :href "http://salava.org/"}
      "Salava"]
     " | "
     [:a {:class "bottom-link" :href "https://github.com/discendum/salava" }
      "Github"]]]])
