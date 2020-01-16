(ns salava.badge.ui.receive
  (:require [clojure.string :refer [replace upper-case blank? starts-with? split]]
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
            [salava.badge.ui.pending :as pb :refer [visibility-modal update-status badge-alert]]
            [salava.badge.ui.settings :as se]
            [salava.core.ui.rate-it :as r]
            [salava.core.ui.share :as s]
            [salava.core.helper :refer [dump]]
            [salava.user.ui.helper :as uh]
            [salava.core.ui.modal :as mo]
            [salava.core.ui.helper :refer [path-for private? navigate-to js-navigate-to plugin-fun hyperlink url?]]
            [salava.core.time :refer [date-from-unix-time unix-time]]
            [salava.admin.ui.admintool :refer [admintool]]
            [salava.social.ui.follow :refer [follow-badge]]
            [salava.core.ui.error :as err]
            [salava.core.ui.content-language :refer [init-content-language content-language-selector content-setter]]
            [salava.social.ui.badge-message-modal :refer [badge-message-link]]))

(defn banner [obf-url]
  (if-let [[_ banner-file] (->> js/window .-location .-search (re-find #"banner=(\w+\.[a-z]{3})"))]
    [:div {:style {:width "640px" :margin "auto"}}
     [:img {:src (str obf-url "/c/download/" banner-file)}]]))

(defn reject-badge [state]
  (let [user-badge-id (:id @state)
        link (if (:user_in_session? @state) "/social" "/user/login")]
    (ajax/DELETE
     (path-for (str "/obpv1/factory/receive/" user-badge-id))
     {:handler (fn [data]
                 (when (:success data)
                   (js-navigate-to link)))})))

(defn reject-badge-modal [state]
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
              :on-click #(reject-badge state)}
     (t :core/Delete)]]])

(defn cert-block [user-badge-id state]
  (when-let [cert-uri (some-> @state :cert :uri)]
    [:div
     (doall
      (map (fn [[badge lang]]
             (let [uri (str cert-uri "&lang=" lang)]
               [:p {:key uri}
                [:i.fa.fa-file-pdf-o.fa-2x.fa-fw] " "
                [:a {:href uri} (if-not (blank? lang) (str badge " (" lang ")") badge)]]))
           (:badge @state)))]))

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

