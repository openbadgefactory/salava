(ns salava.badge.ui.evidence
  (:require [clojure.string :refer [blank? includes? split trim starts-with?]]
            [reagent.core :refer [cursor atom]]
            [salava.core.ui.modal :as mo]
            [reagent.session :as session]
            [salava.core.i18n :refer [t translate-text]]
            [salava.core.ui.helper :refer [plugin-fun path-for hyperlink base-url url?]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.time :refer [date-from-unix-time]]
            [salava.file.icons :refer [file-icon]]
            [salava.file.ui.my :refer [send-file]]))

(defn- settings-tab-content [data state init-data]
 (into [:div]
   (for [f (plugin-fun (session/get :plugins) "block" "settings_tab_content")]
     [f (dissoc data :evidence) state init-data])))

(defn toggle-input-mode [key state]
  (let [key-atom (cursor state [:input_mode])]
    (do
      (reset! key-atom nil)
      (swap! state assoc :input_mode key
             :show-preview false
             :show-form false
             :evidence nil))))

(defn init-settings [badge-id state init-data]
  (ajax/GET
    (path-for (str "/obpv1/badge/settings/" badge-id) true)
    {:handler (fn [data]
                (swap! state assoc :badge-settings data (assoc data :new-tag "")
                       :evidences (:evidences data)))}))

