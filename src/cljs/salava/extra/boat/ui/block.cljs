(ns salava.extra.boat.ui.block
  (:require [salava.core.i18n :refer [t]]))

(defn ^:export footer []
  [:div.footer
   [:div.footer-container
    [:p
     "droits d'auteur © 2019 BOAT"
     #_[:a {:class "bottom-link" :href "mailto:info@badgbl.nl"}
        ""]]
    [:p
     "Powered by Open Badge Factory ®. Open Badge Factory ® is a registered trademark"]]])
