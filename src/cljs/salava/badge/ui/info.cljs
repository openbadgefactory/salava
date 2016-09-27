(ns salava.badge.ui.info
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]
            [salava.badge.ui.helper :as bh]
            [salava.badge.ui.assertion :as a]
            [salava.core.ui.rate-it :as r]
            [salava.core.ui.share :as s]
            [salava.user.ui.helper :as uh]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.time :refer [date-from-unix-time unix-time]]
            [salava.admin.ui.admintool :refer [admintool]]
            [salava.social.ui.badge-message-modal :refer [badge-message-link]]
            [salava.admin.ui.reporttool :refer [reporttool]]))

(defn toggle-visibility [state]
  (let [id (:id @state)
        new-value (if (= (:visibility @state) "private") "public" "private")]
    (ajax/POST
      (path-for (str "/obpv1/badge/set_visibility/" id))
      {:params {:visibility new-value}
       :handler (fn [] (swap! state assoc :visibility new-value))})))

(defn toggle-recipient-name [id show-recipient-name-atom]
  (let [new-value (not @show-recipient-name-atom)]
    (ajax/POST
      (path-for (str "/obpv1/badge/toggle_recipient_name/" id))
      {:params {:show_recipient_name new-value}
       :handler (fn [] (reset! show-recipient-name-atom new-value))})))

(defn toggle-evidence [state]
  (let [id (:id @state)
        new-value (not (:show_evidence @state))]
    (ajax/POST
      (path-for (str "/obpv1/badge/toggle_evidence/" id))
      {:params {:show_evidence new-value}
       :handler (fn [] (swap! state assoc :show_evidence new-value))})))

(defn congratulate [state]
  (ajax/POST
    (path-for (str "/obpv1/badge/congratulate/" (:id @state)))
    {:handler (fn [] (swap! state assoc :congratulated? true))}))

