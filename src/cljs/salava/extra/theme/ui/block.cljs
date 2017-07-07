(ns salava.extra.theme.ui.block
  (:require [salava.core.i18n :refer [t]]))


(defn ^:export footer []
  [:footer.footer
   [:div.footer-container
    [:p.text-muted
    "Copyright © 2015-2017 Discendum Oy | "
     [:a {:class "bottom-link" :href "/terms" }
      "Terms of Use"]
    " | "
     [:a {:class "bottom-link" :href "/privacy"}
      "Privacy Policy"]
     " | "
     [:a {:class "bottom-link" :href "mailto:contact@openbadgefactory.com?Subject=Contact%20request" }
      "contact@openbadgefactory.com"]]
    [:p.text-muted
     "Open Badge Factory ® and Open Badge Passport ® are registered trademarks"]]])
