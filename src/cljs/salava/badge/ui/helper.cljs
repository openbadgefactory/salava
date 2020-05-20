(ns salava.badge.ui.helper
  (:require
   [salava.core.i18n :refer [t]]
   [salava.core.ui.helper :refer [path-for]]
   [salava.core.helper :refer [dump]]
   [clojure.string :refer [blank?]]
   [salava.user.ui.helper :as uh]
   [salava.core.time :refer [unix-time date-from-unix-time]]
   [reagent.core :refer [atom cursor create-class]]
   [reagent-modals.modals :as m]))

(defn badge-expired? [expires-on]
  (and expires-on (< expires-on (unix-time))))

(defn issued-on [issued]
  (when (> issued 0)
    [:div.issued-on
     [:span._label (t :badge/Issuedon) ":"]
     [:span (date-from-unix-time (* 1000 issued))]]))

(defn issuer-image [image]
  (if-not (blank? image)
    [:div {:class "issuer-image pull-left"}
     [:img {:src (str "/" image) :alt ""}]]))

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

(defn creator-image [image name]
  [:div {:class "issuer-image pull-left"}
   [:img {:src (str "/" image) :alt name}]])

(defn creator-label-and-link [name url email]
  [:div {:class "issuer-data clearfix"}
   ;[:label.pull-left (t :badge/Createdby) ":"]
   [:div {:class "issuer-links pull-left"}
    [:a {:target "_blank" :href url} " " name]
    (if (not-empty email)
      [:span [:br] [:a {:href (str "mailto:" email)} email]])]])

(defn creator-description [creator_description]
  [:div [:div {:class "w3-container issuer-description"} creator_description]])

(defn issuer-label-image-link [name url description email image]
  (if (or name url email image)
    [:div {:class "issuer-data clearfix"}
     [:span._label.pull-left {:class "pull-label-left"}  (t :badge/Issuedby) ":"]
     [:div {:class "issuer-links pull-label-left inline"}
      (issuer-image image)
      [:a {:target "_blank" :rel "noopener noreferrer" :href url} name]
      (if (not-empty email)
        [:span [:br] [:a {:href (str "mailto:" email)} email]])]]))

(defn creator-label-image-link [name url description email image]
  (if (or name url email image)
    [:div {:class "issuer-data clearfix"}
     [:span._label.pull-left (t :badge/Createdby) ":"]
     (issuer-image image)
     [:div {:class "issuer-links pull-left"}
      [:a {:target "_blank" :rel "noopener noreferrer" :href url} name]
      (if (not-empty email)
        [:span [:br] [:a {:href (str "mailto:" email)} email]])]]))

(defn issued-by-obf [obf-url verified-by-obf? issued-by-obf?]
  (let [class-name (if verified-by-obf? "verifiedissuedbyobf-image-url" "issuedbyobf-image-url")]
    [:div.col-md-3
     [:a {:class class-name :href obf-url :target "_blank" :style {:display "block"} :aria-label (t :badge/Issuedandverifiedbyobf)}]]))
