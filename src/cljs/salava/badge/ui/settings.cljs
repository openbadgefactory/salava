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
            [reagent.session :as session]
            [clojure.string :refer [upper-case replace blank? starts-with?]]
            [salava.core.ui.rate-it :as r]
            [salava.badge.ui.my :as my]
            [salava.core.ui.modal :as mo]
            [salava.badge.ui.evidence :as evidence]))


(defn set-visibility [visibility state]
  (swap! state assoc-in [:badge-settings :visibility] visibility))

(defn delete-badge [state]
  (ajax/DELETE
    (path-for (str "/obpv1/badge/" (:id @state)))
    {:handler  (fn []
                 (my/init-data state)
                 (navigate-to "/badge"))}))

(defn export-to-pdf [state]
  (let [lang-option "all"
        badge-url (str "/obpv1/badge/export-to-pdf?badges[0]=" (:id @state) "&lang-option="lang-option)]
    (ajax/GET
      (path-for (str "/obpv1/badge/export-to-pdf"))
      {:params {:badges (list (:id @state)) :lang-option lang-option }
       :handler (js-navigate-to badge-url)})))

(defn update-settings [badge-id state]
  (ajax/GET
    (path-for (str "/obpv1/badge/settings/" badge-id) true)
    {:handler (fn [data]
                (swap! state assoc :badge-settings data (assoc data :new-tag "")))}))


