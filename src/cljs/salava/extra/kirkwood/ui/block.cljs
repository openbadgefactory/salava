(ns salava.extra.kirkwood.ui.block
  (:require [salava.core.i18n :refer [t]]))


(defn ^:export footer []
  [:footer.footer
   [:div.footer-container
    [:p.text-muted
    "Copyright © 2015-2017 Discendum Oy | "
     [:a {:class "bottom-link" :href "/terms" }
      "Terms of Use"]
     " | "
     [:a {:class "bottom-link" :href "mailto:kevin.collier@g.kirkwoodschools.org?Subject=Contact%20request" }
      "Contact: kevin.collier@g.kirkwoodschools.org"]]
    [:p.text-muted
     "Open Badge Factory ® and Open Badge Passport ® are registered trademarks"]]])

(defn ^:export login_info []
  [:p "TODO login info goes here"])
