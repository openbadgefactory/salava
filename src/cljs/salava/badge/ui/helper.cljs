(ns salava.badge.ui.helper
  (:require [salava.core.i18n :refer [t]]
            [salava.core.time :refer [unix-time date-from-unix-time]]))

(defn badge-expired? [expires-on]
  (and expires-on (< expires-on (unix-time))))

(defn issued-on [issued]
  (when (> issued 0)
    [:div.issued-on
     [:label (t :badge/Issuedon) ":"]
     [:span (date-from-unix-time (* 1000 issued))]]))

(defn issuer-label-and-link [name url email image]
  [:div {:class "issuer-data clearfix"}
   [:label.pull-left (t :badge/Issuedby) ":"]
   [:div {:class "issuer-links pull-left"}
    [:a {:target "_blank" :href url} " " name]
    (if (not-empty email)
      [:span [:br] [:a {:href (str "mailto:" email)} email]])]
   (if image
     [:div {:class "issuer-image pull-left"}
      [:img {:src (str "/" image)}]])])

(defn issued-by-obf [obf-url verified-by-obf? issued-by-obf?]
  [:div.row
   [:div.col-xs-12
    (if verified-by-obf?
      [:div.issued-by-obf
       [:a {:href obf-url :target "_blank"}
        [:img {:src "/img/verifiedissuedbyobf.png"}]]]
      (if issued-by-obf?
        [:div.issued-by-obf
         [:a {:href obf-url :target "_blank"}
          [:img {:src "/img/issuedbyobf.png"}]]]))]])
