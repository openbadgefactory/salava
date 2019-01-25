(ns salava.badge.ui.evidence
  (:require [salava.core.ui.modal :as mo]
            [reagent.core :refer [cursor atom]]
            [salava.core.i18n :refer [t translate-text]]
            [reagent.session :as session]
            [salava.core.ui.helper :refer [plugin-fun path-for hyperlink base-url]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.time :refer [date-from-unix-time]]
            [clojure.string :refer [blank? includes? split trim starts-with?]]
            [salava.file.icons :refer [file-icon]]
            [salava.file.ui.my :refer [send-file]]))

(defn url? [s]
  (when-not (blank? s)
    (not (blank? (re-find #"^http" (str (trim s)))))))


(defn toggle-input-mode [key state]
  (let [key-atom (cursor state [:input_mode])]
    (swap! state assoc :input_mode key
           :show-preview false
           :show-form false
           :evidence nil)))

(defn delete-evidence! [id data state init-data]
  (let [init-settings (first (plugin-fun (session/get :plugins) "modal" "show_settings_dialog"))]
    (ajax/DELETE
      (path-for (str "/obpv1/badge/evidence/" id))
      { :params {:user_badge_id (:id data)}
        :handler (fn [r]
                   (when (= "success" (:status r))
                     (init-settings (:id data) state init-data "settings")))})))


(defn init-resources [key resource-atom]
  (let [url (case key
              :page_input "/obpv1/page"
              :file_input "/obpv1/file")]
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
                    :properties {:hidden (get-in evidence [:properties :hidden])}}]
    (swap! state assoc :evidence evidence)))

(defn toggle-show-evidence! [id data state init-data]
  (let [visibility-atom (cursor state [:evidence :properties :hidden])
        new-value (not @visibility-atom)
        badgeid (:id data)
        init-settings (first (plugin-fun (session/get :plugins) "modal" "show_settings_dialog"))]
    (ajax/POST
      (path-for (str "/obpv1/badge/toggle_evidence/" id))
      {:params {:show_evidence new-value
                :user_badge_id badgeid}
       #_:handler #_(fn [data]
                  (prn data)
                  #_(when (= (:status data) "success")
                    (init-settings (:id data) state init-data "settings"))
                  )})))

(defn set-page-visibility-to-private [page-id]
  (ajax/POST
    (path-for (str "/obpv1/page/toggle_visibility/" page-id))
    {:params {:visibility "public"}
     :handler (fn [data])}))

(defn save-badge-evidence [data state init-data]
  (let [{:keys [id name narrative url resource_visibility resource_id resource_type mime_type]} (:evidence @state)
        badge-id (:id data)
        init-settings (first (plugin-fun (session/get :plugins) "modal" "show_settings_dialog"))]
    (swap! state assoc :evidence {:message nil})
    (if (and (not (blank? narrative)) (blank? name))
      (swap! state assoc :evidence {:message [:div.alert.alert-warning [:p (t :badge/Emptynamefield)]]
                                    :name name
                                    :narrative narrative
                                    :url url})
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
           :handler (fn [resp]
                      (when (= "success" (:status resp))
                        (init-settings (:id data) state init-data "settings")))})))))

;;;;

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
        "file" [:i {:class (str "fa " (file-icon mime_type)) } ]
        [:i.fa.fa-link])]))
  ([url mime_type]
   (let [site-url (session/get :site-url)]
     (when-not (blank? url)
       [:div.evidence-icon
        (cond
          (and (starts-with? url site-url) (includes? url "/page/view/")) [:i.fa.fa-file-text-o]
          (and (starts-with? url site-url) (includes? url "/file/")) [:i {:class (str "fa " (file-icon @mime_type)) } ]
          :else [:i.fa.fa-link])]
       ))))

