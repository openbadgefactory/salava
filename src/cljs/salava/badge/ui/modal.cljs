(ns salava.badge.ui.modal
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]
            [salava.badge.ui.helper :as bh]
            [salava.badge.ui.assertion :as a]
            [salava.badge.ui.settings :as se]
            [salava.core.ui.rate-it :as r]
            [salava.core.ui.share :as s]
            [salava.core.helper :refer [dump]]
            [salava.user.ui.helper :as uh]
            [salava.gallery.ui.badges :as b]
            [salava.core.ui.helper :refer [path-for private?]]
            [salava.core.time :refer [date-from-unix-time unix-time]]
            [salava.admin.ui.admintool :refer [admintool]]
            [salava.social.ui.follow :refer [follow-badge]]
            [salava.core.ui.error :as err]
            [salava.core.ui.modal :as mo]
            [salava.core.ui.content-language :refer [init-content-language content-language-selector content-setter]]
            ;[salava.badge.ui.endorsement :refer [endorsement-modal-link]]
            [salava.badge.ui.endorsement :as endr]
            [salava.badge.ui.issuer :as issuer]
            [salava.social.ui.badge-message-modal :refer [badge-message-link]]
            [salava.admin.ui.reporttool :refer [reporttool1]]
            [salava.badge.ui.verify :as verify])
  )



(defn congratulate [state]
  (ajax/POST
    (path-for (str "/obpv1/badge/congratulate/" (:id @state)))
    {:handler (fn [] (swap! state assoc :congratulated? true))}))

(defn badge-endorsement-modal-link [badge-id endorsement-count]
  (when (pos? endorsement-count)
    [:div.row
     [:div.col.xs-12
      [:hr.endorsementhr]
      [:a.endorsementlink {:class "endorsement-link"
                           :href "#"
                           :on-click #(do (.preventDefault %)
                                        (mo/open-modal [:badge :endorsement] badge-id))}
       (if (== endorsement-count 1)
         (str  endorsement-count " " (t :badge/endorsement))
         (str  endorsement-count " " (t :badge/endorsements)))]]]))

(defn issuer-modal-link [issuer-id name]
  [:div {:class "issuer-data clearfix"}
   [:label {:class "pull-label-left"}  (t :badge/Issuedby) ":"]
   [:div {:class "issuer-links pull-label-left inline"}
    [:a {:href "#"
         :on-click #(do (.preventDefault %)
                      (mo/open-modal [:badge :issuer] issuer-id))} name]]])

;;;TODO use modal
(defn creator-modal-link [creator-id name]
  (when (and creator-id name)
    [:div {:class "issuer-data clearfix"}
     [:label.pull-left (t :badge/Createdby) ":"]
     [:div {:class "issuer-links pull-label-left inline"}
      name
      #_[:a {:href "#"
             :on-click #(do (.preventDefault %)
                          (mo/open-modal [:badge :creator] creator-id))} name]]]))