(defn save-raiting [id state init-data raiting]
  (ajax/POST
    (path-for (str "/obpv1/badge/save_raiting/" id))
    {:params   {:rating  (if (pos? raiting) raiting nil)}
     :handler (fn []
                (update-settings id state)
                #_(init-data state id (:tab-no @state)))}))


(defn save-settings [state init-data context]
  (let [{:keys [id visibility tags rating]} (:badge-settings @state)]
    (ajax/POST
      (path-for (str "/obpv1/badge/save_settings/" id))
      {:params  {:visibility   visibility
                 :tags         tags
                 :rating       (if (pos? rating) rating nil)}
       :handler (fn []
                  (case context
                    "share" (update-settings id state)
                    (init-data state id nil)))})))


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

(defn visibility-form [state init-data]
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
         [:i {:class "fa fa-globe" }]
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
       [:i {:class "fa fa-group" }]
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
       [:i {:class "fa fa-lock" }]
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

(defn share-tab-content [{:keys [id name image_file issued_on expires_on show_evidence revoked issuer_content_name]} state init-data]
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
      ]]))

(declare settings-tab-content)

(defn evidence-form [data state init-data]
  (let [evidence-name-atom (cursor state [:evidence :name])
        evidence-narrative-atom (cursor state [:evidence :narrative])
        evidence-url-atom (cursor state [:evidence :url])
        message (cursor state [:evidence :message])
        input-mode (cursor state [:input_mode])
        {:keys [image_file name]} data]

    [:div {:id "badge-settings" :class "row flip"}
     [:div {:class "col-md-3 badge-image modal-left"}
      [:img {:src (str "/" image_file) :alt name}]
      ]
     [:div {:class "col-md-9 settings-content settings-tab"}
      (if @message [:div @message])
      [:div
       [:div.row {:style {:margin-bottom "10px"}} [:label.col-md-9.sub-heading (t :badge/Addnewevidence)]]
       [:div.evidence-info (t :badge/Evidenceinfo)]

       [:div.row.form-horizontal
        [:div
         [:div.col-md-12.resource-options
          [:div
           [:div [:a {:class (if (= :url_input @input-mode) "active-resource" "")
                      :href "#"
                      :on-click #(do
                                   (.preventDefault %)
                                   (evidence/toggle-input-mode :url_input state))}
                  [:i.fa.fa-link] (t :admin/Url)]]
           [:div  [:a {:class (if (= :page_input @input-mode) "active-resource" "")
                       :href "#"
                       :on-click #(do
                                    (.preventDefault %)
                                    (evidence/toggle-input-mode :page_input state)
                                    )
                       }
                   [:i.fa.fa-file-text] (t :page/Pages)]]
           [:div  [:a {:class (if (= :file_input @input-mode) "active-resource" "")
                       :href "#":on-click #(do
                                             (.preventDefault %)
                                             (evidence/toggle-input-mode :file_input state))
                       } [:i.fa.fa-files-o] (t :page/Files)]]
           [:div.cancel [:a {:href "#":on-click #(do
                                                   (.preventDefault %)
                                                   (swap! state assoc :tab [settings-tab-content (dissoc data :evidence) state init-data]
                                                          :tab-no 2))}

                         [:i.fa.fa-remove] (t :core/Cancel)]]]]]


        ;;Preview
        (when @(cursor state [:show-preview])
          [:div.preview.evidence-list.col-md-9
           [:div.panel.panel-default
            [:div.panel-heading
             [:div.panel-title
              [:div.url  [evidence/evidence-icon @evidence-url-atom (cursor state [:evidence :properties :mime_type])]] (hyperlink @evidence-url-atom)]
             [:div [:button {:type "button"
                             :aria-label "OK"
                             :class "close panel-close"
                             :on-click #(do (.preventDefault %)(swap! state assoc :show-preview false
                                                                      :evidence nil
                                                                      :show-form false))}
                    [:i.fa.fa-trash.trash]]]]
            [:div.panel-body.evidence-panel-body
             (if (and (not @(cursor state [:show-form]))(every? #(blank? %)(vector @evidence-name-atom @evidence-narrative-atom)))
               [:div [:a {:href "#" :on-click #(do
                                                 (.preventDefault %)
                                                 (reset! (cursor state [:show-form]) true))}[:div [:i.fa.fa-plus] (t :badge/Addmoreinfoaboutevidence) ]]])
             (when (or  @(cursor state [:show-form])(not (every? #(blank? %)(vector @evidence-name-atom @evidence-narrative-atom))))

               [:div [:div.form-group
                      [:label.col-md-3 "Name"]
                      [:div.col-md-9
                       [evidence/input {:name "name" :atom evidence-name-atom :type "text"} nil]]]
                [:div.form-group
                 [:label.col-md-3 (t :page/Description)]
                 [:div.col-md-9
                  [evidence/input {:name "narrative" :rows 5 :cols 40 :atom evidence-narrative-atom } true]]]])

             ]
            [:div.add
             [:button {:type "button"
                       :class "btn btn-primary"
                       :disabled (not (url? @evidence-url-atom))
                       :on-click #(do
                                    (.preventDefault %)
                                    (if (= @input-mode :url_input) (reset! (cursor state [:evidence :properties :resource_type] ) "url"))
                                    (evidence/save-badge-evidence data state init-data)
                                    (when-not @(cursor state [:evidence :message])(swap! state assoc :tab [settings-tab-content data state init-data]
                                                                                         :tab-no 2)))}
              (t :core/Add)]]]])

        ;;url input
        (when (= @input-mode :url_input)
          [:div.preview.evidence-list.col-md-9
           [:div.panel.panel-default
            [:div.panel-heading
             [:div.panel-title
              [:div.url [:i.fa.fa-link]] [evidence/input {:name "evidence-url" :atom evidence-url-atom :type "url" :placeholder (t :badge/EnterevidenceURLstartingwith)}]]]
            [:div.panel-body.evidence-panel-body
             [:div [:div.form-group
                    [:label.col-md-3 "Name"]
                    [:div.col-md-9
                     [evidence/input {:name "name" :atom evidence-name-atom :type "text"} nil]]]
              [:div.form-group
               [:label.col-md-3 (t :page/Description)]
               [:div.col-md-9
                [evidence/input {:name "narrative" :rows 5 :cols 40 :atom evidence-narrative-atom } true]]]]]
            [:div.add
             [:button {:type "button"
                       :class "btn btn-primary"
                       :disabled (not (url? @evidence-url-atom))
                       :on-click #(do
                                    (.preventDefault %)
                                    (reset! (cursor state [:evidence :properties :resource_type] ) "url")
                                    (evidence/save-badge-evidence data state init-data)
                                    (when-not @(cursor state [:evidence :message])(swap! state assoc :tab [settings-tab-content data state init-data]
                                                                                         :tab-no 2)))}
              (t :core/Add)]]]]
          #_[:div.resource-container {:style {:margin-bottom "10px"}}
             [evidence/input {:name "evidence-url" :atom evidence-url-atom :type "url" :placeholder "http://" :preview? (cursor state [:show-preview])}]])

        ;;Resource grid
        (when-not @(cursor state [:show-preview])[evidence/resource-input data state init-data])

        ;;Buttons
        #_[:div.col-md-12
           [:hr]
           (when @(cursor state [:show-preview])[:button {:type "button"
                                                          :class "btn btn-primary"
                                                          :disabled (not (url? @evidence-url-atom))
                                                          :on-click #(do
                                                                       (.preventDefault %)
                                                                       (if (= @input-mode :url_input) (reset! (cursor state [:evidence :properties :resource_type] ) "url"))
                                                                       (evidence/save-badge-evidence data state init-data)
                                                                       (when-not @(cursor state [:evidence :message])(swap! state assoc :tab [settings-tab-content data state init-data]
                                                                                                                            :tab-no 2)))}
                                                 (t :core/Add)])
           [:a.cancel {:on-click #(do
                                    (.preventDefault %)
                                    (swap! state assoc :tab [settings-tab-content (dissoc data :evidence) state init-data]
                                           :tab-no 2))}
            (t :core/Cancel)]]]]]]))

