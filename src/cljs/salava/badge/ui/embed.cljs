(ns salava.badge.ui.embed
  (:require [reagent.core :refer [atom cursor]]
            [reagent-modals.modals :as m]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]
            [salava.badge.ui.helper :as bh]
            [salava.badge.ui.assertion :as a]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.time :refer [date-from-unix-time unix-time]]))

(defn content [state]
  (let [{:keys [id badge_content_id name owner? visibility show_evidence image_file rating issuer_image issued_on expires_on revoked issuer_content_name issuer_content_url issuer_contact issuer_description first_name last_name description criteria_url html_content user-logged-in? congratulated? congratulations view_count evidence_url issued_by_obf verified_by_obf obf_url recipient_count assertion creator_name creator_image creator_url creator_email creator_description  qr_code owner]} @state
        expired? (bh/badge-expired? expires_on)
        show-recipient-name-atom (cursor state [:show_recipient_name])]
    (if (:initializing @state)
      [:div.ajax-message
       [:i {:class "fa fa-cog fa-spin fa-2x "}]
       [:span (str (t :core/Loading) "...")]]
      [:div {:id "badge-info"}
       
       [:div.panel
        [m/modal-window]
        [:div.panel-body
         (if (or verified_by_obf issued_by_obf)
           (bh/issued-by-obf obf_url verified_by_obf issued_by_obf))
         [:div.row
          [:div {:class "col-sm-5 badge-image"}
           [:div.row
            [:div.col-xs-12
             [:img {:src (str "/" image_file)}]]]
           
         
           ]
          [:div {:class "col-sm-7 badge-info"}
           [:div.row
            [:div {:class "col-sm-12"}
             (if revoked
               [:div.revoked (t :badge/Revoked)])
             (if expired?
               [:div.expired (t :badge/Expiredon) ": " (date-from-unix-time (* 1000 expires_on))])
             [:h1.uppercase-header name]
             (bh/issuer-label-image-link issuer_content_name issuer_content_url issuer_contact issuer_image)
             (bh/creator-label-image-link creator_name creator_url creator_email creator_image)
             
             (if (and issued_on (> issued_on 0))
               [:div [:label (str (t :badge/Issuedon)": ")]  (date-from-unix-time (* 1000 issued_on))])
             (if (and expires_on (not expired?))
               [:div [:label (str (t :badge/Expireson) ": ")]  (date-from-unix-time (* 1000 expires_on))])
            (if assertion
              [:div {:id "assertion-link"}
               [:label (str (t :badge/Metadata) ": ")] 
               [:a {:href     "#"
                    :on-click #(do (.preventDefault %)
                                   (m/modal! [a/assertion-modal assertion] {:size :lg}))}
                (t :badge/Openassertion) "..."]])
             [:div [:label (str (t :badge/Recipient) ": ") ]  first_name " " last_name]
            
            [:div.description description]
            [:h2.uppercase-header (t :badge/Criteria)]
            [:a {:href criteria_url :target "_blank"} (t :badge/Opencriteriapage) "..."]]]
          [:div {:class "row criteria-html"}
           [:div.col-md-12
            {:dangerouslySetInnerHTML {:__html html_content}}]]
          (if (and show_evidence evidence_url)
            [:div.row
             [:div.col-md-12
              [:h2.uppercase-header (t :badge/Evidence)]
              [:div [:a {:target "_blank" :href evidence_url} (t :badge/Openevidencepage) "..."]]]])]]]]])))
(defn init-data [state id]
  (ajax/GET
    (path-for (str "/obpv1/badge/info/" id))
    {:handler (fn [data]
                (reset! state (assoc data :id id
                                     :show-link-or-embed-code nil
                                     :initializing false)))}))


(defn handler [site-navi params]
  (let [id (:badge-id params) 
        state (atom {:initializing true}) ]
    (init-data state id)
    (fn []
      (layout/embed-page (content state)))))
