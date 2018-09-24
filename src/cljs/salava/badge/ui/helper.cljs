(ns salava.badge.ui.helper
  (:require
    [salava.core.i18n :refer [t]]
    [salava.core.ui.helper :refer [path-for]]
    [salava.core.helper :refer [dump]]
    [clojure.string :refer [blank?]]
    [salava.user.ui.helper :as uh]
    [salava.core.time :refer [unix-time date-from-unix-time]]
    [reagent.core :refer [atom cursor create-class]]
    [reagent-modals.modals :as m]
    ))


(defn badge-expired? [expires-on]
  (and expires-on (< expires-on (unix-time))))

(defn meta-badge [meta_badge meta_badge_req]
  (if meta_badge
    [:div.metabadge [:label (t :badge/Milestonebadge)]])
  (if meta_badge_req
    [:div.metabadge [:label (t :badge/Requiredbadge)]]))

  (defn issued-on [issued]
    (when (> issued 0)
      [:div.issued-on
       [:label (t :badge/Issuedon) ":"]
       [:span (date-from-unix-time (* 1000 issued))]]))

(defn issuer-image [image]
  (if-not (blank? image)
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
     [:img {:src (str "/" image)}]])

  (defn creator-label-and-link [name url email]
    [:div {:class "issuer-data clearfix"}
     ;[:label.pull-left (t :badge/Createdby) ":"]
     [:div {:class "issuer-links pull-left"}
      [:a {:target "_blank" :href url} " " name]
      (if (not-empty email)
        [:span [:br] [:a {:href (str "mailto:" email)} email]])]])

  (defn creator-description [creator_description]
    [:div [:div {:class "w3-container issuer-description"} creator_description]])

  (defn endorser-image [image]
      [:div {:class "endorser-image fa-10x pull-left"}
       #_[:img {:src (if (nil? image) uh/default-profile-picture (str "/" image))}]])

  (defn endorsement-displayer [endorsement]
        [:div {:class "media endorsement-content-item"}
         [:div.endcontent
            [:div [:i {:class "fa fa-thumbs-o-up" :style {:font-size "20px"}}]]
            [:div {:class "endorsement-body-container"}
             [:div.namedate
              [:div.name [:h4 {:class "media-heading endorser-body"}
              #_[:a {:href "#"
                    :on-click #(do (.preventDefault %) #_(set-new-view (endorser-content endorsement)))
                   } (or (:issuer_name endorsement)(:endorser_name endorsement))]
                [:span.name (:endorser_name endorsement)]
              ]]
              [:div.date [:span (date-from-unix-time (* 1000 (:endorsement_issuedon endorsement)))]]
              ]
              [:div.commentbox
                [:span {:style {:font-style "italic"} } (:endorsement_comment endorsement)]]]]])

  (defn endorser-info-displayer [name url description email image endorsements]
    [:div.panel {:id "profile"}
     [:div.panel-body
      [:div.row
        [:div.col-xs-12
           [:div.row.endorser-header
            [:div [:h1.uppercase-header name]]]

            [:div.row.endorser-info
             [:div {:class "col-md-9 col-sm-9 col-xs-12"}
              (if (not-empty description)
                [:div {:class "row about"}
                 [:div.col-xs-12 [:b (t :badge/About) ":"]]
                 [:div.col-xs-12 description]])
              (if (not-empty email)
                [:div {:class "row"}
                 [:div.col-xs-12 [:b (t :badge/Contact)":"]]
                 [:div.col-xs-12
                  [:span [:a {:href (str "mailto:" email)} email]]]])
              (if (not-empty url)
                [:div {:class "row"}
                 [:div.col-xs-12 [:b (t :badge/Website) ":"]]
                 [:div.col-xs-12
                  [:a {:target "_blank" :href url} name]]])]
              (when (not-empty image)
               [:div {:class "col-md-3 col-sm-3 col-xs-12"}
                [:div.profile-picture-wrapper
                [:img.profile-picture {:src (str "/" image)}]]])]]]

      (when (not-empty endorsements)
        [:div.row
         [:div.col-xs-12
          [:div [:h2.uppercase-header (t :badge/endorsements)]]
          [:div.row
            (into [:div]
                  (for [endorsement endorsements]
                    (endorsement-displayer endorsement)))]]])]])


(defn issuer-label-image-link [name url description email image]
    (if (or name url email image)
      [:div {:class "issuer-data clearfix"}
       [:label {:class "pull-label-left"}  (t :badge/Issuedby) ":"]
       [:div {:class "issuer-links pull-label-left inline"}
         (issuer-image image)
         [:a {:target "_blank" :rel "noopener noreferrer" :href url} name]
         (if (not-empty email)
           [:span [:br] [:a {:href (str "mailto:" email)} email]])]]))

(defn creator-label-image-link [name url description email image]
  (if (or name url email image)
    [:div {:class "issuer-data clearfix"}
     [:label.pull-left (t :badge/Createdby) ":"]
     (issuer-image image)
     [:div {:class "issuer-links pull-left"}
      [:a {:target "_blank" :rel "noopener noreferrer" :href url} name]
      (if (not-empty email)
        [:span [:br] [:a {:href (str "mailto:" email)} email]])]]))


(defn issued-by-obf [obf-url verified-by-obf? issued-by-obf?]
  (let [class-name (if verified-by-obf? "verifiedissuedbyobf-image-url" "issuedbyobf-image-url")]
     [:div.col-md-3
      [:a {:class class-name :href obf-url :target "_blank" :style {:display "block"}}]]))

