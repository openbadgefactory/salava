(ns salava.extra.msftembo.ui.block
  (:require [salava.core.i18n :refer [t]]))

(defn ^:export login_top
  "Content inserted above login box"
  []
  [:div {:key "msftembo"}
   ; ...
   ])

(defn ^:export login_bottom
  "Content inserted below login box"
  []
  [:div {:key "msftembo"}
   ; ...
   ])

(defn ^:export footer []
  [:footer.footer
   [:div.footer-container
    [:p.text-muted
    "Copyright © 2015-2018 Discendum Oy | "
     [:a {:class "bottom-link" :href "/terms" }
      "Terms of Use"]
     " | "
     [:a {:class "bottom-link" :href "mailto:msftembo@example.com?Subject=Contact%20request" }
      "Contact: msftembo@example.com"]]
    [:p.text-muted
     "Open Badge Factory ® and Open Badge Passport ® are registered trademarks"]]])
