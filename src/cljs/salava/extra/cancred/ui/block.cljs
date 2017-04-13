(ns salava.extra.cancred.ui.block
  (:require [salava.core.i18n :refer [t]]))


(defn ^:export footer []
  [:footer.footer
   [:div.footer-container
    [:p.text-muted
    "Copyright © Learning Agents 2015-2017. | "
     [:a {:class "bottom-link" :href "/terms/" }
      "Terms of Use"]
    " | "
     [:a {:class "bottom-link" :href "/privacy/"}
      "Privacy Policy"]
     " | "
     [:a {:class "bottom-link" :href "mailto:info@cancred.ca?Subject=CanCred_Passport_contact" }
      "info@cancred.ca"]]
    [:p.text-muted
     "Powered by Open Badge Factory ®. Open Badge Factory ® is a registered trademark"]]])
