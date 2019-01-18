(ns salava.badge.ui.evidence
  (:require [salava.core.ui.modal :as mo]
            [reagent.core :refer [cursor atom]]
            [salava.core.i18n :refer [t translate-text]]
            [reagent.session :as session]
            [salava.core.ui.helper :refer [plugin-fun path-for hyperlink base-url]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.time :refer [date-from-unix-time]]
            [clojure.string :refer [blank? includes? split trim]]
            [salava.file.icons :refer [file-icon]]
            [salava.file.ui.my :refer [send-file]]))

(defn url? [s]
  (when-not (blank? s)
    (not (blank? (re-find #"^http" (str (trim s)))))))

(defn input [input-data textarea?]
  (let [{:keys [name atom placeholder type error-message-atom rows cols]} input-data]
    [(if textarea? :textarea :input)
     {:class       "form-control"
      :id          (str "input-" name)
      :name        name
      :type         type
      :placeholder placeholder
      :on-change   #(do
                      (reset! atom (.-target.value %))
                      (if error-message-atom(reset! error-message-atom (:message ""))))
      :value       @atom
      :rows rows
      :cols cols}]))



(defn toggle-input-mode [key state]
  (let [key-atom (cursor state [:input_mode])]
    (if (= key @key-atom) (reset! key-atom nil) (do (reset! key-atom nil) (reset! key-atom key)))))


(defn init-resources [key resource-atom]
  (let [url (case key
              :page_input "/obpv1/page"
              :file_input "/obpv1/file")]
    (ajax/GET
      (path-for url true)
      {:handler (fn [data]
                  (reset! resource-atom data)
                  )})))


(defn remove-url [url coll state]
  (swap! state assoc-in [:evidence :urls] (remove #(= url %) @coll)))

(defn grid-element [element-data state key]
  (let [{:keys [id name path visibility mtime badges ctime mime_type]} element-data
        value (case key
                :page_input (str (session/get :site-url) (path-for (str "/page/view/" id)))
                :file_input (str (session/get :site-url) "/" path)
                nil)
        evidence-url-atom (cursor state [:evidence :url])
        show-url? (cursor state [:evidence :show_url])
        resource-input-mode (cursor state [:input_mode])
        update-page-visibility ()]
    (prn visibility)
   (when (= "private" visibility) (swap! state assoc :evidence {:update-page-visibility id}))
    [:div.radio
     [:label
      [:input { :name "opt"
                :type "radio"
                :value value
                :on-change #(do
                              (reset! evidence-url-atom (.-target.value %))
                              (reset! resource-input-mode nil)
                              (reset! show-url? true))}]
      (case key
        :page_input [:a {:href "#" :on-click #(do
                                                (.preventDefault %)
                                                (mo/open-modal [:page :view] {:page-id id}))} name]

        :file_input [:div [:i {:style {:margin-right "10px"}
                               :class (str "file-icon-large fa " (file-icon mime_type))}]
                     [:a {:href (str "/" path) :target "_blank"} name]])]]))




(defn toggle-visible-area [visible-area-atom key]
  (if (= key @visible-area-atom) (reset! visible-area-atom nil) (reset! visible-area-atom key)))

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
            [:span " " (translate-text message)]])]
   ])

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


(defn resources-grid [key resource-atom state]
  (let [evidence-url-atom (cursor state [:evidence :url])
        {:keys[files]} @resource-atom]
    [:div.resource-upload
     (if files
       [:div [:label
                       [:span [:i.fa.fa-upload] (t :file/Upload) ]
                       [:input {:id "grid-file-upload"
                                :type "file"
                                :name "file"
                                :on-change #(upload-file resource-atom state)
                                :style {:display "none"}}]
                       ]])
     [:div.col-md-9.resource-container
      (reduce (fn [r resource]
                (conj r [grid-element resource state key])
                ) [:form] (if files files @resource-atom))
      ]]))


