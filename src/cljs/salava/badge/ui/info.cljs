(ns salava.badge.ui.info
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
            [salava.badge.ui.settings :as se]
            [salava.core.ui.rate-it :as r]
            [salava.core.ui.share :as s]
            [salava.core.helper :refer [dump]]
            [salava.user.ui.helper :as uh]
            [salava.core.ui.modal :as mo]
            [salava.core.ui.helper :refer [path-for private? hyperlink url? plugin-fun]]
            [salava.core.time :refer [date-from-unix-time unix-time unix-time]]
            [salava.admin.ui.admintool :refer [admintool]]
            [salava.social.ui.follow :refer [follow-badge]]
            [salava.core.ui.error :as err]
            [salava.core.ui.content-language :refer [init-content-language content-language-selector content-setter]]
            [salava.social.ui.badge-message-modal :refer [badge-message-link]]
            [salava.admin.ui.reporttool :refer [reporttool1]]
            [salava.badge.ui.verify :refer [check-badge]]
            [salava.metabadge.ui.metabadge :refer [metabadge]]
            [salava.badge.ui.evidence :refer [evidence-icon]]
            [dommy.core :as dommy :refer-macros [sel1]]
            [salava.translator.ui.helper :refer [translate]]))


(defn init-endorsement-count [id state]
  (ajax/GET
    (path-for (str "/obpv1/badge/user/endorsement/count/" id))
    {:handler (fn [{:keys [user_endorsement_count]}] (reset! (cursor state [:user_endorsement_count]) user_endorsement_count))}))

(defn init-data [state id]
  (ajax/GET
    (path-for (str "/obpv1/badge/info/" id))
    {:handler (fn [data]
                (reset! state (assoc data :id id
                                :show-link-or-embed-code nil
                                :initializing false
                                :content-language (init-content-language (:content data))
                                :permission "success"))
                (init-endorsement-count id state))}
    (fn [] (swap! state assoc :permission "error"))))

(comment
  (defn toggle-visibility [state]
    (let [id (:id @state)
          new-value (if (= (:visibility @state) "private") "public" "private")]
      (ajax/POST
        (path-for (str "/obpv1/badge/set_visibility/" id))
        {:params {:visibility new-value}
         :handler (fn [] (swap! state assoc :visibility new-value))})))

  (defn toggle-recipient-name [id show-recipient-name-atom]
    (let [new-value (not (pos? @show-recipient-name-atom))]
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

  (defn show-settings-dialog [badge-id state init-data]
    (ajax/GET
      (path-for (str "/obpv1/badge/settings/" badge-id) true)
      {:handler (fn [data]
                  (swap! state assoc :badge-settings data (assoc data :new-tag ""))
                  (m/modal! [se/settings-modal data state init-data true]
                            {:size :lg}))}))
  (defn congratulate [state]
    (ajax/POST
      (path-for (str "/obpv1/badge/congratulate/" (:id @state)))
      {:handler (fn [] (swap! state assoc :congratulated? true))}))

  (defn badge-endorsement-modal-link [badge-id endorsement-count]
    [:div.row
     [:div.col.xs-12
      [:hr.endorsementhr]
      [:a.endorsementlink {:class "endorsement-link"
                           :href "#"
                           :on-click #(do (.preventDefault %)
                                        (mo/open-modal [:badge :endorsement] badge-id))}
       (if (== endorsement-count 1)
         (str  endorsement-count " " (t :badge/endorsement))
         (str  endorsement-count " " (t :badge/endorsements)))]]])

  (defn issuer-modal-link [issuer-id name]
    [:div {:class "issuer-data clearfix"}
     [:label {:class "pull-label-left"}  (t :badge/Issuedby) ":"]
     [:div {:class "issuer-links pull-label-left inline"}
      [:a {:href "#"
           :on-click #(do (.preventDefault %)
                        (mo/open-modal [:badge :issuer] issuer-id))} name]]])

  (defn creator-modal-link [creator-id name]
    [:div {:class "issuer-data clearfix"}
     [:label.pull-left (t :badge/Createdby) ":"]
     [:div {:class "issuer-links pull-label-left inline"}
      [:a {:href "#"
           :on-click #(do (.preventDefault %)
                        (mo/open-modal [:badge :creator] creator-id))} name]]]))

(defn save-rating [id state init-data rating]
  (ajax/POST
    (path-for (str "/obpv1/badge/save_rating/" id))
    {:params   {:rating  (if (pos? rating) rating nil)}
     :handler (fn []
                (init-data state id))}))

(defn num-days-left [timestamp]
  (int (/ (- timestamp (/ (.now js/Date) 1000)) 86400)))