(defn badge-info-content [state]
  (let [{:keys [id badge_id email owner? issued_on expires_on assertion_url
                user-logged-in?
                evidence_url issued_by_obf verified_by_obf obf_url
                recipient_count assertion  qr_code owner message_count content issuer-endorsements user_exists? user_in_session? evidences]} @state
        expired?                                                                 (bh/badge-expired? expires_on)
        show-recipient-name-atom                                                 (cursor state [:show_recipient_name])
        selected-language                                                        (cursor state [:content-language])
        {:keys [name description tags alignment criteria_content image_file
                issuer_content_id issuer_content_name issuer_content_url issuer_contact
                issuer_image issuer_description criteria_url
                creator_content_id creator_name creator_url creator_email
                creator_image creator_description message_count endorsement_count]} (content-setter @selected-language content)]
    [:div {:id (when user_exists? "badge-info")}

     [:div.panel
      [:div.panel-body
       (if (or verified_by_obf issued_by_obf)
         [:div.row.flip (bh/issued-by-obf obf_url verified_by_obf issued_by_obf)])
       [:div {:class "row row_reverse" :id (when-not user_exists? "badge-info")}
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

           (into [:div]
                 (for [f (plugin-fun (session/get :plugins) "block" "meta_link")]
                   [f {:assertion_url assertion_url}]))

           (if assertion
             [:div {:id "assertion-link"}
              [:label (t :badge/Metadata) ": "]
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

         (when (seq evidences)
           [:div.row {:id "badge-settings"}
            [:div.col-md-12
             [:h2.uppercase-header (t :badge/Evidences) #_(if (= (count  evidences) 1)  (t :badge/Evidence) (str (t :badge/Evidence) " (" (count evidences) ")"))]
             (reduce (fn [r evidence]
                       (let [{:keys [narrative description name id url mtime ctime properties]} evidence
                             added-by-user? (and (not (blank? description)) (starts-with? description "Added by badge recipient")) ;;use regex
                             desc (cond
                                    (not (blank? narrative)) narrative
                                    (not added-by-user?) description ;;todo use regex to match description
                                    :else nil)]
                         (conj r (when url? url
                                       [:div.modal-evidence
                                        (when-not added-by-user? [:span.label.label-success (t :badge/Verifiedevidence)])
                                        [:div.evidence-icon [:i.fa.fa-link]]
                                        [:div.content
                                         (when-not (blank? name) [:div.content-body.name name])
                                         (when-not (blank? desc) [:div.content-body.description {:dangerouslySetInnerHTML {:__html desc}}])
                                         [:div.content-body.url
                                          (hyperlink url)]]]))))
                     [:div] evidences)]])]]]]]))

(defn user-options [state]
  (let [{:keys [assertion_url id obf_url user_exists? user_in_session? content]} @state
        badge (select-keys @state [:id :visibility])
        reload-fn #(js-navigate-to "/badge")
        selected-language   (cursor state [:content-language])
        {:keys [image_file name]} (content-setter @selected-language content)
        b_ (map (fn [v] [(:name v) (:language_code v)]) (:content @state))
        cert-state (cursor state [:cert-state])]
    (reset! cert-state {:cert [] :badge b_})
    (se/init-cert id cert-state)
    (fn []
      [:div.text-center
       (if user_exists?
         (if user_in_session?
           [:div.row.button-row
            (if (:badge-alert @state)
              [:div.ajax-message
               [:i.fa.fa-cog.fa-spin.fa-2x]]
              [:div.col-md-12
               [:button {:class "btn btn-primary"
                         :on-click #(do
                                      (m/modal! [visibility-modal badge state reload-fn] {:size :md}) ;:hidden (fn [] 0(reload-fn state))})
                                      (.preventDefault %))

                         :data-dismiss "modal"}
                (t :badge/Acceptbadge)]
               [:button {:class "btn btn-warning"
                         :on-click #(do
                                      (update-status (:id badge) "declined" state reload-fn)
                                      (.preventDefault %)
                                      (swap! state assoc :badge-alert "declined" :badge-name (:name badge)))}
                (t :badge/Declinebadge)]])]
           [:div [:p [:a#login-button.btn.btn-primary {:href (path-for "/user/login")} (t :user/Login)]]])
         [:div
          [:p [:a#login-button.btn.btn-primary {:href (path-for "/user/login")} (t :user/Login)]]
          [:p [:a {:href (path-for "/user/register")} [:i.fa.fa-user-plus] " " (t :user/Createnewaccount)]]])
       [:hr.border]
       (if (some-> @cert-state :cert :uri)
         [:div
          [:div.col-md-12
           [:p.pull-left [:i.fa.fa-download] " " (t :badge/DownloadThisBadge)]
           [:p.pull-right [:a {:href "#" :on-click (fn [] (m/modal! [reject-badge-modal state] {:size :lg}))} [:i.fa.fa-ban] " " (t :badge/IDontWantThisBadge)]]]
          [:div.col-md-12
           [:div {:style {:text-align "start"}}
            [:p [:a {:href (str obf_url "/c/receive/download?url=" assertion_url)} [:img {:style {:vertical-align "bottom" :width "35px" :height "auto"} :src (str "/" image_file) :alt ""}] name]]
            [cert-block id cert-state]]]]
         [:div.col-md-12
          [:p.pull-left [:a {:href (str obf_url "/c/receive/download?url=" assertion_url)} [:i.fa.fa-download] " " (t :badge/DownloadThisBadge)]]
          [:p.pull-right [:a {:href "#" :on-click (fn [] (m/modal! [reject-badge-modal state] {:size :lg}))} [:i.fa.fa-ban] " " (t :badge/IDontWantThisBadge)]]])])))

(defn language-switcher []
  (let [current-lang (session/get-in [:user :language] "en")
        languages (session/get :languages)]
    [:div.pull-right
     (doall
      (map (fn [lang]
             ^{:key lang} [:a {:style (if (= current-lang lang) {:font-weight "bold" :text-decoration "underline"} {})
                               :href "#"
                               :on-click #(session/assoc-in! [:user :language] lang)}
                           (upper-case lang) " "])
           languages))]))

(defn existing-user-content [state]
  (let [{:keys [user_exists? user_in_session? assertion_url obf_url email]} @state
        current-lang (session/get-in [:user :language])]

    [:div.existing {:style {:width "640px" :margin "15px auto"}}
     ;[badge-alert state]
     [:div.panel
      [:div.panel-heading
       [:div.row
        [:div.col-md-12
         [language-switcher]
         [:h1.uppercase-header (t :badge/YouHaveGotaBadge)]]]]
      [:div.panel-body
       [:p (t :badge/BadgeIssuedTo) ": " [:strong email]]
       [:hr.border]
       [badge-info-content state]
       [user-options state]]]]))

(defn content [state]
  (let [{:keys [id badge_id email owner? issued_on expires_on assertion_url
                obf_url user_exists? user_in_session? evidences]} @state]

    (session/assoc-in! [:user :pending :email] email)
    [:div#badge-receive
     [m/modal-window]
     (banner obf_url)
     (if user_exists?
       [existing-user-content state]
       [:div {:style {:width "640px" :margin "15px auto"}}
        [:div.panel
         [:div.panel-heading
          [:div.row
           [:div.col-md-12
            [language-switcher]
            [:h1.uppercase-header (t :badge/YouHaveGotaBadge)]]]]
         [:div.panel-body
          [:p (t :badge/BadgeIssuedTo) ": " [:strong email]] [:p (t :badge/AnOpenBadgeIs) " " (t :badge/YouCanAddYourBadges) " " (t :badge/TheEasiestWayToManage)]

          [:p (t :badge/IfYouSignUpUsingEmail)]

          [user-options state]]]
        [badge-info-content state]])]))

(defn handler [site-navi params]
  (let [id (:badge-id params)
        state (atom {:initializing true :result "initial" :cert-state nil})
        user (session/get :user)
        site-navi (assoc site-navi :navi-items [] :no-login true)
        languages (session/get :languages)
        lang (session/get-in [:user :language] (-> (or js/window.navigator.userLanguage js/window.navigator.language) (split #"-") first))]

    (when (some #(= % lang) languages) (session/assoc-in! [:user :language] lang))
    (init-data state id)
    (fn []
      (cond
        (= "initial" (:result @state)) [:div]
        (= "error" (:result @state)) (layout/landing-page site-navi (err/error-not-found))
        :else (layout/landing-page site-navi (content state))))))