(defn verify-badge-link [badge-id]
  [:div {:id "verify-link"}
   [:a.link {:href "#"
             :on-click #(do (.preventDefault %)
                          ;;                           (swap! state assoc :verifying true)
                          (mo/open-modal [:badge :verify] badge-id)
                          #_(m/modal! [verify-badge-modal badge-id] {:size :lg}))}
    (str (t :badge/Verifybadge) "...")]])



(defn content [state]

  (let [{:keys [id badge_id  owner? visibility show_evidence rating issuer_image issued_on expires_on revoked issuer_content_id issuer_content_name issuer_content_url issuer_contact issuer_description first_name last_name description criteria_url criteria_content user-logged-in? congratulated? congratulations view_count evidence_url issued_by_obf verified_by_obf obf_url recipient_count assertion creator_content_id creator_name creator_image creator_url creator_email creator_description  qr_code owner message_count issuer-endorsements content endorsement_count endorsements]} @state
        expired? (bh/badge-expired? expires_on)
        show-recipient-name-atom (cursor state [:show_recipient_name])
        selected-language (cursor state [:content-language])
        {:keys [name description tags alignment criteria_content image_file image_file issuer_content_id issuer_content_name issuer_content_url issuer_contact issuer_image issuer_description criteria_url  creator_name creator_url creator_email creator_image creator_description message_count endorsement_count]} (content-setter @selected-language content)]

    [:div
     [:div.col-xs-12
      [:div.pull-right
       [follow-badge badge_id]]]
     [:div {:id "badge-info"}
      [:div.panel
       [:div.panel-body
        (if (or verified_by_obf issued_by_obf)
          (bh/issued-by-obf obf_url verified_by_obf issued_by_obf))
        [:div.row
         [:div {:class "col-md-3 badge-image"}
          [:div.row
           [:div.col-xs-12
            [:img {:src (str "/" image_file)}]]]
          [:div.row
           [:div.col-xs-12 {:id "badge-congratulated"}
            (if (and user-logged-in? (not owner?))
              (if congratulated?
                [:div.congratulated
                 [:i {:class "fa fa-heart"}]
                 (str " " (t :badge/Congratulated))]
                [:button {:class    "btn btn-primary"
                          :on-click #(congratulate state)}
                 [:i {:class "fa fa-heart"}]
                 (str " " (t :badge/Congratulate) "!")])
              )]]
          #_(if (session/get :user)
              [badge-message-link message_count  badge_id])
          ]
         [:div {:class "col-md-9 badge-info"}
          [:div.row
           [:div {:class "col-md-12"}
            (content-language-selector selected-language (:content @state))

            [:h1.uppercase-header name]
            #_(bh/issuer-label-image-link issuer_content_name issuer_content_url issuer_description issuer_contact issuer_image issuer-endorsements)
            #_(bh/creator-label-image-link creator_name creator_url creator_description creator_email creator_image)
            (issuer-modal-link issuer_content_id issuer_content_name)
            (creator-modal-link creator_content_id creator_name)

            (if (and issued_on (> issued_on 0))
              [:div [:label (t :badge/Issuedon) ": "]  (date-from-unix-time (* 1000 issued_on))])
            (if (and expires_on (not expired?))
              [:div [:label (t :badge/Expireson) ": "]  (date-from-unix-time (* 1000 expires_on))])
            (if assertion
              [:div {:id "assertion-link"}
               [:label (t :badge/Metadata)": "]
               [:a {:href     "#"
                    :on-click #(mo/set-new-view [:badge :metadata] (dissoc assertion :evidence))}
                (t :badge/Openassertion) "..."]])
            (verify-badge-link id)
            (if (pos? @show-recipient-name-atom)
              (if (and user-logged-in? (not owner?))
                [:div [:label (t :badge/Recipient) ": " ] [:a {:href (path-for (str "/user/profile/" owner))} first_name " " last_name]]
                [:div [:label (t :badge/Recipient) ": "]  first_name " " last_name])
              )
            [:div.description description]]]

          (when-not (empty? alignment)
            [:div.row
             [:div.col-md-12
              [:h2.uppercase-header (t :badge/Alignments)]
              (doall
                (map (fn [{:keys [name url description]}]
                       [:p {:key url}
                        [:a {:target "_blank" :rel "noopener noreferrer" :href url} name] [:br] description])
                     alignment))]])

          [:div {:class "row criteria-html"}
           [:div.col-md-12
            [:h2.uppercase-header (t :badge/Criteria)]
            [:a {:href criteria_url :target "_blank"} (t :badge/Opencriteriapage) "..."]
            [:div {:dangerouslySetInnerHTML {:__html criteria_content}}]]]

          (if (and (pos? show_evidence) evidence_url)
            [:div.row
             [:div.col-md-12
              [:h2.uppercase-header (t :badge/Evidence)]
              [:div [:a {:target "_blank" :href evidence_url} (t :badge/Openevidencepage) "..."]]]])

          ]]
        (if owner? "" [reporttool1 id name "badge"])]]]]
    ))


(defn init-data [state id]
  (ajax/GET
    (path-for (str "/obpv1/badge/info/" id))
    {:handler (fn [data]
                (reset! state (assoc data :id id
                                :show-link-or-embed-code nil
                                :initializing false
                                :content-language (init-content-language (:content data))
                                :permission "success")))}
    (fn [] (swap! state assoc :permission "error"))))





(defn handler [params]

  (let [id (:badge-id params)
        state (atom {:initializing true
                     :permission "initial"})
        user (session/get :user)]
    (init-data state id)

    (fn []
      (cond
        (= "initial" (:permission @state)) [:div]
        (and user (= "error" (:permission @state))) (err/error-content)
        (= "error" (:permission @state)) (err/error-content)
        (and (= "success" (:permission @state)) (:owner? @state) user) (content state)
        (and (= "success" (:permission @state)) user) (content state)
        :else (content state) ))
    ))


(def ^:export modalroutes
  {:badge {:info handler
           :metadata a/assertion-content
           :endorsement endr/badge-endorsement-content
           :issuer issuer/content
           :creator issuer/creator-content
           :verify verify/verify-badge}})