(defn init-evidence-form [evidence state show-url?]
  (let [ {:keys [id name url audience narrative genre description]} evidence
         evidence { :id (:id evidence)
                   :name (:name evidence)
                   :url (:url evidence)
                   :audience (:audience evidence)
                   :narrative (:narrative evidence)
                   :genre (:genre evidence)
                   :description (:description evidence)
                   }]
    (if (every? #(blank? %) (vector name audience narrative genre description))
      (swap! state assoc :show_form nil)
      (swap! state assoc :show_form true))
    (if show-url?
      (swap! state assoc :evidence (assoc evidence :show_url true :input_mode nil) )
      (swap! state assoc :evidence evidence))))

(defn save-badge-evidence [data state init-data]
  (let [{:keys [id name description url update-page-visibility]} (:evidence @state)
        badge-id (:id data)
        init-settings (first (plugin-fun (session/get :plugins) "modal" "show_settings_dialog"))]
    (swap! state assoc :evidence {:message nil})
    (if (blank? url)
      (swap! state assoc :evidence {:message [:div.alert.alert-warning [:p "Resource field can't be empty!"]]})

      (ajax/POST
        (path-for (str "/obpv1/badge/evidence/" badge-id))
        {:params {:id id
                  :name (if name (trim name))
                  :description description
                  :url (if url (trim url))}
         :handler (fn [resp]
                    (when (= "success" (:status resp))
                      (init-settings (:id data) state init-data "settings"))


                    )}))))

(defn selected-url [state init-data]
  (let [evidence-url-atom (cursor state [:evidence :url])
        show-url? (cursor state [:evidence :show_url])
        evidence (cursor state [:evidence])]
    (fn []
      (when (and (not (empty? @evidence-url-atom )) @show-url?)
        [:div.col-md-9


         [:button.close {:type "button"
                         :aria-label "OK"
                         :on-click #(do
                                      (.preventDefault %)
                                      (reset! show-url? false)
                                      (reset! evidence (assoc @evidence :url nil :show_url false
                                                         ))
                                      (init-evidence-form @evidence state false)
                                      )
                         }
          [:span {:aria-hidden "true"
                  :dangerouslySetInnerHTML {:__html "&times;"}}]]
         [:div.selected-url (hyperlink (trim @evidence-url-atom))]]
        ))))

(defn resource-input [state]
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
                        [:div [resources-grid :file_input resource-atom state]])
          :page_input (do
                        (init-resources :page_input resource-atom)
                        [:div [resources-grid :page_input resource-atom state]])
          :url_input [:div {:style {:margin-bottom "10px"}}
                      [input {:name "evidence-url" :atom evidence-url-atom :type "url"}]
                      [:button {:type "button"
                                :class "btn btn-primary"
                                :disabled (not (url? @evidence-url-atom))
                                :on-click #(do
                                             (.preventDefault %)
                                             (reset! show-url? true)
                                             (reset! resource-input-mode nil))}
                       (t :core/Add)]]
          nil)]])))