(defn content [state]
  (let [{:keys [id badge_id  owner? visibility show_evidence rating issued_on expires_on
                revoked first_name last_name user-logged-in? congratulated? congratulations
                view_count issued_by_obf verified_by_obf obf_url
                recipient_count assertion  qr_code owner message_count content issuer-endorsements evidences user_endorsement_count]} @state
        expired?                                                                 (bh/badge-expired? expires_on)
        show-recipient-name-atom                                                 (cursor state [:show_recipient_name])
        revoked                                                                  (pos? revoked)
        selected-language                                                        (cursor state [:content-language])
        {:keys [name description tags alignment criteria_content image_file
                issuer_content_id issuer_content_name issuer_content_url issuer_contact
                issuer_image issuer_description criteria_url
                creator_content_id creator_name creator_url creator_email
                creator_image creator_description message_count endorsement_count default_language_code]} (content-setter @selected-language content)
        evidences (remove #(= true (get-in % [:properties :hidden])) evidences)
        lng (->> (remove blank? (list @(cursor state [:content-language]) default_language_code "en")) first)]
   (if (= lng "ar") (dommy/set-attr! (sel1 :html) :dir "rtl") (dommy/set-attr! (sel1 :html) :dir "ltr"))
   [:div {:id "badge-info"}
    [m/modal-window]
    [:div.panel
     [:div.panel-body
      (comment
        (if (and owner? (not expired?) (not revoked))
          [:div {:class "row" :id "badge-share-inputs"}
           (if-not (private?)
             [:div.pull-left
              [:div {:class (str "checkbox " visibility)}
               [:a.link {:href "#" :on-click #(do (.preventDefault %) (show-settings-dialog id state init-data))}
                [:i {:class "fa"}]
                (if (not (= visibility "public"))
                  (t :core/Publishandshare)
                  (t :core/Public))]]])

           [:div {:class "pull-right text-right"}
            [follow-badge badge_id]
            [:button {:class    "btn btn-primary settings-btn"
                      :on-click #(do (.preventDefault %) (show-settings-dialog id state init-data))}
             (t :badge/Settings)]
            [:button {:class    "btn btn-primary print-btn"
                      :on-click #(.print js/window)}
             (t :core/Print)]]

           [:div.share-wrapper
            [s/share-buttons-badge
             (str (session/get :site-url) (path-for (str "/badge/info/" id)))
             name
             (= "public" visibility)
             true
             (cursor state [:show-link-or-embed])
             image_file
             {:name     name
              :authory  issuer_content_name
              :licence  (str (upper-case (replace (session/get :site-name) #"\s" "")) "-" id)
              :url      (str (session/get :site-url) (path-for (str "/badge/info/" id)))
              :datefrom issued_on
              :dateto   expires_on}]]]))

      (if (and (not expired?) (not revoked))
        (admintool id "badge"))

      (if (or verified_by_obf issued_by_obf)
        [:div.row (bh/issued-by-obf obf_url verified_by_obf issued_by_obf)])
      [:div {:class "row flip"}
       [:div {:id "pull-right" :class "col-md-3 badge-image"}
        [:div.row
         [:div.col-xs-12
          [:img {:src (str "/" image_file)}]]]
        (when (and qr_code (= visibility "public"))
          [:img#print-qr-code {:src (str "data:image/png;base64," qr_code)}])
        (comment
          (if owner?
            [:div.row {:id "badge-rating"}
             [:div.col-xs-12
              [:div.rating
               [:div (t :badge/Rating)]
               [:div
                {:on-click #(save-rating id state init-data (get-in @state [:badge-settings :rating]))}
                [r/rate-it rating (cursor state [:badge-settings :rating])]]]
              (if (and expires_on (not expired?))
                [:div.expiresin [:i {:class "fa fa-hourglass-half"}] (str (t :badge/Expiresin) " " (num-days-left expires_on) " " (t :badge/days))])
              [:div.view-count
               (cond
                 (= view_count 1) (t :badge/Viewedonce)
                 (> view_count 1) (str (t :badge/Viewed) " " view_count " " (t :badge/times))
                 :else            (t :badge/Badgeisnotviewedyet))]]])
          (if (> recipient_count 1)
            [:div.row {:id "badge-views"}
             [:div.col-xs-12
              [:a.link {:href     "#"
                        :on-click #(do
                                     (mo/open-modal [:gallery :badges] {:badge-id badge_id})
                                     (.preventDefault %))} (t :badge/Otherrecipients)]]])
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
                 (str " " (t :badge/Congratulate) "!")]))]])


        [:div#info-page-block #_(if (session/get :user)
                                 [badge-message-link message_count badge_id])
           (bm/badge-endorsement-modal-link {:badge-id badge_id :id id :lng lng} endorsement_count user_endorsement_count)]]


       [:div {:class "col-md-9 badge-info" :style {:display "block"}}
        [:div.row
         [:div {:class "col-md-12"}
          (if revoked
            [:div.revoked (translate lng :badge/Revoked)])
          (if expired?
            [:div.expired [:label (translate lng :badge/Expiredon) ": "] (date-from-unix-time (* 1000 expires_on))])
          [:h1.uppercase-header name]
          (if (< 1 (count (:content @state)))
            [:div.inline [:label (translate lng :core/Languages)": "](content-language-selector selected-language (:content @state))])
          (bm/issuer-modal-link issuer_content_id issuer_content_name lng)
          (bm/creator-modal-link creator_content_id creator_name lng)

          (if (and issued_on (> issued_on 0))
            [:div [:label (translate lng :badge/Issuedon) ": "]  (date-from-unix-time (* 1000 issued_on))])
          (if (and expires_on (not expired?))
            [:div [:label (translate lng :badge/Expireson) ": "] (str (date-from-unix-time (* 1000 expires_on)) " ("(num-days-left expires_on) " " (t :badge/days)")")])

          (comment
            (if assertion
              [:div {:id "assertion-link"}
               [:label (t :badge/Metadata)": "]
               [:a.link {:href     "#"
                         :on-click #(do (.preventDefault %)
                                      (m/modal! [a/assertion-modal (dissoc assertion :evidence)] {:size :lg}))}
                (t :badge/Openassertion) "..."]]))

          (if (pos? @show-recipient-name-atom)
            (if (and user-logged-in? (not owner?))
              [:div [:label (t :badge/Recipient) ": " ] [:a.link {:href (path-for (str "/profile/" owner))} first_name " " last_name]]
              [:div [:label (translate lng :badge/Recipient) #_(t :badge/Recipient) ": "]  first_name " " last_name]))

          #_[:div [metabadge (:assertion_url @state)]]

          [:div.description description]

          ;check-badge-link
          (check-badge id lng)]]

        (when-not (empty? alignment)
          [:div.row
           [:div.col-md-12
            [:h2.uppercase-header (translate lng :badge/Alignments) #_(t :badge/Alignments)]
            (doall
              (map (fn [{:keys [name url description]}]
                     [:p {:key url}
                      [:a {:target "_blank" :rel "noopener noreferrer" :href url} name] [:br] description])
                   alignment))]])

        [:div {:class "row criteria-html"}
         [:div.col-md-12
          [:h2.uppercase-header (translate lng :badge/Criteria)]
          [:a.link {:href criteria_url :target "_blank"} (translate lng :badge/Opencriteriapage) #_(t :badge/Opencriteriapage) "..."]
          [:div {:dangerouslySetInnerHTML {:__html criteria_content}}]]]

        (when (seq evidences)
          [:div.row {:id "badge-settings"}
           [:div.col-md-12
            [:h2.uppercase-header (translate lng :badge/Evidences) #_(t :badge/Evidences)]
            (reduce (fn [r evidence]
                      (let [{:keys [narrative description name id url mtime ctime properties]} evidence
                            added-by-user? (and (not (blank? description)) (starts-with? description "Added by badge recipient")) ;;use regex
                            {:keys [resource_id resource_type mime_type hidden]} properties
                            desc (cond
                                   (not (blank? narrative)) narrative
                                   (not added-by-user?) description ;;todo use regex to match description
                                   :else nil)]

                        (conj r (when (and (not hidden) (url? url))
                                  [:div.modal-evidence
                                   (when-not added-by-user? [:span.label.label-success (translate lng :badge/Verifiedevidence) #_(t :badge/Verifiedevidence)])
                                   [evidence-icon {:type resource_type :mime_type mime_type}]
                                   [:div.content

                                    (when-not (blank? name) [:div.content-body.name name])
                                    (when-not (blank? desc) [:div.content-body.description {:dangerouslySetInnerHTML {:__html desc}}])
                                    [:div.content-body.url
                                     (case resource_type
                                       "file" (hyperlink url)
                                       "page" (if (session/get :user)
                                                [:a {:href "#"
                                                     :on-click #(do
                                                                  (.preventDefault %)
                                                                  (mo/open-modal [:page :view] {:page-id resource_id}))} url]
                                                (hyperlink url))
                                       (hyperlink url))]]]))))

                    [:div ] evidences)]])

        (if (and owner? (not-empty congratulations))
          [:div.row
           [:div.col-md-12 {:id "badge-congratulations"}
            [:h3.congratulated-header
             [:i {:class "fa fa-heart"}]
             " " (translate lng :badge/Congratulatedby) #_(t :badge/Congratulatedby) ":"]
            (into [:div]
                  (for [congratulation congratulations
                        :let           [{:keys [id first_name last_name profile_picture]} congratulation]]
                    (uh/profile-link-inline id first_name last_name profile_picture)))]])

        (into [:div]
              (for [f (plugin-fun (session/get :plugins) "block" "badge_info")]
                [f id lng]))]]

      (if owner? "" [reporttool1 id name "badge"])]]]))




(defn handler [site-navi params]
  (let [id (:badge-id params)
        state (atom {:initializing true
                     :permission "initial"})

        user (session/get :user)]
    (init-data state id)
    (fn []

      (cond
        (= "initial" (:permission @state)) [:div]
        (and user (= "error" (:permission @state))) (layout/default-no-sidebar site-navi (err/error-content))
        (= "error" (:permission @state)) (layout/landing-page site-navi (err/error-content))
        (and (= "success" (:permission @state)) (:owner? @state) user) (layout/default site-navi (content state))
        (and (= "success" (:permission @state)) user) (layout/default-no-sidebar site-navi (content state))
        :else (layout/landing-page site-navi (content state))))))