(defn delete-evidence! [id data state init-data]
  (ajax/DELETE
    (path-for (str "/obpv1/badge/evidence/" id))
    { :params {:user_badge_id (:id data)}
      :handler #(init-settings (:id @state) state init-data)}))

(defn init-resources [key resource-atom]
  (let [url (case key
              :page_input "/obpv1/page"
              :file_input "/obpv1/file"
              nil)]
    (ajax/GET
      (path-for url true)
      {:handler (fn [data]
                  (reset! resource-atom data))})))

(defn init-evidence-form [evidence state show-url?]
  (let [ {:keys [id name url narrative]} evidence
         evidence { :id (:id evidence)
                    :name (:name evidence)
                    :url (:url evidence)
                    :narrative (:narrative evidence)
                    :properties {:resource_type (get-in evidence [:properties :resource_type]) :hidden (get-in evidence [:properties :hidden])}}]
    (swap! state assoc :evidence evidence)))

(defn toggle-show-evidence!
  ([id data state init-data]
   (let [visibility-atom (cursor state [:evidence :properties :hidden])
         new-value (not @visibility-atom)
         badgeid (:id @state)]
     (ajax/POST
       (path-for (str "/obpv1/badge/toggle_evidence/" id))
       {:params {:show_evidence new-value
                 :user_badge_id (int badgeid)}
        :handler #(init-settings badgeid state init-data)})))
  ([id data state init-data show_evidence]
   (let [badgeid (:id @state)]
     (ajax/POST
       (path-for (str "/obpv1/badge/toggle_evidence/" id))
       {:params {:show_evidence show_evidence
                 :user_badge_id (int badgeid)}
        :handler #(init-settings badgeid state init-data)}))))

(defn set-page-visibility-to-private [page-id]
  (ajax/POST
    (path-for (str "/obpv1/page/toggle_visibility/" page-id))
    {:params {:visibility "public"}
     :handler (fn [data])}))

(defn save-badge-evidence [data state init-data]
  (let [{:keys [id name narrative url resource_visibility properties]} (:evidence @state)
        {:keys [resource_type mime_type resource_id]} properties
        badge-id (:id data)]
    (swap! state assoc :evidence {:message nil})
           ;:input_mode nil

    (if (and (not (blank? narrative)) (blank? name))
      (swap! state assoc :evidence {:message [:div.alert.alert-warning [:p (t :badge/Emptynamefield)]]
                                    :name name
                                    :narrative narrative
                                    :url url
                                    :properties {:mime_type mime_type
                                                 :resource_id resource_id
                                                 :resource_type (case @(cursor state [:input_mode])
                                                                  :page_input "page"
                                                                  :file_input "file"
                                                                  "url")}
                                    :resource_visibility resource_visibility})
      (do
        (if (= "private" resource_visibility) (set-page-visibility-to-private resource_id))
        (ajax/POST
          (path-for (str "/obpv1/badge/evidence/" badge-id))
          {:params {:evidence {:id id
                               :name (if name (trim name))
                               :narrative narrative
                               :url (if url (trim url))
                               :resource_id resource_id
                               :resource_type resource_type
                               :mime_type mime_type}}
           :handler #(init-settings (:id @state) state init-data)})))))

(defn input [input-data textarea?]
  (let [{:keys [name atom placeholder type error-message-atom rows cols preview?]} input-data]
    [(if textarea? :textarea :input)
     {:class       "form-control"
      :id          (str "input-" name)
      :name        name
      :type         type
      :placeholder placeholder
      :on-change   #(do
                      (reset! atom (.-target.value %))
                      (if error-message-atom(reset! error-message-atom (:message "")))
                      (when (and preview? (url? @atom )) (reset! preview? true)))
      :value       @atom
      :rows rows
      :cols cols}]))

(defn evidence-icon
  ([evidence_type]
   (let [{:keys [type mime_type]} evidence_type]
     [:div.evidence-icon
      (case type
        "page" [:i.fa.fa-file-text-o]
        "url" [:i.fa.fa-link]
        "file" [:i {:class (str "fa " (file-icon mime_type))}]
        [:i.fa.fa-link])]))
  ([url mime_type]
   (let [site-url (session/get :site-url)]
     (when-not (blank? url)
       [:div.evidence-icon
        (cond
          (and (starts-with? url site-url) (includes? url "/page/view/")) [:i.fa.fa-file-text-o]
          (and (starts-with? url site-url) (includes? url "/file/")) [:i {:class (str "fa " (file-icon @mime_type))}]
          :else [:i.fa.fa-link])]))))

(defn grid-element [element-data state key]
  (let [{:keys [id name path description visibility mtime badges ctime mime_type]} element-data
        value (case key
                :page_input (str (session/get :site-url) (path-for (str "/page/view/" id)))
                :file_input (str (session/get :site-url) "/" path)
                nil)]
    [:div
     [:a.resource-link {:href "#"
                        :on-click #(do
                                     (.preventDefault %)
                                     (swap! state assoc :show-form false
                                            :show-preview true
                                            :evidence {:url value
                                                       :name name
                                                       :narrative description
                                                       :properties {:mime_type mime_type
                                                                    :resource_id id
                                                                    :resource_type (case key
                                                                                     :page_input "page"
                                                                                     :file_input "file")}
                                                       :resource_visibility visibility}))}
      (case @(cursor state [:input_mode])
        :page_input [:div.resource  [:i.fa.fa-file-text-o] name]
        :file_input [:div.resource [:i {:style {:margin-right "10px"}
                                        :class (str "file-icon-large fa " (file-icon mime_type))}]
                     name])]]))

(defn upload-status [status title message]
  [:div
   [:h4.modal-title (translate-text title)]
   [:div (if status
           [:div {:class (str "alert " (if (= status "error")
                                         "alert-warning"
                                         "alert-success"))}
            (translate-text message)]
           [:div
            [:i {:class "fa fa-cog fa-spin fa-2x"}]
            [:span " " (translate-text message)]])]])

(defn upload-file [files-atom state]
  (let [files (:files @files-atom)
        file (-> (.querySelector js/document "input")
                 .-files
                 (.item 0))
        form-data (doto
                    (js/FormData.)
                    (.append "file" file (.-name file)))]
    (ajax/POST
      (path-for "/obpv1/file/upload")
      {:body    form-data
       :response-format :json
       :keywords?       true
       :handler (fn [data]
                  (swap! state assoc :evidence {:message [upload-status (:status data) (:message data) (:reason data)]})
                  (if (= (:status data) "success")
                    (do
                      (swap! state assoc :show-form false
                             :show-preview true
                             :evidence {:url (str (session/get :site-url) "/" (get-in data [:data :path]))
                                        :name (get-in data [:data :name])
                                        :narrative nil
                                        :properties {:mime_type (get-in data [:data :mime_type])
                                                     :resource_id (get-in data [:data :id])
                                                     :resource_type "file"}
                                        :message [upload-status (:status data) (:message data) (:reason data)]})
                      (swap! files-atom assoc :files (conj files (:data data))))
                    (swap! state assoc :evidence {:message [upload-status (:status data) (:message data) (:reason data)]})))
       :error-handler (fn [{:keys [status status-text]}]
                        (swap! state assoc :evidence {:message [upload-status "error" (t :file/Errorwhileuploading) (t :file/Filetoobig)]}))})))

(defn files-grid [state]
  (let [files (:files @(cursor state [:files]))]
    [:div.col-md-12.resource-container
     (reduce (fn [r resource]
               (conj r [grid-element resource state :file_input]))
             [:div [:label
                    [:a [:span [:i.fa.fa-upload] (t :file/Upload)]]
                    [:input {:id "grid-file-upload"
                             :type "file"
                             :name "file"
                             :on-change #(upload-file (cursor state [:files]) state)
                             :style {:display "none"}}]]
              (if (seq files) [:div [:label {:style {:margin "5px" :margin-bottom "10px"}} (t :badge/Orchoosefile)]])]
             files)]))

(defn pages-grid [state]
  (let [pages @(cursor state [:pages])]
    [:div.col-md-12.resource-container
     (reduce (fn [r resource]
               (conj r [grid-element resource state :page_input]))
             [:div
              (if (seq pages) [:div [:label {:style {:margin-bottom "10px"}} (t :badge/Clickpagebelow)]])]
             pages)]))

(defn resource-input [data state init-data]
  (let [resource-input-mode (cursor state [:input_mode])
        evidence-url-atom (cursor state [:evidence :url])
        show-url? (cursor state [:evidence :show_url])]
    (fn []
      [:div.form-group
       [:div.col-md-9
        (case @resource-input-mode
          :file_input [files-grid state]
          :page_input [pages-grid state]
          nil)]])))

(defn evidence-form [data state init-data]
  (let [evidence-name-atom (cursor state [:evidence :name])
        evidence-narrative-atom (cursor state [:evidence :narrative])
        evidence-url-atom (cursor state [:evidence :url])
        message (cursor state [:evidence :message])
        input-mode (cursor state [:input_mode])
        {:keys [image_file name]} data]

    [:div {:id "badge-settings" :class "row flip"}
     [:div {:class "col-md-3 badge-image modal-left"}
      [:img {:src (str "/" image_file) :alt name}]]

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
                                   (toggle-input-mode :url_input state))}
                  [:i.fa.fa-link] (t :admin/Url)]]
           [:div  [:a {:class (if (= :page_input @input-mode) "active-resource" "")
                       :href "#"
                       :on-click #(do
                                    (.preventDefault %)
                                    (toggle-input-mode :page_input state))}

                   [:i.fa.fa-file-text] (t :page/Pages)]]
           [:div  [:a {:class (if (= :file_input @input-mode) "active-resource" "")
                       :href "#":on-click #(do
                                             (.preventDefault %)
                                             (toggle-input-mode :file_input state))}
                      [:i.fa.fa-files-o] (t :page/Files)]]
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
              [:div.url  [evidence-icon @evidence-url-atom (cursor state [:evidence :properties :mime_type])]] (hyperlink @evidence-url-atom)]
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
                                                 (reset! (cursor state [:show-form]) true))}[:div [:i.fa.fa-plus] (t :badge/Addmoreinfoaboutevidence)]]])
             (when (or  @(cursor state [:show-form])(not (every? #(blank? %)(vector @evidence-name-atom @evidence-narrative-atom))))

               [:div [:div.form-group
                      [:label.col-md-3 "Name"]
                      [:div.col-md-9
                       [input {:name "name" :atom evidence-name-atom :type "text"} nil]]]
                [:div.form-group
                 [:label.col-md-3 (t :page/Description)]
                 [:div.col-md-9
                  [input {:name "narrative" :rows 5 :cols 40 :atom evidence-narrative-atom } true]]]])]


            [:div.add
             [:button {:type "button"
                       :class "btn btn-primary"
                       :disabled (not (url? @evidence-url-atom))
                       :on-click #(do
                                    (.preventDefault %)
                                    (if (= @input-mode :url_input) (reset! (cursor state [:evidence :properties :resource_type] ) "url"))
                                    (save-badge-evidence data state init-data)
                                    (when-not @(cursor state [:evidence :message])(swap! state assoc :tab [settings-tab-content data state init-data]
                                                                                         :tab-no 2)))}
              (t :core/Add)]]]])

        ;;url input
        (when (= @input-mode :url_input)
          [:div.preview.evidence-list.col-md-9
           [:div.panel.panel-default
            [:div.panel-heading
             [:div.panel-title
              [:div.url [:i.fa.fa-link]] [input {:name "evidence-url" :atom evidence-url-atom :type "url" :placeholder (t :badge/EnterevidenceURLstartingwith)}]]]
            [:div.panel-body.evidence-panel-body
             [:div [:div.form-group
                    [:label.col-md-3 "Name"]
                    [:div.col-md-9
                     [input {:name "name" :atom evidence-name-atom :type "text"} nil]]]
              [:div.form-group
               [:label.col-md-3 (t :page/Description)]
               [:div.col-md-9
                [input {:name "narrative" :rows 5 :cols 40 :atom evidence-narrative-atom } true]]]]]
            [:div.add
             [:button {:type "button"
                       :class "btn btn-primary"
                       :disabled (not (url? @evidence-url-atom))
                       :on-click #(do
                                    (.preventDefault %)
                                    (reset! (cursor state [:evidence :properties :resource_type] ) "url")
                                    (save-badge-evidence data state init-data)
                                    (when-not @(cursor state [:evidence :message])(swap! state assoc :tab [settings-tab-content data state init-data]
                                                                                         :tab-no 2)))}
              (t :core/Add)]]]])
        ;;Resource grid
        (when-not @(cursor state [:show-preview])[resource-input data state init-data])]]]]))

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
                   (toggle-show-evidence! id data state init-data show_evidence))
                 (conj r
                       (when (and (not (blank? url)) (url? url))
                         [:div.panel.panel-default
                          [:div.panel-heading {:id (str "heading" id)
                                               :role "tab"}
                           [:div.panel-title {:class visibility-class}
                            (when-not added-by-user? [:span.label.label-success (t :badge/Verifiedevidence)])
                            [:div.url.row.flip [:div.col-md-1 [evidence-icon {:type resource_type :mime_type mime_type}]]
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
                                                   (init-evidence-form evidence state true)
                                                   (toggle-show-evidence! id data state init-data))} [:i.fa.show-more {:class (if (= true hidden) (str " fa-toggle-off") (str " fa-toggle-on"))}]]]

                           (when added-by-user?
                             [:div [:div [:button {:type "button"
                                                   :aria-label "OK"
                                                   :class "close panel-edit"
                                                   :on-click #(do (.preventDefault %)
                                                                (init-evidence-form evidence state true))
                                                   :role "button"
                                                   :data-toggle "collapse"
                                                   :data-target (str "#collapse" id)
                                                   :data-parent "#accordion"
                                                   :href (str "#collapse" id)
                                                   :aria-expanded "true"}
                                                   ;:aria-controls (str "collapse" id)

                                          [:i.fa.fa-edit.edit-evidence]]];;edit-button

                              [:div [:button {:type "button"
                                              :aria-label "OK"
                                              :class "close"
                                              :data-toggle "collapse"
                                              ;data-target (str "#collapse" id)
                                              :on-click #(do (.preventDefault %)
                                                           (delete-evidence! id data state init-data))
                                              :aria-expanded "false"
                                              :aria-controls (str "collapse" id)}

                                     [:i.fa.fa-trash.trash]]]])] ;;delete-button


                          [:div.panel-collapse {:id (str "collapse" id)
                                                :class "collapse"
                                                :role "tabpanel"
                                                :aria-labelledby (str "heading" id)}
                           [:div.panel-body.evidence-panel-body {:style {:padding-top "10px"}}
                            [:div.form-group
                             [:label.col-md-3 (t :badge/Name)]
                             [:div.col-md-9
                              [input {:name "name" :atom evidence-name-atom :type "text"} nil]]]
                            [:div.form-group
                             [:label.col-md-3 (t :page/Description)]
                             [:div.col-md-9
                              [input {:name "narrative" :rows 5 :cols 40 :atom evidence-narrative-atom } true]]]
                            ;[:hr]
                            [:div
                             [:button {:type         "button"
                                       :class        "btn btn-primary"
                                       :on-click     #(do (.preventDefault %)(save-badge-evidence data state init-data))
                                       :data-toggle "collapse"
                                       :data-target (str "#collapse" id)}
                              (t :badge/Save)]]]]]))))

             [:div {:id "accordion" :class "panel-group evidence-list" :role "tablist" :aria-multiselectable "true"}] @(cursor state [:badge-settings :evidences]))]))