(defn content [state]
  (let [{:keys [id badge_content_id name owner? visibility show_evidence image_file rating issuer_image issued_on expires_on revoked issuer_content_name issuer_content_url issuer_contact issuer_description first_name last_name description criteria_url html_content user-logged-in? congratulated? congratulations view_count evidence_url issued_by_obf verified_by_obf obf_url recipient_count assertion creator_name creator_image creator_url creator_email creator_description  qr_code owner message_count]} @state
        expired? (bh/badge-expired? expires_on)
        show-recipient-name-atom (cursor state [:show_recipient_name])
        reporttool-atom (cursor state [:reporttool])]
    (if (:initializing @state)
      [:div.ajax-message
       [:i {:class "fa fa-cog fa-spin fa-2x "}]
       [:span (str (t :core/Loading) "...")]]
      [:div {:id "badge-info"}
       [m/modal-window]
       [:div.panel
        [:div.panel-body
         (if (and owner? (not expired?) (not revoked))
           [:div.row {:id "badge-share-inputs"}
            [:div.col-sm-3
             [:div.checkbox
              [:label
               [:input {:type      "checkbox"
                        :on-change #(toggle-visibility state)
                        :checked   (= visibility "public")}]
               (t :core/Publishandshare)]]]
            [:div.col-sm-3
             [:div.checkbox
              [:label
               [:input {:type      "checkbox"
                        :on-change #(toggle-recipient-name id show-recipient-name-atom)
                        :checked   @show-recipient-name-atom}]
               (t :badge/Showyourname)]]]
            (if evidence_url
              [:div.col-sm-3
               [:div.checkbox
                [:label
                 [:input {:type      "checkbox"
                          :on-change #(toggle-evidence state)
                          :checked   show_evidence}]
                 (t :badge/Showevidence)]]])
            [:div {:class "col-sm-3 text-right"}
             [:button {:class    "btn btn-primary"
                       :on-click #(.print js/window)}
              (t :core/Print)]]
            [:div.col-sm-12
             [s/share-buttons (str (session/get :site-url) (path-for (str "/badge/info/" id))) name (= "public" visibility) true (cursor state [:show-link-or-embed])]]]
           (admintool id "badge"))
         (if (or verified_by_obf issued_by_obf)
           (bh/issued-by-obf obf_url verified_by_obf issued_by_obf))
         [:div.row
          [:div {:class "col-md-3 badge-image"}
           [:div.row
            [:div.col-xs-12
             [:img {:src (str "/" image_file)}]]]
           (if (and qr_code (= visibility "public"))
             [:img#print-qr-code {:src (str "data:image/png;base64," qr_code)}])
           (if owner?
             [:div.row {:id "badge-rating"}
              [:div.col-xs-12
               [:div.rating
                [r/rate-it rating]]
               [:div.view-count
                (cond
                  (= view_count 1) (t :badge/Viewedonce)
                  (> view_count 1) (str (t :badge/Viewed) " " view_count " " (t :badge/times))
                  :else (t :badge/Badgeisnotviewedyet))]]])
           (if (> recipient_count 1)
             [:div.row {:id "badge-views"}
              [:div.col-xs-12
               [:a {:href (path-for (str "/gallery/badgeview/" badge_content_id))} (t :badge/Otherrecipients)]]])
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
                  (str " " (t :badge/Congratulations) "!")])
               )]]
           [badge-message-link message_count  badge_content_id]]
          [:div {:class "col-md-9 badge-info"}
           [:div.row
            [:div {:class "col-md-12"}
             (if revoked
               [:div.revoked (t :badge/Revoked)])
             (if expired?
               [:div.expired (t :badge/Expiredon) ": " (date-from-unix-time (* 1000 expires_on))])
             [:h1.uppercase-header name]
             (bh/issuer-label-image-link issuer_content_name issuer_content_url issuer_contact issuer_image)
             (bh/creator-label-image-link creator_name creator_url creator_email creator_image)
             
             (if (and issued_on (> issued_on 0))
               [:div [:label (t :badge/Issuedon)] ": " (date-from-unix-time (* 1000 issued_on))])
             (if (and expires_on (not expired?))
               [:div [:label (t :badge/Expireson)] ": " (date-from-unix-time (* 1000 expires_on))])
            (if assertion
              [:div {:id "assertion-link"}
               [:label (t :badge/Metadata)] ": "
               [:a {:href     "#"
                    :on-click #(do (.preventDefault %)
                                   (m/modal! [a/assertion-modal assertion] {:size :lg}))}
                (t :badge/Openassertion) "..."]])
            (if @show-recipient-name-atom
              (if (and user-logged-in? (not owner?))
                [:div [:label (t :badge/Recipient)] ": " [:a {:href (path-for (str "/user/profile/" owner))} first_name " " last_name]]
                [:div [:label (t :badge/Recipient)] ": " first_name " " last_name])
              )
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
              [:div [:a {:target "_blank" :href evidence_url} (t :badge/Openevidencepage) "..."]]]])
          (if (and owner? (not-empty congratulations))
            [:div.row
             [:div.col-md-12 {:id "badge-congratulations"}
              [:h3.congratulated-header
               [:i {:class "fa fa-heart"}]
               " " (t :badge/Congratulatedby) ":"]
              (into [:div]
                    (for [congratulation congratulations
                          :let [{:keys [id first_name last_name profile_picture]} congratulation]]
                      (uh/profile-link-inline id first_name last_name profile_picture)))]])
          ]]
         (if owner? "" (reporttool id name "badge" reporttool-atom))]]])))

(defn init-data [state id]
  (let [reporttool-init {:description ""
                         :report-type "bug"
                         :item-id ""
                         :item-content-id ""
                         :item-url   ""
                         :item-name "" ;
                         :item-type "" ;badge/user/page/badges
                         :reporter-id ""
                         :status "false"}]
   (ajax/GET
    (path-for (str "/obpv1/badge/info/" id))
    {:handler (fn [data]
                (reset! state (assoc data :id id
                                     :show-link-or-embed-code nil
                                     :initializing false
                                     :reporttool reporttool-init)))})))


(defn handler [site-navi params]
  (let [id (:badge-id params)
        state (atom {:initializing true
                     :reporttool {}})
        user (session/get :user)]
    (init-data state id)
    (fn []
      (cond (and (:owner? @state) user) (layout/default site-navi (content state))
            user (layout/default-no-sidebar site-navi (content state))
            :else (layout/landing-page site-navi (content state))))))