(defn evidence-list [data state init-data]
  (let [evidence-name-atom (cursor state [:evidence :name])
        evidence-narrative-atom (cursor state [:evidence :narrative])
        visibility-atom (cursor state [:evidence :properties :hidden])]
    [:div
     (reduce (fn [r evidence]
               (let [{:keys [narrative description name id url mtime ctime properties]} evidence
                     {:keys [resource_id resource_type mime_type hidden]} properties
                     added-by-user? (and (not (blank? description)) (starts-with? description "Added by badge recipient"))
                     desc (cond
                            (not (blank? narrative)) narrative
                            (not added-by-user?) description ;;todo use regex to match description
                            :else nil)
                     visibility-class (if (= true hidden) " opaque" "")
                     show_evidence (if (pos? @(cursor state [:badge-settings :show_evidence])) true false)]
                 (if (and (blank? properties) (not added-by-user?))
                   (evidence/toggle-show-evidence! id data state init-data show_evidence))
                 (conj r
                       (when-not (blank? url)
                         [:div.panel.panel-default
                          [:div.panel-heading {:id (str "heading" id)
                                               :role "tab"}
                           [:div.panel-title {:class visibility-class}
                            (when-not added-by-user? [:span.label.label-success (t :badge/Verifiedevidence)])
                            [:div.url.row.flip [:div.col-md-1 [evidence/evidence-icon {:type resource_type :mime_type mime_type}]]
                             [:div.col-md.11.break (case resource_type
                                                     "file" (hyperlink url)
                                                     "page" (if (session/get :user)
                                                              [:a {:href "#"
                                                                   :on-click #(do
                                                                                (.preventDefault %)
                                                                                (mo/open-modal [:page :view] {:page-id resource_id}))} url]
                                                              (hyperlink url))
                                                     (hyperlink url))]]
                            (when-not (blank? name) [:div.inline [:label (t :badge/Name) ": "] name])
                            (when-not (blank? desc) [:div [:label (t :admin/Description) ": "]   desc])]

                           [:div [:div.evidence-status
                                  ;[:span.label.label-info
                                  (case hidden
                                    true (t :badge/Hidden)
                                    false (t :badge/Visibleinbadge)
                                    nil)];]
                            [:button {:type "button"
                                      :aria-label "OK"
                                      :class "close evidence-toggle"
                                      :on-click #(do
                                                   (.preventDefault %)
                                                   (evidence/init-evidence-form evidence state true)
                                                   (evidence/toggle-show-evidence! id data state init-data))} [:i.fa.show-more {:class (if (= true hidden) (str " fa-toggle-off") (str " fa-toggle-on"))
                                                                                                                                }]]]
                           (when added-by-user?
                             [:div [:div [:button {:type "button"
                                                   :aria-label "OK"
                                                   :class "close panel-edit"
                                                   :on-click #(do (.preventDefault %)
                                                                (evidence/init-evidence-form evidence state true))
                                                   :role "button"
                                                   :data-toggle "collapse"
                                                   :data-target (str "#collapse" id)
                                                   :data-parent "#accordion"
                                                   :href (str "#collapse" id)
                                                   :aria-expanded "true"
                                                   ;:aria-controls (str "collapse" id)
                                                   }
                                          [:i.fa.fa-edit.edit-evidence]]];;edit-button

                              [:div [:button {:type "button"
                                              :aria-label "OK"
                                              :class "close"
                                              :data-toggle "collapse"
                                              ;data-target (str "#collapse" id)
                                              :on-click #(do (.preventDefault %)
                                                           (evidence/delete-evidence! id data state init-data))
                                              :aria-expanded "false"
                                              :aria-controls (str "collapse" id)
                                              }
                                     [:i.fa.fa-trash.trash]]] ;;delete-button
                              ])]

                          [:div.panel-collapse {:id (str "collapse" id)
                                                :class "collapse"
                                                :role "tabpanel"
                                                :aria-labelledby (str "heading" id)}
                           [:div.panel-body.evidence-panel-body {:style {:padding-top "10px"}}
                            [:div.form-group
                             [:label.col-md-3 (t :badge/Name)]
                             [:div.col-md-9
                              [evidence/input {:name "name" :atom evidence-name-atom :type "text"} nil]]]
                            [:div.form-group
                             [:label.col-md-3 (t :page/Description)]
                             [:div.col-md-9
                              [evidence/input {:name "narrative" :rows 5 :cols 40 :atom evidence-narrative-atom } true]]]
                            ;[:hr]
                            [:div
                             [:button {:type         "button"
                                       :class        "btn btn-primary"
                                       :on-click     #(do (.preventDefault %)(evidence/save-badge-evidence data state init-data))
                                       :data-toggle "collapse"
                                       :data-target (str "#collapse" id)}
                              (t :badge/Save)]]
                            ]]])))
               )[:div {:id "accordion" :class "panel-group evidence-list" :role "tablist" :aria-multiselectable "true"}] @(cursor state [:badge-settings :evidences]))]))

