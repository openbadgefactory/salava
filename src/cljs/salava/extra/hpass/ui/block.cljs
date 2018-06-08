(ns salava.extra.hpass.ui.block
  (:require [salava.core.i18n :refer [t]]))

(defn ^:export login_top []
  [:div.login-top-container
   [:div.green-banner]
   [:div.login_top
    [:h1
    "Welcome to myHPass"]
    [:hr]]]
  )

(defn ^:export login_bottom []
  [:div.login-bottom
   [:br]
   [:p [:b "Get Recognised. Build your professional profile. Advance your career."][:br]
    "myHPass is a free platform for you as a humanitarian or volunteer. Store your digital badges and share your skills, learning and experience. "]
   [:br]
   [:a {:href "http://hpass.org/"} "Learn more about HPass"]
   [:br]
   [:br]
   [:div.footer]
  ])

(defn ^:export footer []
  [:footer.footer
   [:div.footer-container
       [:p.text-muted
      "Copyright © 2018 Humanitarian Leadership Academy | "
     [:a {:class "bottom-link" :href "mailto:info@hpass.org" }
      "info@hpass.org"]
       " | "
     [:a {:class "bottom-link" :href "/terms" }
      "Privacy Policy and Terms of Use"]

    ]]])

(defn ^:export terms []
  [:div {:style {:padding "10px"}}
   [:div
    [:p {:style {:text-align "center"}} "Read our privacy policy from "  [:a {:href "https://hpass.org/privacy-policy/"} [:b "here"]]]]])

(defn ^:export accept-terms-string []
  "I have read and i agree to the privacy policy")