(defn grid-element [element-data state key data init-data]
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
                                                       :mime_type mime_type
                                                       :resource_id id
                                                       :resource_type (case key
                                                                        :page_input "page"
                                                                        :file_input "file")
                                                       :resource_visibility visibility})) }
      (case key
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
                  (if (= (:status data) "success")
                    (swap! files-atom assoc :files (conj files (:data data))))
                  (swap! state assoc :evidence {:message [upload-status (:status data) (:message data) (:reason data)]})
                  )


       :error-handler (fn [{:keys [status status-text]}]
                        (swap! state assoc :evidence {:message [upload-status "error" (t :file/Errorwhileuploading) (t :file/Filetoobig)]})
                        )})))


(defn resources-grid [key resource-atom state data init-data]
  (let [evidence-url-atom (cursor state [:evidence :url])
        {:keys[files]} @resource-atom
        init-container (case key
                         :file_input [:div [:label
                                            [:span [:i.fa.fa-upload] (t :file/Upload) ]
                                            [:input {:id "grid-file-upload"
                                                     :type "file"
                                                     :name "file"
                                                     :on-change #(upload-file resource-atom state)
                                                     :style {:display "none"}}]]]
                         :page_input [:div])]
    [:div.col-md-9.resource-container
     (reduce (fn [r resource]
               (conj r [grid-element resource state key data init-data])
               ) init-container (if files files @resource-atom))]))

(defn resource-input [data state init-data]
  (let [resource-input-mode (cursor state [:input_mode])
        evidence-url-atom (cursor state [:evidence :url])
        resource-atom (atom {})
        show-url? (cursor state [:evidence :show_url])]
    (fn []
      [:div.form-group
       [:div.col-md-9
        (case @resource-input-mode
          :file_input (do
                        (init-resources :file_input resource-atom)
                        [:div [resources-grid :file_input resource-atom state data init-data]])
          :page_input (do
                        (init-resources :page_input resource-atom)
                        [:div [resources-grid :page_input resource-atom state data init-data]])
          :url_input [:div.resource-container {:style {:margin-bottom "10px"}}
                      [input {:name "evidence-url" :atom evidence-url-atom :type "url" :placeholder "http://" :preview? (cursor state [:show-preview])}]]
          nil)]])))

(defn evidence-form [data state init-data]
  (let [settings-tab (first (plugin-fun (session/get :plugins) "settings" "settings_tab_content"))
        evidence-name-atom (cursor state [:evidence :name])
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
       [:div.form-horizontal
        [:div.form-group
         [:label.col-md-9.sub-heading (t :badge/Addnewevidence)]
         [:div.col-md-9.evidence-info (t :badge/Evidenceinfo)]
         [:div.col-md-9.resource-options
          [:div ;{:style {:float "left"}}
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
                                    (toggle-input-mode :page_input state)) }
                   [:i.fa.fa-file-text] (t :page/Mypages)]]
           [:div  [:a {:class (if (= :file_input @input-mode) "active-resource" "")
                       :href "#":on-click #(do
                                             (.preventDefault %)
                                             (toggle-input-mode :file_input state))
                       } [:i.fa.fa-files-o] (t :page/Files)]]]]]

        ;;Preview
        (when @(cursor state [:show-preview])
          [:div.preview.evidence-list
           [:div.panel.panel-default
            [:div.panel-heading
             [:div.panel-title
              [:div.url [:div {:style {:float "left"}} [evidence-icon @evidence-url-atom (cursor state [:evidence :mime_type])]] (hyperlink @evidence-url-atom)]]
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
                       [input {:name "name" :atom evidence-name-atom :type "text"} nil]]]
                [:div.form-group
                 [:label.col-md-3 (t :page/Description)]
                 [:div.col-md-9
                  [input {:name "narrative" :rows 5 :cols 40 :atom evidence-narrative-atom } true]]]])

             ]]])

        ;;Resource grid
        (when-not @(cursor state [:show-preview])[resource-input data state init-data])

        ;;Buttons
        [:div
         [:hr]
         [:button {:type "button"
                   :class "btn btn-primary"
                   :disabled (not (url? @evidence-url-atom))
                   :on-click #(do

                                (.preventDefault %)
                                (if (= @input-mode :url_input) (reset! (cursor state [:evidence :resource_type] ) "url"))
                                (save-badge-evidence data state init-data)

                                ;(reset! (cursor state [:show-form]) false)
                                ;(reset! (cursor state [:show-preview]) false)

                                (reset! input-mode nil)
                                )}
          (t :core/Add)]
         [:a.cancel {:on-click #(do (.preventDefault %) (swap! state assoc :tab [settings-tab (dissoc data :evidence) state init-data]
                                                               :tab-no 2))}
          (t :core/Cancel)]]]]]]))