(defn evidence-form [data state init-data]
  (let [settings-tab (first (plugin-fun (session/get :plugins) "settings" "settings_tab_content"))
        evidence-name-atom (cursor state [:evidence :name])
        evidence-description-atom (cursor state [:evidence :description])
        evidence-url-atom (cursor state [:evidence :url])
        page-input-enabled? (cursor state [:page_input])
        message (cursor state [:evidence :message])
        show-url? (cursor state [:evidence :show_url])
        file-input-enabled? (cursor state [:file_input])
        resource-input-mode (cursor state [:input_mode])
        show-form (cursor state [:show_form])
        {:keys [image_file name]} data]

    [:div {:id "badge-settings" :class "row flip"}
     [:div {:class "col-md-3 badge-image modal-left"}
      [:img {:src (str "/" image_file) :alt name}]]
     [:div {:class "col-md-9 settings-content settings-tab"}
      (if @message [:div @message])
      [:div
       [:div.form-horizontal
        [:div.form-group
         [:label.col-md-9.sub-heading "Add evidence resource"]
         [:div.col-md-9 {:style {:margin-top "10px" :margin-bottom "10px"}}
          [:div {:style {:float "left"}}
           [:div {:style {:float "left" :margin-right "10px"}}[:a {:style {:margin "10px"} :href "#"  :on-click #(toggle-input-mode :url_input state)} [:i.fa.fa-link] (t :admin/Url)]]
           [:div {:style {:float "left" :margin-right "10px"}} [:a {:href "#" :on-click #(do (toggle-input-mode :page_input state)
                                                                                           #_(swap! state assoc :tab [resource-tab data state init-data])
                                                                                           ) } [:i.fa.fa-file] (t :page/Mypages)]]
           [:div {:style {:float "left"}} [:a {:href "#"
                                               :on-click #(do
                                                            (.preventDefault %)
                                                            (toggle-input-mode :file_input state))
                                               } [:i.fa.fa-file] (t :page/Files)]]]]]

         [selected-url state init-data]
         [resource-input state]

        (if @show-form
          [:div
           [:div.form-group
            [:label.col-md-3 "Name"]
            [:div.col-md-9
             [input {:name "name" :atom evidence-name-atom :type "text"} nil]]]
           [:div.form-group
            [:label.col-md-3 (t :page/Description)]
            [:div.col-md-9
             [input {:name "Description" :rows 5 :cols 40 :atom evidence-description-atom } true]]]]

          [:div [:i.fa.fa-plus]
           [:a {:href "#" :on-click #(do (.preventDefault %) (reset! show-form true))} "Add more info about this evidence"]])
         [:hr]]
        [:div
         [:button {:type         "button"
                   :class        "btn btn-primary"
                   :on-click     #(save-badge-evidence data state init-data)}
          (t :badge/Save)]
         [:button {:type "button"
                   :class "btn btn-primary"
                   :on-click #(do (.preventDefault %) (swap! state assoc :tab [settings-tab (dissoc data :evidence) state init-data] :tab-no 2))}
          (t :core/Cancel)]]]]]))

(defn delete-evidence! [id data state init-data]
  (let [init-settings (first (plugin-fun (session/get :plugins) "modal" "show_settings_dialog"))]
    (ajax/DELETE
      (path-for (str "/obpv1/badge/evidence/" id))
      { :params {:user_badge_id (:id data)}
        :handler (fn [r]
                   (when (= "success" (:status r))
                     (init-settings (:id data) state init-data "settings")))})))

(defn evidence-list [data state init-data]
  (reduce (fn [r evidence]
            (let [{:keys [genre description narrative name audience id url mtime ctime]} evidence
                  text ()]
              (conj r
                    [:div.panel.panel-default
                     [:div.panel-heading {:id (str "heading" id)
                                          :role "tab"}
                      [:div.panel-title
                       [:a {:href "#" :on-click #(do (.preventDefault %)
                                                   (init-evidence-form evidence state true)
                                                   (swap! state assoc :tab [evidence-form data state init-data] :input_mode nil))}
                        [:div.url [:div {:style {:float "left"}} [:i.fa.fa-link]]  url]]
                       (when-not (every? #(blank? %) (vector genre description narrative name audience))
                         [:div.show-more [:a {:role "button" :data-toggle "collapse"
                                              :data-parent "#accordion"
                                              :href (str "#collapse" id)
                                              :aria-expanded "true"
                                              :aria-controls (str "collapse" id)} (t :admin/Showmore)]])]
                      [:div [:button {:type "button"
                                      :aria-label "OK"
                                      :class "close panel-close"
                                      :on-click #(delete-evidence! id data state init-data)}
                             [:span {:aria-hidden "true"
                                     :dangerouslySetInnerHTML {:__html "&times;"}}]]]
                      ]
                     (when-not (every? #(blank? %) (vector genre description narrative name audience))
                       [:div {:id (str "collapse" id)
                              :class "panel-collapse collapse"
                              :role "tabpanel"
                              :aria-labelledby (str "heading" id)}
                        [:div.panel-body.evidence-panel-body {:style {:padding-top "10px"}}
                         (when-not (blank? name) [:div.inline [:label (t :badge/Name) ": "] name])
                         (when-not (blank? description) [:div [:label (t :admin/Description)]  [:div.description description]])
                         ]])])
  )
            )[:div {:id "accordion" :class "panel-group evidence-list" :role "tablist" :aria-multiselectable "true"}] (:evidences data)))

(defn evidence-block [data state init-data]
  [:div#badge-settings
   [:div.form-group
    [:label {:class "col-md-12 sub-heading" :for "evidence"} (t :badge/Evidence)]


    [:div.col-md-9 {:class "new-evidence"}
     [:i.fa.fa-plus-square.fa-sm]
     [:a {:href "#" :on-click #(do (.preventDefault %)
                                 (swap! state assoc :evidence nil
                                        :show_form nil
                                        :input_mode nil)
                                 (swap! state assoc :tab [evidence-form data state init-data] :tab-no 2))} (t :badge/Addnewevidence)]]]


   [:div.form-group
    [:div.col-md-9 [evidence-list data state init-data]]]
   ])

(defn evidence-modal [state]
  (fn []
    [:div
     [evidence-form state]
     ]
    )
  )