(defn evidenceblock [data state init-data]
  (let [files-atom (cursor state [:files])
        pages-atom (cursor state [:pages])]
    [:div#badge-settings
     [:div.form-group
      [:div.col-md-9 {:class "new-evidence"}
       [:i.fa.fa-plus]
       [:a {:href "#"
            :on-click #(do (.preventDefault %)
                         (when (empty? @(cursor state [:files])) (evidence/init-resources :file_input files-atom))
                         (when (empty? @(cursor state [:pages]))(evidence/init-resources :page_input pages-atom))
                         (swap! state assoc :evidence nil
                                :show-preview false
                                :show-form false
                                :input_mode :url_input
                                :tab [evidence-form data state init-data]
                                :tab-no 2)
                         )} (t :badge/Addnewevidence)]]]
     (when-not (empty? @(cursor state [:badge-settings :evidences])) [:div.form-group
                                                                      [:div.col-md-12 [evidence-list data state init-data]]])]))

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
                                                  [:div.form-group
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
                                                                     (str (t :social/Followbadge))]]
                                                    [:div.col-md-12 (t :social/Badgenotificationsinfo)]]]

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

                                                   [:div {:class "row"}
                                                    [:label {:class "col-md-12 sub-heading" :for "evidence"}
                                                     (t :badge/Evidences)]]
                                                   #_(when-not (empty? @(cursor state [:badge-settings :evidences]))
                                                       [:div.form-group[:fieldset {:class "col-md-9 checkbox"}
                                                                        [:div.col-md-12 [:label {:for "show-evidence"}
                                                                                         [:input {:type      "checkbox"
                                                                                                  :id        "show-evidence"
                                                                                                  :on-change #(toggle-evidence state)
                                                                                                  :checked   (get-in @state [:badge-settings :show_evidence])}]
                                                                                         (t :badge/Evidencevisibility)]]]])
                                                   [:div [evidenceblock data state init-data]]

                                                   (into [:div]
                                                         (for [f (plugin-fun (session/get :plugins) "block" "badge_settings")]
                                                           [f id]))

                                                   ]]
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
     [:div (t :badge/Downloadbakedbadge)]
     ]]])
