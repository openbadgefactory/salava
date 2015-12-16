(ns salava.badge.ui.helper
  (:require [salava.core.i18n :refer [t]]
            [salava.core.time :refer [date-from-unix-time]]))

(defn issued-on [issued]
  (when (> issued 0)
    [:div.issued-on
     [:label (t :badge/Issuedon ":")]
     [:span (date-from-unix-time (* 1000 issued))]]))

(defn issuer-label-and-link [name url email]
  [:div.issuer
   [:label (t :badge/Issuedby ": ")]
   [:a {:target "_blank" :href url} name]
   (if email
     [:span " / " [:a {:href (str "mailto:" email)} email]])])
