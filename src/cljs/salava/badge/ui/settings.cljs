(ns salava.badge.ui.settings
  (:require [reagent.core :refer [cursor atom]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.i18n :refer [t]]
            [salava.core.time :refer [date-from-unix-time]]
            [salava.core.ui.tag :as tag]
            [salava.core.ui.rate-it :as r]
            [salava.core.ui.helper :refer [plugin-fun private? navigate-to path-for js-navigate-to hyperlink url?]]
            [salava.badge.ui.helper :as bh]
            [salava.core.ui.share :as s]
            [reagent.session :as session]))

(defn set-visibility [visibility state]
  (swap! state assoc-in [:badge-settings :visibility] visibility))

(defn init-badges
  ([state]
   (ajax/GET
     (path-for "/obpv1/badge" true)
     {:handler (fn [data]
                 (swap! state assoc :badges (filter #(= "accepted" (:status %)) data)
                        :pending () ;(filter #(= "pending" (:status %)) data)
                        :initializing false))})))

(defn delete-badge [state]
  (ajax/DELETE
    (path-for (str "/obpv1/badge/" (:id @state)))
    {:handler  (fn []
                 #_(init-badges state)
                 #_(navigate-to "/badge"))}))

(defn export-to-pdf [state]
  (let [lang-option "all"
        badge-url (str "/obpv1/badge/export-to-pdf?badges[0]=" (:id @state) "&lang-option="lang-option)]
    (js-navigate-to badge-url)))

(defn update-settings [badge-id state]
  (ajax/GET
    (path-for (str "/obpv1/badge/settings/" badge-id) true)
    {:handler (fn [data]
                (swap! state assoc :badge-settings (assoc data :new-tag "")))}))

(defn save-settings [state]
 (let [{:keys [id visibility tags rating]} (:badge-settings @state)]
  (ajax/POST
    (path-for (str "/obpv1/badge/save_settings/" id))
    {:params  {:visibility   visibility
               :tags         tags
               :rating       (if (pos? rating) rating nil)}
     :handler (fn []
                (update-settings id state))})))


(defn toggle-recipient-name [id show-recipient-name-atom]
  (let [new-value (not @show-recipient-name-atom)]
    (ajax/POST
      (path-for (str "/obpv1/badge/toggle_recipient_name/" id))
      {:params {:show_recipient_name new-value}
       :handler (fn [] (reset! show-recipient-name-atom new-value))})))

(defn toggle-evidence [state]
  (let [id (get-in @state [:badge-settings :id])
        new-value (not (get-in @state [:badge-settings :show_evidence]))]
    (ajax/POST
      (path-for (str "/obpv1/badge/toggle_evidences_all/" id))
      {:params {:show_evidence new-value}
       :handler (fn [] (do

                         (swap! state assoc-in [:badge-settings :show_evidence] new-value)
                         (swap! state assoc :show_evidence new-value)))})))

(defn toggle-receive-notifications [badge_id notifications-atom]
  (let [req-path (if @notifications-atom
                   (str "/obpv1/social/delete_connection_badge/" badge_id)
                   (str "/obpv1/social/create_connection_badge/" badge_id))]
    (ajax/POST (path-for req-path)
               {:handler (fn [data]
                           (reset! notifications-atom (:connected? data)))})))

#_(defn visibility-form [state init-data]
    [:form {:class "form-horizontal"}
     [:div
      [:fieldset {:class "form-group visibility"}
       [:legend {:class "col-md-9 sub-heading"}
        (t :badge/Badgevisibility)]
       [:div {:class (str "col-md-12 " (get-in @state [:badge-settings :visibility]))}
        (if-not (private?)
          [:div [:input {:id              "visibility-public"
                         :name            "visibility"
                         :value           "public"
                         :type            "radio"
                         :on-change       #(do
                                             (set-visibility "public" state)
                                             (save-settings state init-data "share"))
                         :default-checked (= "public" (get-in @state [:badge-settings :visibility]))}]
           [:i {:class "fa fa-globe"}]
           [:label {:for "visibility-public"}
            (t :badge/Public)]])
        [:div [:input {:id              "visibility-internal"
                       :name            "visibility"
                       :value           "internal"
                       :type            "radio"
                       :on-change       #(do
                                           (set-visibility "internal" state)
                                           (save-settings state init-data "share"))
                       :default-checked (= "internal" (get-in @state [:badge-settings :visibility]))}]
         [:i {:class "fa fa-group"}]
         [:label {:for "visibility-internal"}
          (t :badge/Shared)]]
        [:div [:input {:id              "visibility-private"
                       :name            "visibility"
                       :value           "private"
                       :type            "radio"
                       :on-change       #(do
                                           (set-visibility "private" state)
                                           (save-settings state init-data "share"))
                       :default-checked (= "private" (get-in @state [:badge-settings :visibility]))}]
         [:i {:class "fa fa-lock"}]
         [:label {:for "visibility-private"}
          (t :badge/Private)]]]]]])

(defn delete-tab-content [{:keys [name image_file]} state]
  [:div {:id "badge-settings" :class "row flip"}
   [:div {:class "col-md-3 badge-image modal-left"}
    [:img {:src (str "/" image_file) :alt name}]]
   [:div {:class "col-md-9 delete-confirm delete-tab"}
    [:div {:class "alert alert-warning"}
     (t :badge/Confirmdelete)]
    [:div
     [:button {:type     "button"
               :class    "btn btn-primary"
               :data-dismiss "modal"
               :on-click #(swap! state assoc-in [:badge-settings :confirm-delete?] false)}
      (t :badge/Cancel)]
     [:button {:type         "button"
               :class        "btn btn-warning"
               :data-dismiss "modal"
               :on-click     #(delete-badge state)}
      (t :badge/Delete)]]]])

#_(defn share-tab-content [{:keys [id name image_file issued_on expires_on show_evidence revoked issuer_content_name]} state init-data]
    (let [expired? (bh/badge-expired? expires_on)
          revoked (pos? revoked)
          visibility (cursor state [:badge-settings :visibility])]
     [:div {:id "badge-settings" :class "row flip"}
      [:div {:class "col-md-3 badge-image modal-left"}
       [:img {:src (str "/" image_file) :alt name}]]
      [:div {:class "col-md-9 settings-content"}
       (if (and (not expired?) (not revoked))
         [visibility-form state init-data])
       [:div
        [:hr]
        [s/share-buttons-badge
         (str (session/get :site-url) (path-for (str "/badge/info/" id)))
         name
         (= "public" @visibility)
         true
         (cursor state [:show-link-or-embed])
         image_file
         {:name     name
          :authory  issuer_content_name
          :licence  (str (upper-case (replace (session/get :site-name) #"\s" "")) "-" id)
          :url      (str (session/get :site-url) (path-for (str "/badge/info/" id)))
          :datefrom issued_on
          :dateto   expires_on}]]

       (into [:div
               (for [f (plugin-fun (session/get :plugins) "block" "badge_share")]
                 [f id])])]]))

(defn settings-tab-content [data state init-data]
  (let [{:keys [id name image_file issued_on expires_on show_evidence revoked rating]} data
        expired? (bh/badge-expired? expires_on)
        show-recipient-name-atom (cursor state [:show_recipient_name])
        notifications-atom (cursor state [:receive-notifications])
        revoked (pos? revoked)
        badge_id (:badge_id @state)]
    [:div {:id "badge-settings" :class "row flip"}
     [:div {:class "col-md-3 badge-image modal-left"}
      [:img {:src (str "/" image_file) :alt name}]]
     [:div {:class "col-md-9 settings-content settings-tab"}
      (cond
        revoked [:div.revoked (t :badge/Revoked)]
        expired? [:div.expired (t :badge/Expiredon) ": " (date-from-unix-time (* 1000 expires_on))]
        (and (not revoked) (not expired?)) [:div [:div {:class "form-horizontal"}
                                                  #_[:div.form-group
                                                     [:fieldset {:class "col-md-9 checkbox"}
                                                      [:legend {:class "col-md-9 sub-heading"}
                                                       (t :badge/Rating)]
                                                      [:div.col-md-12
                                                       {:on-click #(save-raiting id state init-data (get-in @state [:badge-settings :rating]))}
                                                       [r/rate-it rating (cursor state [:badge-settings :rating])]]]]

                                                  [:div.form-group
                                                   [:fieldset {:class "col-md-9 checkbox"}
                                                    [:div.col-md-12 [:label {:for "show-name"}
                                                                     [:input {:type      "checkbox"
                                                                              :id        "show-name"
                                                                              :on-change #(toggle-recipient-name id show-recipient-name-atom)
                                                                              :checked   @show-recipient-name-atom}]
                                                                     (t :badge/Showyourname)]]]]


                                                  [:div.form-group
                                                   [:fieldset {:class "col-md-9 checkbox"}
                                                    [:div.col-md-12 [:label {:for "receive-notifications"}
                                                                     [:input {:type      "checkbox"
                                                                              :id        "receive-notifications"
                                                                              :on-change #(toggle-receive-notifications badge_id notifications-atom)
                                                                              :checked   @notifications-atom}]
                                                                     (str (t :social/Getbadgenotifications) #_(t :social/Followbadge))]]]]
                                                  [:div
                                                   [:div {:class "row"}
                                                    [:label {:class "col-md-12 sub-heading" :for "newtags"}
                                                     (t :badge/Tags)]]
                                                   [:div {:class "row"}
                                                    [:div {:class "col-md-12"}
                                                     [tag/tags (cursor state [:badge-settings :tags]) state init-data]]]
                                                   [:div {:class "form-group"}
                                                    [:div {:class "col-md-12"}
                                                     [tag/new-tag-input (cursor state [:badge-settings :tags]) (cursor state [:badge-settings :new-tag]) state init-data]]]

                                                   (into [:div]
                                                         (for [f (plugin-fun (session/get :plugins) "block" "evidence_block")]
                                                           [f data state init-data]))

                                                   (into [:div]
                                                         (for [f (plugin-fun (session/get :plugins) "block" "badge_settings")]
                                                           [f id]))]]

                                            [:div.modal-footer]])]]))

(defn download-tab-content [{:keys [name image_file obf_url assertion_url]} state]
  [:div {:id "badge-settings" :class "row flip"}
   [:div {:class "col-md-3 badge-image modal-left"}
    [:img {:src (str "/" image_file) :alt name}]]
   [:div {:class "col-md-9 settings-content download-tab"}
    [:div
     [:button {:class "btn btn-primary" :on-click  #(export-to-pdf state)} (t :badge/Downloadpdf)]
     [:div (t :badge/Pdfdownload)]]
    [:hr]
    [:div
     [:a {:class "btn btn-primary" :href (str obf_url "/c/receive/download?url="(js/encodeURIComponent assertion_url))} (t :badge/Downloadbadgeimage)]
     [:div (t :badge/Downloadbakedbadge)]]]])
