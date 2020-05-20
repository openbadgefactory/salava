(ns salava.extra.cancred.ui.block
  (:require [salava.core.i18n :refer [t]]))

(defn ^:export footer []
  [:div.footer ;:footer.footer
   [:div.footer-container
    [:p.text-muted
     "Copyright © Learning Agents 2015-2019. | "
     [:a {:class "bottom-link" :href "/terms/"}
      "Terms of Use"]
     " | "
     [:a {:class "bottom-link" :href "/privacy/"}
      "Privacy Policy"]
     " | "
     [:a {:class "bottom-link" :href "mailto:info@cancred.ca?Subject=CanCred_Passport_contact"}
      "info@cancred.ca"]]
    [:p.text-muted
     "Powered by Open Badge Factory ®. Open Badge Factory ® is a registered trademark"]]])

(defn ^:export terms []
  [:div {:style {:padding-top "30px" :text-align "center"}}
   [:p "Please open and read these documents before continuing:"]
   [:p
    [:a {:href "https://passport.cancred.ca/terms/" :target "_blank"} "Terms of Use"]]
   [:p
    [:a {:href "https://passport.cancred.ca/privacy/" :target "_blank"} "Privacy and Cookie Policy"]]])
