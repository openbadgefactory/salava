(ns salava.badge.ui.receive
  (:require [clojure.string :refer [replace upper-case]]
            [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]
            [salava.badge.ui.helper :as bh]
            [salava.badge.ui.modal :as bm]
            [salava.badge.ui.assertion :as a]
            [salava.badge.ui.endorsement :as end]
            [salava.badge.ui.settings :as se]
            [salava.core.ui.rate-it :as r]
            [salava.core.ui.share :as s]
            [salava.core.helper :refer [dump]]
            [salava.user.ui.helper :as uh]
            [salava.core.ui.modal :as mo]
            [salava.core.ui.helper :refer [path-for private? js-navigate-to]]
            [salava.core.time :refer [date-from-unix-time unix-time]]
            [salava.admin.ui.admintool :refer [admintool]]
            [salava.social.ui.follow :refer [follow-badge]]
            [salava.core.ui.error :as err]
            [salava.core.ui.content-language :refer [init-content-language content-language-selector content-setter]]
            [salava.social.ui.badge-message-modal :refer [badge-message-link]]
            [salava.metabadge.ui.metabadge :as mb]
            ))

(defn banner [obf-url]
  (if-let [[_ banner-file] (->> js/window .-location .-search (re-find #"banner=(\w+\.[a-z]{3})"))]
    [:div {:style {:width "640px" :margin "auto"}}
     [:img {:src (str obf-url "/c/download/" banner-file)}]]))

(defn reject-badge [user-badge-id]
  (ajax/DELETE
    (path-for (str "/obpv1/factory/receive/" user-badge-id))
    {:handler (fn [data]
                (when (:success data)
                  (js-navigate-to "/user/login")))}))

(defn reject-badge-modal [user-badge-id]
  [:div
   [:div.modal-header
    [:button {:type "button"
              :class "close"
              :data-dismiss "modal"
              :aria-label "OK"}
     [:span {:aria-hidden "true"
             :dangerouslySetInnerHTML {:__html "&times;"}}]]]
   [:div.modal-body
    [:div {:class (str "alert alert-warning")}
     (t :badge/RejectConfirm)]]
   [:div.modal-footer
    [:button {:type "button"
              :class "btn btn-primary"
              :data-dismiss "modal"}
     (t :core/Cancel)]
    [:button {:type "button"
              :class "btn btn-warning"
              :on-click #(reject-badge user-badge-id)}
     (t :core/Delete)]]])



(defn init-data [state id]
  (ajax/GET
    (path-for (str "/obpv1/badge/pending/" id))
    {:handler (fn [data]
                (if (empty? (:content data))
                  (swap! state assoc :result "error")
                  (reset! state (assoc data :id id
                                       :initializing false
                                       :content-language (init-content-language (:content data))
                                       :result "success"))))}
    (fn [] (swap! state assoc :result "error"))))

(defn num-days-left [timestamp]
  (int (/ (- timestamp (/ (.now js/Date) 1000)) 86400)))


(defn content [state]
  (let [{:keys [id badge_id email owner? issued_on expires_on assertion_url
                user-logged-in?
                evidence_url issued_by_obf verified_by_obf obf_url
                recipient_count assertion  qr_code owner message_count content issuer-endorsements]} @state
        expired?                                                                 (bh/badge-expired? expires_on)
        show-recipient-name-atom                                                 (cursor state [:show_recipient_name])
        selected-language                                                        (cursor state [:content-language])
        {:keys [name description tags alignment criteria_content image_file
                issuer_content_id issuer_content_name issuer_content_url issuer_contact
                issuer_image issuer_description criteria_url
                creator_content_id creator_name creator_url creator_email
                creator_image creator_description message_count endorsement_count]} (content-setter @selected-language content)]

    (session/assoc-in! [:user :pending :email] email)

    [:div
     (banner obf_url)
     [:div {:style {:width "640px" :margin "15px auto"}}
      [:div.panel
       [:div.panel-body
        [:div.row
         [:div.col-md-12

          [:div.pull-right
           (doall (map (fn [lang] [:a {:href "#" :on-click #(session/assoc-in! [:user :language] lang)} (upper-case lang) " "]) (session/get :languages)))]

          [:h1.uppercase-header (t :badge/YouHaveGotaBadge)]

          [:p (t :badge/BadgeIssuedTo) ": " [:strong email]]

          [:p (t :badge/AnOpenBadgeIs) " " (t :badge/YouCanAddYourBadges) " " (t :badge/TheEasiestWayToManage)]

          [:p (t :badge/IfYouSignUpUsingEmail)]


          [:div.text-center
           [:p
            [:a#login-button.btn.btn-primary {:href (path-for "/user/login")} (t :user/Login)]]
           [:p
            [:a {:href (path-for "/user/register")} [:i.fa.fa-user-plus] " " (t :user/Createnewaccount)]]
           [:p
            [:a {:href (str obf_url "/c/receive/download?url=" assertion_url)} [:i.fa.fa-download] " " (t :badge/DownloadThisBadge)]]
           [:hr]
           [:p
            [:a {:href "#" :on-click (fn [] (m/modal! [reject-badge-modal id] {:size :lg}))} [:i.fa.fa-ban] " " (t :badge/IDontWantThisBadge)]]
           ]]]]]
      [:div#badge-info
       [m/modal-window]
       [:div.panel
        [:div.panel-body
         (if (or verified_by_obf issued_by_obf)
           [:div.row.flip (bh/issued-by-obf obf_url verified_by_obf issued_by_obf)])
         [:div {:class "row row_reverse"}
          [:div {:class "col-md-3 badge-image"}
           [:div.row
            [:div.col-xs-12
             [:img {:src (str "/" image_file)}]]]
           (bm/badge-endorsement-modal-link badge_id endorsement_count)]

          [:div {:class "col-md-9 badge-info"}
           [:div.row
            [:div {:class "col-md-12"}
             (content-language-selector selected-language (:content @state))
             [:h1.uppercase-header name]
             (bm/issuer-modal-link issuer_content_id issuer_content_name)
             (bm/creator-modal-link creator_content_id creator_name)

             (if (and issued_on (> issued_on 0))
               [:div [:label (t :badge/Issuedon) ": "]  (date-from-unix-time (* 1000 issued_on))])
             (if (and expires_on (not expired?))
               [:div [:label (t :badge/Expireson) ": "]  (date-from-unix-time (* 1000 expires_on))])

             [:div [mb/metabadge assertion_url #_(:assertion_url @state)]]

             (if assertion
               [:div {:id "assertion-link"}
                [:label (t :badge/Metadata)": "]
                [:a.link {:href     "#"
                          :on-click #(do (.preventDefault %)
                                         (m/modal! [a/assertion-modal (dissoc assertion :evidence)] {:size :lg}))}
                 (t :badge/Openassertion) "..."]])
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
             [:a.link {:href criteria_url :target "_blank"} (t :badge/Opencriteriapage) "..."]
             [:div {:dangerouslySetInnerHTML {:__html criteria_content}}]]]

           (if evidence_url
             [:div.row
              [:div.col-md-12
               [:h2.uppercase-header (t :badge/Evidence)]
               [:div [:a.link {:target "_blank" :href evidence_url} (t :badge/Openevidencepage) "..."]]]])]]]]]]]))



(defn handler [site-navi params]
  (let [id (:badge-id params)
        state (atom {:initializing true :result "initial"})
        user (session/get :user)
        site-navi (assoc site-navi :navi-items [] :no-login true)]
    (init-data state id)
    (fn []
      (cond
        (= "initial" (:result @state)) [:div]
        (= "error" (:result @state)) (layout/landing-page site-navi (err/error-not-found))
        :else (layout/landing-page site-navi (content state))))))

