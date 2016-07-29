(ns salava.badge.ui.helper
  (:require [salava.core.i18n :refer [t]]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.time :refer [unix-time date-from-unix-time]]))

(defn badge-expired? [expires-on]
  (and expires-on (< expires-on (unix-time))))

(defn issued-on [issued]
  (when (> issued 0)
    [:div.issued-on
     [:label (t :badge/Issuedon) ":"]
     [:span (date-from-unix-time (* 1000 issued))]]))

(defn issuer-image [image]
    (if image
    [:div {:class "issuer-image pull-left"}
     [:img {:src (str "/" image)}]]))

(defn issuer-label-and-link [name url email]
  [:div {:class "issuer-data clearfix"}
   ;[:label.pull-left (t :badge/Issuedby) ":"]
   [:div {:class "issuer-links pull-left"}
    [:a {:target "_blank" :href url} " " name]
    (if (not-empty email)
      [:span [:br] [:a {:href (str "mailto:" email)} email]])]])

(defn issuer-description [issuer_description]
  (if issuer-description
  [:div [:div {:class "w3-container issuer-description"} issuer_description]]))

(defn creator-image [image]
  [:div {:class "issuer-image pull-left"}
   [:img {:src (str "/" image)}]]
   )

(defn creator-label-and-link [name url email]
  [:div {:class "issuer-data clearfix"}
   ;[:label.pull-left (t :badge/Createdby) ":"]
   [:div {:class "issuer-links pull-left"}
    [:a {:target "_blank" :href url} " " name]
    (if (not-empty email)
      [:span [:br] [:a {:href (str "mailto:" email)} email]])]])

(defn creator-description [creator_description]
  [:div [:div {:class "w3-container issuer-description"} creator_description]])

(defn issued-by-obf [obf-url verified-by-obf? issued-by-obf?]
  (let [class-name (if verified-by-obf? "verifiedissuedbyobf-image-url" "issuedbyobf-image-url")]
  [:div.row
   [:div.col-xs-12
     [:div {:class class-name} [:a {:href obf-url :target "_blank"}]]]]))

