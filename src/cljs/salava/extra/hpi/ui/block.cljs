(ns salava.extra.hpi.ui.block
  (:require [salava.core.i18n :refer [t]]))


(defn ^:export footer []
  [:footer.footer
   [:div.footer-container
    [:p.text-muted
    "Copyright © 2016 Discendum Oy | "
     [:a {:class "bottom-link" :href "/terms" }
      "Terms of Use"]
     " | "
     [:a {:class "bottom-link" :href "mailto:kaya@humanitarian.academy?Subject=Contact%20request" }
      "Contact: kaya@humanitarian.academy"]]
    [:p.text-muted
     "Open Badge Factory ® and Open Badge Passport ® are registered trademarks"]]])