(defn evidenceblock [data state init-data]
  (let [files-atom (cursor state [:files])
        pages-atom (cursor state [:pages])]
    [:div#badge-settings
     [:div.form-group
      [:div.col-md-9 {:class "new-evidence"}
       [:i.fa.fa-plus]
       [:a {:href "#"
            :on-click #(do (.preventDefault %)
                         (when (empty? @(cursor state [:files])) (init-resources :file_input files-atom))
                         (when (empty? @(cursor state [:pages]))(init-resources :page_input pages-atom))
                         (swap! state assoc :evidence nil
                                :show-preview false
                                :show-form false
                                :input_mode :url_input
                                :tab [evidence-form data state init-data]
                                :tab-no 2))}
           (t :badge/Addnewevidence)]]]
     (when-not (empty? @(cursor state [:badge-settings :evidences])) [:div.form-group
                                                                      [:div.col-md-12 [evidence-list data state init-data]]])]))

(defn evidence-list-badge-view [evidences]
 (if (seq evidences)
  [:div.row {:id "badge-settings"}
   [:div.col-md-12
    [:h2.uppercase-header (t :badge/Evidences) #_(if (= (count  evidences) 1)  (t :badge/Evidence) (str (t :badge/Evidence) " (" (count evidences) ")"))]
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
                           (when-not added-by-user? [:span.label.label-success (t :badge/Verifiedevidence)])
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

            [:div] evidences)]]
  [:div ""]))