(defn evidence-list [data state init-data]
  (let [evidence-name-atom (cursor state [:evidence :name])
        evidence-narrative-atom (cursor state [:evidence :narrative])
        visibility-atom (cursor state [:evidence :properties :hidden])]
    (reduce (fn [r evidence]
              (let [{:keys [narrative description name id url mtime ctime evidence_type properties]} evidence
                    {:keys [resource_id resource_type mime_type hidden]} properties
                    added-by-user? (and (not (blank? description)) (starts-with? description "Added by badge recipient"))
                    desc (cond
                           (not (blank? narrative)) narrative
                           (not added-by-user?) description ;;todo use regex to match description
                           :else nil)]
                (prn @visibility-atom)
                (init-evidence-form evidence state true)
                (conj r
                      (when-not (blank? url)
                        [:div.panel.panel-default
                         [:div.panel-heading {:id (str "heading" id)
                                              :role "tab"}
                          [:div.panel-title
                           [:div.url [:div {:style {:float "left"}} [evidence-icon evidence_type]] (hyperlink url)]
                           (when-not (blank? name) [:div.inline [:label (t :badge/Name) ": "] name])
                           (when-not (blank? desc) [:div [:label (t :admin/Description) ": "]   desc])]

                          [:div [:button {:type "button"
                                          :aria-label "OK"
                                          :class "close evidence-toggle"
                                          :on-click #(do
                                                       (.preventDefault %)
                                                       (toggle-show-evidence! id data state init-data))} [:i.fa.show-more {:class (if @visibility-atom (str " fa-toggle-off") (str " fa-toggle-on"))
                                                                                            :title (if @visibility-atom (t :badge/Showevidence) (t :badge/Showevidence))}]]]
                          (when added-by-user?
                            [:div [:div [:button {:type "button"
                                                  :aria-label "OK"
                                                  :class "close panel-edit"
                                                  :on-click #(do (.preventDefault %)
                                                               (init-evidence-form evidence state true))
                                                  :role "button" :data-toggle "collapse"
                                                  :data-parent "#accordion"
                                                  :href (str "#collapse" id)
                                                  :aria-expanded "true"
                                                  :aria-controls (str "collapse" id)}
                                         [:i.fa.fa-edit.edit-evidence]]];;edit-button

                             [:div [:button {:type "button"
                                             :aria-label "OK"
                                             :class "close"
                                             :on-click #(do (.preventDefault %)(delete-evidence! id data state init-data))}
                                    [:i.fa.fa-trash.trash]]] ;;delete-button
                             ])
                          ]

                         [:div {:id (str "collapse" id)
                                :class "panel-collapse collapse"
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
                           [:hr]
                           [:div
                            [:button {:type         "button"
                                      :class        "btn btn-primary"
                                      :on-click     #(do (.preventDefault %)(save-badge-evidence data state init-data))
                                      :data-toggle "collapse"
                                      :data-target (str "#collapse" id)}
                             (t :badge/Save)]]
                           ]]])))
              )[:div {:id "accordion" :class "panel-group evidence-list" :role "tablist" :aria-multiselectable "true"}] (:evidences data))))

(defn evidence-block [data state init-data]
  [:div#badge-settings
   [:div.form-group
    [:div.col-md-9 {:class "new-evidence"}
     [:i.fa.fa-plus]
     [:a {:on-click #(do (.preventDefault %)
                       (swap! state assoc :evidence nil
                              :show-preview false
                              :show-form false
                              :input_mode :url_input)
                       (swap! state assoc :tab [evidence-form data state init-data] :tab-no 2))} (t :badge/Addnewevidence)]]]
   (when-not (empty? (:evidences data)) [:div.form-group
                                         [:div.col-md-12 [evidence-list data state init-data]]])])

