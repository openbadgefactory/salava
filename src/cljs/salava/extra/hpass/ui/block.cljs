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
   [:a {:href ""} "Learn more about HPass"]
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

#_(defn ^:export footer []
  [:footer.footer
   [:div.footer-container
    [:p.text-muted
    "Copyright © 2015-2017 Discendum Oy | "
     [:a {:class "bottom-link" :href "/terms" }
      "Terms of Use"]
     " | "
     [:a {:class "bottom-link" :href "mailto:kaya@humanitarian.academy?Subject=Contact%20request" }
      "Contact: kaya@humanitarian.academy"]]
    [:p.text-muted
     "Open Badge Factory ® and Open Badge Passport ® are registered trademarks"]]])
