(ns salava.badge.ui.embed-pic
  (:require [reagent.core :refer [atom cursor]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.badge.ui.helper :as bh]
            [salava.core.ui.helper :refer [path-for]]))

(defn content [state]
  (let [{:keys [id badge_content_id name owner? visibility show_evidence image_file rating issuer_image issued_on expires_on revoked issuer_content_name issuer_content_url issuer_contact issuer_description first_name last_name description criteria_url user-logged-in? congratulated? congratulations view_count evidence_url issued_by_obf verified_by_obf obf_url recipient_count assertion creator_name creator_image creator_url creator_email creator_description  qr_code owner]} @state
        expired? (bh/badge-expired? expires_on)]

    [:div {:class "badge-image-embed-pic"}
     [:div.row
      [:div.col-xs-12
       [:img {:src (str "/" image_file)}]]]]))

(defn init-data [state id]
  (ajax/GET
   (path-for (str "/obpv1/badge/info/" id))
    {:handler (fn [data]
                (reset! state (assoc data :id id
                                     :show-link-or-embed-code nil)))}))


(defn handler [site-navi params]
  (let [id (:badge-id params) 
        state (atom {}) ]
    (init-data state id)
    (fn []
      (layout/embed-page (content state)))))
