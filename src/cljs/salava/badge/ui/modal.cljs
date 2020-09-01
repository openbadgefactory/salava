(ns salava.badge.ui.modal
  (:require
   [clojure.string :refer [blank? starts-with? split includes?]]
   [reagent.core :refer [atom cursor]]
   [reagent.session :as session]
   [salava.admin.ui.reporttool :refer [reporttool1]]
   [salava.badge.ui.helper :as bh]
   [salava.badge.ui.assertion :as a]
   [salava.badge.ui.block :as block]
   [salava.badge.ui.endorsement :as endr]
   [salava.badge.ui.issuer :as issuer]
   [salava.badge.ui.settings :as se]
   [salava.badge.ui.social :as st]
   [salava.badge.ui.verify :refer [check-badge]]
   [salava.core.helper :refer [dump]]
   [salava.core.ui.helper :refer [path-for private? plugin-fun hyperlink url?]]
   [salava.core.time :refer [date-from-unix-time]]
   [salava.core.ui.ajax-utils :as ajax]
   [salava.core.i18n :refer [t]]
   [salava.core.ui.error :as err]
   [salava.core.ui.modal :as mo]
   [salava.core.ui.content-language :refer [init-content-language content-language-selector content-setter]]
   [salava.core.ui.share :as s]
   [salava.core.ui.tag :as tag]
   [salava.social.ui.follow :refer [follow-badge]]
   [salava.social.ui.badge-message-modal :refer [badge-message-link]]
   [salava.translator.ui.helper :refer [translate]]
   [salava.badge.ui.ext-endorsement :as ext]))

(defn- init-owner-profile-visibility [user-id state]
  (ajax/GET
   (path-for (str "/obpv1/profile/user/visibility/" user-id))
   {:handler (fn [data] (reset! (cursor state [:profile_visibility]) data))}))

(defn- init-badge-connection [state badge-id]
  (ajax/GET
   (path-for (str "/obpv1/social/connected/" badge-id))
   {:handler (fn [data]
               (swap! state assoc :receive-notifications data))}))

(defn- init-new-message-count [state]
  (ajax/GET
   (path-for (str "/obpv1/social/messages_count/" (:badge_id @state)))
   {:handler (fn [data]
               (reset! (cursor state [:message_count]) data))}))

(defn- init-endorsement-count [id state]
  (ajax/GET
   (path-for (str "/obpv1/badge/user_endorsement/count/" id))
   {:handler (fn [{:keys [user_endorsement_count]}] (reset! (cursor state [:user_endorsement_count]) user_endorsement_count))}))

(defn- init-more [state]
  (init-endorsement-count (:id @state) state)
  (init-owner-profile-visibility (:user_id @state) state)
  (when (:owner? @state)
    (init-new-message-count state)
    (ajax/GET
     (path-for (str "/obpv1/badge/user_endorsement/pending_count/" (:id @state)))
     {:handler (fn [data] (reset! (cursor state [:pending_endorsements_count]) data)
                 (swap! state assoc :notification (+ data @(cursor state [:message_count :new-messages]))))})))

(defn init-data
  ([state id tab-no]
   (ajax/GET
    (path-for (str "/obpv1/badge/info/" id))
    {:handler (fn [data]
                (do (reset! state (assoc data :id id
                                         :show-link-or-embed-code nil
                                         :show_evidence_options false
                                         :initializing false
                                         :content-language (init-content-language (:content data))
                                         :tab-no tab-no
                                         :permission "success"
                                         :evidence {:url nil}
                                         :request-comment " "
                                         :selected-users []
                                         :external-users []
                                         :request-mode false))
                    (init-more state)
                    (if (or (:user-logged-in @state) (:owner? @state))  (init-badge-connection state (:badge_id data)))))}
    (fn [] (swap! state assoc :permission "error"))))

  ([state id tab-no data]
   (do (reset! state (assoc data :id id
                            :show-link-or-embed-code nil
                            :initializing false
                            :content-language (init-content-language (:content data))
                            :tab-no tab-no
                            :permission "success"
                            :request-comment " "
                            :selected-users []
                            :external-users []
                            :request-mode false))
       (init-more state)
       (if (or (:user-logged-in @state) (:owner? @state)) (init-badge-connection state (:badge_id data))))))

(defn show-settings-dialog [badge-id state init-data context]
  (ajax/GET
   (path-for (str "/obpv1/badge/settings/" badge-id) true)
   {:handler (fn [data]
               (swap! state assoc :badge-settings data (assoc data :new-tag ""))
               (if (= context "settings")
                 (swap! state assoc :tab [se/settings-tab-content data state init-data]
                        :tab-no 2
                        :evidences (:evidences data))
                 (swap! state assoc :tab [st/social-tab (assoc data :congratulations (:congratulations @state) :user_endorsement_count (:user_endorsement_count @state)) state]
                        :tab-no 3)))})

  (se/init-email-notifications badge-id state))

