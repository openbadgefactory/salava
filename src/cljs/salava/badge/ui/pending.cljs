(ns salava.badge.ui.pending
  (:require [reagent.core :refer [atom cursor create-class]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.helper :refer [path-for plugin-fun private?]]
            [salava.core.ui.content-language :refer [init-content-language content-language-selector content-setter]]
            [salava.badge.ui.helper :as bh]
            [salava.badge.ui.modal :as bm]
            [salava.core.time :refer [date-from-unix-time]]
            [reagent.session :as session]
            [salava.core.ui.popover :refer [info]]
            [reagent-modals.modals :as m]
            #_[salava.metabadge.ui.metabadge :as mb]))

(defn init-data [state]
  (ajax/GET
    (path-for "/obpv1/social/pending_badges" true)
    {:handler (fn [data]
                (swap! state assoc :spinner false :pending-badges (:pending-badges data)))}))


(defn- init-badge-preview [state]
  (ajax/GET
    (path-for (str "/obpv1/badge/info/" (:id @state)))
    {:handler (fn [data]
                (swap! state assoc :result data
                       :id (:id @state)
                       :content-language (init-content-language (:content data))))}))

(defn update-status [id new-status state reload-fn]
  (ajax/POST
    (path-for (str "/obpv1/badge/set_status/" id))
    {:response-format :json
     :keywords? true
     :params {:status new-status}
     :handler (fn []
                  (js/setTimeout (fn [] (swap! state assoc :badge-alert nil)) 2000)
                  (if reload-fn (reload-fn state)))
     :error-handler (fn [{:keys [status status-text]}])}))

(defn update-visibility [visibility badge state]
  (swap! state assoc :badge-alert nil)
  (ajax/POST
    (path-for (str "/obpv1/badge/set_visibility/" (:id badge)))
    {:params {:visibility visibility}
     :handler (fn [data]
                (when (= (:status data) "success")
                  (update-status (:id badge) "accepted" state nil)
                  (swap! state assoc :badge-alert "accepted" :badge-name (:name badge))))}))

(defn num-days-left [timestamp]
  (int (/ (- timestamp (/ (.now js/Date) 1000)) 86400)))

(defn- show-more-content [state]
  (fn []
    (let [ data (:result @state)
           selected-language (cursor state [:content-language])
           {:keys [id badge_id issued_on expires_on issued_by_obf verified_by_obf obf_url first_name last_name content]}  data
           expired?                                                                 (bh/badge-expired? expires_on)
           show-recipient-name-atom                                                 (cursor state [:result :show_recipient_name])
           {:keys [name description tags alignment criteria_content image_file issuer_content_id issuer_content_name issuer_content_url issuer_contact issuer_image issuer_description criteria_url creator_content_id creator_name creator_url creator_email creator_image creator_description message_count endorsement_count]} (content-setter @selected-language content)]
      [:div {:class "preview-badge" :style {:display (:show-result @state)}}
       [:div.row.flip
        [:div.col-md-9.badge-info
         (if (< 1 (count content))
           [:div.inline [:label (t :core/Languages)": "](content-language-selector selected-language content)])
         (bm/issuer-modal-link issuer_content_id issuer_content_name)
         (bm/creator-modal-link creator_content_id creator_name)
         (if (and issued_on (> issued_on 0))
           [:div [:span._label (t :badge/Issuedon) ": "]  (date-from-unix-time (* 1000 issued_on))])
         (if (and expires_on (not expired?))
           [:div [:span._label (t :badge/Expireson) ": "] (str (date-from-unix-time (* 1000 expires_on)) " ("(num-days-left expires_on) " " (t :badge/days)")")])
         (if (pos? @show-recipient-name-atom)
           [:div [:span._label (t :badge/Recipient) ": "]  first_name " " last_name])
         ;[:div [mb/metabadge (:assertion_url @state)]]
         [:div {:class "criteria-html"}
          [:h2.uppercase-header (t :badge/Criteria)]
          [:a {:href criteria_url :target "_blank"} (t :badge/Opencriteriapage) "..."]
          [:div {:dangerouslySetInnerHTML {:__html criteria_content}}]]]]])))

(defn- show-more [state]
  (fn []
    [:div
     [:br]
     [:a {:href "#"
          :style {:display (:show-link @state)}
          :on-click #(do
                       (swap! state assoc :show-result "block"
                              :show-link "none")
                       (init-badge-preview state))}
         (t :admin/Showmore)]
     [show-more-content state]]))

(defn pending-badge-content [{:keys [id image_file name description visibility assertion_url meta_badge meta_badge_req issuer_content_name issuer_content_url issued_on issued_by_obf verified_by_obf obf_url]}]
  (let [state (atom {:id id
                     :show-result "none"
                     :show-link "block"
                     :result {}
                     :assertion_url assertion_url
                     :visibility visibility})
        metabadge-fn (first (plugin-fun (session/get :plugins) "metabadge" "metabadge"))]
    (init-badge-preview state)
    (fn []
      (let [data (:result @state)
            selected-language (cursor state [:content-language])
            {:keys [badge_id content assertion_url]} data
            {:keys [name description tags alignment criteria_content image_file issuer_content_id issuer_content_name issuer_content_url issuer_contact issuer_image issuer_description criteria_url creator_content_id creator_name creator_url creator_email creator_image creator_description message_count endorsement_count ]} (content-setter @selected-language content)]
        [:div#badge-info
         (if (or verified_by_obf issued_by_obf)
           [:div.row.flip
            (bh/issued-by-obf obf_url verified_by_obf issued_by_obf)])

         [:div.row.flip
          [:div.col-md-3.badge-image
           [:img.badge-image {:src (str "/" image_file) :alt ""}]
           (bm/badge-endorsement-modal-link badge_id endorsement_count)]

          [:div.col-md-9
           [:h1.media-heading {:style {:font-size "45px"}} name]

           [:div.description description]

           ;METABADGE
           (into [:div {:style {:margin "10px -10px"}}]
             (for [f (plugin-fun (session/get :plugins) "block" "meta_link")]
               [f {:user_badge_id id}]))

           [show-more state]]]]))))

(defn badge-alert [state]
  (if (:badge-alert @state)
    [:div {:class "alert alert-success"}
     (case (:badge-alert @state)
       "accepted"  [:div (str (t :badge/Youhaveaccepted) " \"" (:badge-name @state) "\". ") (t :badge/Youcanfind)]
       "declined" (t :badge/Badgedeclined)
       "")]))

(defn visibility-modal [badge state reload-fn]
  (let [visibility (atom (:visibility badge))]
    (create-class {:reagent-render
                   (fn [] [:div#badge-settings {:style {:padding "10px"}}
                           [:form {:class "form-horizontal"}
                            [:div ;{:class "col-md-12"}
                             [:fieldset {:class "form-group visibility"}
                              [:legend {:class "col-md-9 sub-heading"}
                               (t :badge/Badgevisibility) [info {:content (t :badge/Visibilityinfo) :placement "right"}]]
                              [:div {:class (str "col-md-12 " @visibility) :style {:margin-top "20px"}}
                               (if-not (private?)
                                 [:div [:input {:id              "visibility-public"
                                                :name            "visibility"
                                                :value           "public"
                                                :type            "radio"
                                                :on-change       #(do
                                                                    (.preventDefault %)
                                                                    (reset! visibility "public"))}]
                                  [:i {:class "fa fa-globe"}]
                                  [:label {:for "visibility-public"}
                                   (t :badge/Public)]])
                               [:div [:input {:id              "visibility-internal"
                                              :name            "visibility"
                                              :value           "internal"
                                              :type            "radio"
                                              :on-change       #(do
                                                                  (.preventDefault %)
                                                                  (reset! visibility "internal"))}]
                                [:i {:class "fa fa-group"}]
                                [:label {:for "visibility-internal"}
                                 (t :badge/Shared)]]
                               [:div [:input {:id              "visibility-private"
                                              :name            "visibility"
                                              :value           "private"
                                              :type            "radio"
                                              :on-change       #(do
                                                                  (.preventDefault %)
                                                                  (reset! visibility "private"))
                                              :default-checked (= "private" (:visibility badge)) #_(= "private" (:visibility badge) #_(get-in @state [:badge-settings :visibility]))}]
                                [:i {:class "fa fa-lock"}]
                                [:label {:for "visibility-private"}
                                 (t :badge/Private)]]]]]]

                           (into [:div]
                                 (for [f (plugin-fun (session/get :plugins) "block" "badge_share")]
                                   [f (:id badge)]))

                           [:hr.border]
                           [:button.btn.btn-primary {:on-click #(do
                                                                  (.preventDefault %)
                                                                  (update-visibility @visibility badge state))

                                                     :data-dismiss "modal"}(t :core/Save)]])

                   :component-will-unmount (fn [] (m/close-modal!))})))

(defn badge-pending [badge state reload-fn]
  [:div.row {:key (:id badge)}
   [:div.col-md-12
    [:div.badge-container-pending.thumbnail
     [pending-badge-content badge]
     [:div {:class "row button-row"}
      [:div.col-md-12
       [:button {:class "btn btn-primary"
                 :on-click #(do
                              (m/modal! [visibility-modal badge state reload-fn] {:size :md :hidden (fn [] (reload-fn state))})
                              (.preventDefault %))

                 :data-dismiss "modal"}
        (t :badge/Acceptbadge)]
       [:button {:class "btn btn-warning"
                 :on-click #(do
                              (update-status (:id badge) "declined" state reload-fn)
                              (.preventDefault %)
                              (swap! state assoc :badge-alert "declined" :badge-name (:name badge)))}
        (t :badge/Declinebadge)]]]]]])

(defn badges-pending [state reload-fn]
  (if (:spinner @state)
    [:div.ajax-message
     [:i {:class "fa fa-cog fa-spin fa-2x "}]
     [:span (str (t :core/Loading) "...")]
     [:hr]]
    (into [:div {:id "pending-badges"}]
          (for [badge (:pending @state)]
            (badge-pending badge state reload-fn)))))
