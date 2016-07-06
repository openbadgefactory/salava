(ns salava.core.ui.footer
  (:require [salava.core.i18n :refer [t]]))


(defn footer []
  [:footer.footer
   [:div.footer-container
    [:p.text-muted
    "Copyright © 2016 Discendum Oy | "
     [:a {:class "bottom-link" :href "/terms" }
      "Terms of Use"]
    " | "
     [:a {:class "bottom-link" :href "/privacy"}
      "Privacy Policy"]
     " | "
     [:a {:class "bottom-link" :href "mailto:contact@openbadgefactory.com?Subject=Contact%20request" }
      "contact@openbadgefactory.com"]]
    [:p.text-muted
     "Open Badge Factory ® is a registered trademark"]]])