(defn congratulate [state]
  (ajax/POST
   (path-for (str "/obpv1/badge/congratulate/" (:id @state)))
   {:handler (fn [] (swap! state assoc :congratulated? true))}))

(defn badge-endorsement-modal-link
  ([params]
   (let [{:keys [badge-id endorsement-count lng]} params]
     (when (pos? endorsement-count)
       [:div.endorsement-link
        [:span [:i {:class "fa fa-handshake-o"}]]
        [:a {:href "#"
             :on-click #(do (.preventDefault %)
                            (mo/open-modal [:badge :endorsement] {:badge-id badge-id :lang lng}))}
         (if (== endorsement-count 1)
           (str  endorsement-count " " (translate lng :badge/endorsement))
           (str  endorsement-count " " (translate lng :badge/endorsements)))]])))
  ([badge-id endorsement-count]
   (when (pos? endorsement-count)
     [:div.endorsement-link
      [:span [:i {:class "fa fa-handshake-o"}]]
      [:a {:href "#"
           :on-click #(do (.preventDefault %)
                          (mo/open-modal [:badge :endorsement] badge-id))}
       (if (== endorsement-count 1)
         (str  endorsement-count " " (t :badge/endorsement))
         (str  endorsement-count " " (t :badge/endorsements)))]]))
  ([params endorsement-count user-endorsement-count]
   (if-not (pos? user-endorsement-count)
     (if (contains? params :lng)
       (badge-endorsement-modal-link (assoc params :endorsement-count endorsement-count))
       (badge-endorsement-modal-link (:badge-id params) endorsement-count))
     (let [endorsement-count (+ endorsement-count user-endorsement-count)]
       [:div.endorsement-link
        [:span [:i {:class "fa fa-handshake-o"}]]
        [:a {:href "#"
             :on-click #(do (.preventDefault %)
                            (mo/open-modal [:badge :endorsement] params))}
         (if (== endorsement-count 1)
           (str  endorsement-count " " (translate (:lng params) :badge/endorsement) #_(t :badge/endorsement))
           (str  endorsement-count " " (translate (:lng params) :badge/endorsements) #_(t :badge/endorsements)))]]))))

(defn is-user? [url]
  (let [base-url (str (session/get :site-url) (session/get :base-path))]
    (when-not (blank? url) (includes? url (str base-url "/profile/")))))

(defn issuer-modal-link-user
 ([issuer-id name url]
  (let [id (->> (split url #"/profile/") last long)]
    [:div {:class "issuer-data clearfix"}
     [:span._label {:class "pull-label-left"}  (t :badge/Issuedby) ":"]
     [:div {:class "issuer-links pull-label-left inline"}
      [:a {:href "#"
           :on-click #(do (.preventDefault %)
                          (mo/open-modal [:profile :view] {:user-id id} {}))} name]]]))
 ([issuer-id name lang url]
  (let [id (->> (split url #"/profile/") last long)]
   [:div {:class "issuer-data clearfix"}
    [:span._label {:class "pull-label-left"}  (translate lang :badge/Issuedby) ":"]
    [:div {:class "issuer-links pull-label-left inline"}
     [:a {:href "#"
          :on-click #(do (.preventDefault %)
                         (mo/open-modal [:profile :view] {:user-id id} {}))} name]]])))

(defn issuer-modal-link
  ([issuer-id name]
   [:div {:class "issuer-data clearfix"}
    [:span._label {:class "pull-label-left"}  (t :badge/Issuedby) ":"]
    [:div {:class "issuer-links pull-label-left inline"}
     [:a {:href "#"
          :on-click #(do (.preventDefault %)
                         (mo/open-modal [:badge :issuer] issuer-id {}))} name]]])
  ([issuer-id name lang]
   [:div {:class "issuer-data clearfix"}
    [:span._label {:class "pull-label-left"}  (translate lang :badge/Issuedby) ":"]
    [:div {:class "issuer-links pull-label-left inline"}
     [:a {:href "#"
          :on-click #(do (.preventDefault %)
                         (mo/open-modal [:badge :issuer] issuer-id {}))} name]]]))

(defn creator-modal-link-user
  ([creator-id name url]
   (when (and creator-id (not (blank? name)))
     (let [id (->> (split url #"/profile/") last long)]
      [:div {:class "issuer-data clearfix"}
       [:span._label.pull-left (t :badge/Createdby) ":"]
       [:div {:class "issuer-links pull-label-left inline"}
        [:a {:href "#"
             :on-click #(do (.preventDefault %)
                            (mo/open-modal [:profile :view] {:user-id id}))} name]]])))
  ([creator-id name lang url]
   (when (and creator-id (not (blank? name)))
     (let [id (->> (split url #"/profile/") last long)]
      [:div {:class "issuer-data clearfix"}
       [:span._label.pull-left (translate lang :badge/Createdby) ":"]
       [:div {:class "issuer-links pull-label-left inline"}
        [:a {:href "#"
             :on-click #(do (.preventDefault %)
                            (mo/open-modal [:profile :view] {:user-id id}))} name]]]))))

;;;TODO creator endorsements
(defn creator-modal-link
  ([creator-id name]
   (when (and creator-id (not (blank? name)))
     [:div {:class "issuer-data clearfix"}
      [:span._label.pull-left (t :badge/Createdby) ":"]
      [:div {:class "issuer-links pull-label-left inline"}
       [:a {:href "#"
            :on-click #(do (.preventDefault %)
                           (mo/open-modal [:badge :creator] creator-id))} name]]]))
  ([creator-id name lang]
   (when (and creator-id (not (blank? name)))
     [:div {:class "issuer-data clearfix"}
      [:span._label.pull-left (translate lang :badge/Createdby) ":"]
      [:div {:class "issuer-links pull-label-left inline"}
       [:a {:href "#"
            :on-click #(do (.preventDefault %)
                           (mo/open-modal [:badge :creator] creator-id))} name]]])))

(defn num-days-left [timestamp]
  (int (/ (- timestamp (/ (.now js/Date) 1000)) 86400)))

(defn save-rating [id state init-data rating]
  (ajax/POST
   (path-for (str "/obpv1/badge/save_rating/" id))
   {:params   {:rating  (if (pos? rating) rating nil)}
    :handler (fn []
               (init-data state id))}))

(defn follow-verified-bar [{:keys [verified_by_obf issued_by_obf badge_id obf_url]} context show-messages selfie_id]
  (let [visibility (if show-messages "hidden" "visible")]
    (if (= context "gallery")
      [:div.row.flip
       (if (or verified_by_obf issued_by_obf)
         (bh/issued-by-obf obf_url verified_by_obf issued_by_obf)
         (if selfie_id
           (into [:div.col-md-3];.selfie-stamp]
                 (for [f (plugin-fun (session/get :plugins) "block" "selfie_stamp")]
                   [f]))
          [:div.col-md-3]))
       [:div.col.md-9.text-right {:style {:padding-right "10px" :margin-right "auto" :visibility visibility}}
        [follow-badge badge_id]]]
      [:div.row.flip {:style {:padding "10px"}}
       [:div.col-md-3]
       [:div.col.md-9.pull-right {:style {:padding-right "10px" :margin-bottom "10px" :margin-right "auto"}}
        [follow-badge badge_id]]])))

(defn below-image-block [state endorsement_count]
  (let [{:keys [id view_count owner? badge_id message_count user-logged-in? congratulated? expires_on revoked user_endorsement_count]} @state
        invalid? (or (bh/badge-expired? expires_on) (pos? revoked))]
    [:div.badge-info-container
     ;view count
     (when (and owner? (pos? view_count))
       [:div.row {:id "badge-rating"}
        [:div.view-count
         [:i {:class "fa fa-eye"}]
         (cond
           (= view_count 1) (t :badge/Viewedonce)
           (> view_count 1) (str view_count " " (t :badge/Views)) #_(str (t :badge/Viewed) " " view_count " " (t :badge/times)))]])
     ;congratulate
     [:div.row
      [:div {:id "badge-congratulated"}
       (if (and user-logged-in? (not owner?))
         (if congratulated?
           [:div.congratulated
            [:span [:i {:class "fa fa-heart"}]]
            (str " " (t :badge/Congratulated))]
           [:button {:class    "btn btn-primary"
                     :on-click #(congratulate state)}
            [:i {:class "fa fa-heart"}]
            (str " " (t :badge/Congratulate) "!")]))]]
     ;messages
     #_(when-not invalid?
         (if user-logged-in?
           [:div.row
            [badge-message-link message_count  badge_id]]))

     ;endorsements
     [:div.row (badge-endorsement-modal-link {:badge-id badge_id :id id} endorsement_count user_endorsement_count)]]))


(defn badge-content [state]
  (let [{:keys [id badge_id  owner? visibility show_evidence rating issuer_image issued_on expires_on revoked issuer_content_id issuer_content_name issuer_content_url issuer_contact issuer_description first_name last_name description criteria_url criteria_content user-logged-in? congratulated? congratulations view_count evidence_url issued_by_obf verified_by_obf obf_url recipient_count assertion creator_content_id creator_name creator_image creator_url creator_email creator_description  qr_code owner message_count issuer-endorsements content endorsement_count endorsements evidences]} @state
        expired? (bh/badge-expired? expires_on)
        revoked (pos? revoked)
        show-recipient-name-atom (cursor state [:show_recipient_name])
        selected-language (cursor state [:content-language])
        metabadge-fn (first (plugin-fun (session/get :plugins) "metabadge" "metabadge"))
        {:keys [name description tags alignment criteria_content image_file image_file issuer_content_id issuer_content_name issuer_content_url issuer_contact issuer_image issuer_description criteria_url  creator_name creator_url creator_email creator_image creator_description message_count endorsement_count creator_content_id]} (content-setter @selected-language content)
        evidences (remove #(= true (get-in % [:properties :hidden])) evidences)]

    [:div {:id "badge-info" :class "row flip"}
     [:div {:class "col-md-3"}
      [:div.badge-image
       [:img {:src (str "/" image_file) :alt name}]]
      [below-image-block state endorsement_count]]
     [:div {:class "col-md-9 badge-info view-tab" :style {:display "block"}}
      [:div.row
       [:div {:class "col-md-12"}
        (if revoked
          [:div.revoked (t :badge/Revoked)])
        (if expired?
          [:div.expired [:span._label (str (t :badge/Expiredon) ":")] (date-from-unix-time (* 1000 expires_on))])
        [:h1.uppercase-header name]
        (if (< 1 (count (:content @state)))
          [:div.inline [:span._label (t :core/Languages) ": "] (content-language-selector selected-language (:content @state))])
        (if (is-user? issuer_content_url)
          (issuer-modal-link-user issuer_content_id issuer_content_name issuer_content_url)
          (issuer-modal-link issuer_content_id issuer_content_name))
        (when-not (= creator_name issuer_content_name)
          (if (is-user? creator_url)
            (creator-modal-link-user creator_content_id creator_name creator_url)
            (creator-modal-link creator_content_id creator_name)))
        (if (and issued_on (> issued_on 0))
          [:div [:span._label (t :badge/Issuedon) ": "]  (date-from-unix-time (* 1000 issued_on))])
        (if (and expires_on (not expired?))
          [:div [:span._label (t :badge/Expireson) ": "] (str (date-from-unix-time (* 1000 expires_on)) " (" (num-days-left expires_on) " " (t :badge/days) ")")])

        (if (pos? @show-recipient-name-atom)
          (if (and user-logged-in? (not owner?))
            [:div [:span._label (t :badge/Recipient) ": "]  [:a {:href "#" :on-click #(do (.preventDefault %) (mo/open-modal [:profile :view] {:user-id owner}))} #_{:href (path-for (str "/profile/" owner))} first_name " " last_name]]
            (if (and (= "public" @(cursor state [:profile_visibility])) (= 2 @show-recipient-name-atom))
              [:div [:span._label (t  :badge/Recipient) ": "] [:a.link {:href "#" :on-click #(do (.preventDefault %) (mo/open-modal [:profile :view] {:user-id owner})) #_(path-for (str "/profile/" owner))} first_name " " last_name]]
              [:div [:span._label (t  :badge/Recipient) ": "]  first_name " " last_name])))

        ;metabadges
        (when owner?
          (into [:div]
                (for [f (plugin-fun (session/get :plugins) "block" "meta_link")]
                  [f {:user_badge_id id}])))

        [:div.description description]

        ;;Check-badge
        (check-badge id)

        ;;Endorse-badge
        (when (and (session/get :user) (not owner?) (not expired?)) [endr/endorse-badge id])]]
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

      ;;evidence list
      (into [:div]
            (for [f (plugin-fun (session/get :plugins) "block" "evidence_list_badge")]
              [f id]))

      ;;map
      (into [:div]
            (for [f (plugin-fun (session/get :plugins) "block" "badge_info")]
              [f id]))]]))

(defn modal-navi [state]
  (let [invalid? (or (bh/badge-expired? (:expires_on @state)) (pos? (:revoked @state)))
        selected-language (cursor state [:content-language])
        data (content-setter @selected-language (:content @state))
        disable-link (if invalid? "btn disabled")
        disable-export (if (or (private?) invalid?) "btn disabled")]
    (fn []
      [:div.col-md-9.badge-modal-navi
       [:ul {:class "nav nav-tabs wrap-grid"}
        [:li.nav-item {:class  (if (or (nil? (:tab-no @state)) (= 1 (:tab-no @state))) "active")}
         [:a.nav-link {:href "#" :on-click #(do (init-data state (:id @state) 1))}
          [:div  [:i.nav-icon {:class "fa fa-eye fa-lg"}] (t :page/View)]]]
        [:li.nav-item {:class (if (= 2 (:tab-no @state)) "active")}
         [:a.nav-link {:class disable-link :href "#" :on-click #(show-settings-dialog (:id @state) state init-data "settings")}
          [:div  [:i.nav-icon {:class "fa fa-cogs fa-lg"}] (t :page/Settings)]]]
        #_[:li.nav-item {:class (if (= 3 (:tab-no @state)) "active")}
           [:a.nav-link {:class disable-link :href "#" :on-click #(show-settings-dialog (:id @state) state init-data "share")}
            [:div  [:i.nav-icon {:class "fa fa-share-alt fa-lg"}] (t :badge/Share)]]]
        [:li.nav-item {:class (if (= 3 (:tab-no @state)) "active")}
         (when (pos? @(cursor state [:notification])) [:span.badge.modal-navi-info @(cursor state [:notification])])
         [:a.nav-link {:class disable-export :href "#" :on-click #(show-settings-dialog (:id @state) state init-data "share") #_(do
                                                                                                                                  (swap! state assoc :tab [st/social-tab (assoc data :settings_fn show-settings-dialog :congratulations (:congratulations @state) :user_endorsement_count (:user_endorsement_count @state)) state init-data] :tab-no 3))}
          [:i.nav-icon {:class "fa fa-users fa-lg"}] (t :core/Social)]]
        [:li.nav-item {:class (if (= 4 (:tab-no @state)) "active")}
         [:a.nav-link {:class disable-export  :href "#" :on-click #(swap! state assoc :tab [se/download-tab-content (assoc data
                                                                                                                           :assertion_url (:assertion_url @state)
                                                                                                                           :obf_url (:obf_url @state)
                                                                                                                           :cert_list (:cert_list @state)) state] :tab-no 4)}
          [:div  [:i.nav-icon {:class "fa fa-download fa-lg"}] (t :core/Download)]]]

        [:li.nav-item {:class (if (= 6 (:tab-no @state)) "active")}
         [:a.nav-link.delete-button {:href "#" :on-click #(do
                                                            (swap! state assoc-in [:badge-settings :confirm-delete?] true)
                                                            (swap! state assoc :tab [se/delete-tab-content data state] :tab-no 6))}
          [:i.nav-icon {:class "fa fa-trash fa-lg"}] (t :core/Delete)]]]])))

(defn modal-top-bar [state]
  (let [{:keys [verified_by_obf issued_by_obf obf_url owner?]} @state]
    [:div.row.flip
     (if (or verified_by_obf issued_by_obf)
       (bh/issued-by-obf obf_url verified_by_obf issued_by_obf)
       [:div.col-md-3])
     (if owner? [modal-navi state])]))

(defn content [state]
  (let [{:keys [id owner? tab user-logged-in?]} @state
        selected-language (cursor state [:content-language])
        data (content-setter @selected-language (:content @state))]
    [:div
     (when (and (not owner?) user-logged-in?) [follow-verified-bar @state nil nil])
     [modal-top-bar state]
     (if tab tab [badge-content state])
     (if (and owner? (session/get :user)) "" [reporttool1 id  (:name data) "badge"])]))

(defn handler [params]
  (let [id (:badge-id params)
        data (:data params)
        state (atom {:initializing true
                     :permission "initial"})

        user (session/get :user)]
    (if data (init-data state id nil data) (init-data state id nil))

    (fn []
      (cond
        (= "initial" (:permission @state)) [:div]
        (and user (= "error" (:permission @state))) (err/error-content)
        (= "error" (:permission @state)) (err/error-content)
        (and (= "success" (:permission @state)) (:owner? @state) user) (content state)
        (and (= "success" (:permission @state)) user) (content state)
        :else (content state)))))

(def ^:export modalroutes
  {:badge {:info handler
           :metadata a/assertion-content
           :endorsement endr/badge-endorsement-content
           :issuer issuer/content
           :creator issuer/creator-content
           :linkedin1 s/linkedin-modal1
           :linkedin2 s/content-modal-render
           :endorse endr/endorse-badge-content
           :userbadgeendorsement endr/user-badge-endorsement-content
           :userendorsement endr/user-endorsement-content
           :extuserendorsement ext/ext-endorsement-content
           :requestendorsement endr/request-endorsement
           :my block/mybadgesmodal}})
