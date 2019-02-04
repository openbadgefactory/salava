(ns salava.badge.ui.evidence
  (:require [salava.core.ui.modal :as mo]
            [reagent.core :refer [cursor atom]]
            [salava.core.i18n :refer [t translate-text]]
            [reagent.session :as session]
            [salava.core.ui.helper :refer [plugin-fun path-for hyperlink base-url url?]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.time :refer [date-from-unix-time]]
            [clojure.string :refer [blank? includes? split trim starts-with?]]
            [salava.file.icons :refer [file-icon]]
            [salava.file.ui.my :refer [send-file]]
            #_[salava.badge.ui.settings :as se]))

#_(defn url? [s]
    (when-not (blank? s)
      (not (blank? (re-find #"^http" (str (trim s)))))))


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
        :handler #(init-settings badgeid state init-data)})
     )))

(defn set-page-visibility-to-private [page-id]
  (ajax/POST
    (path-for (str "/obpv1/page/toggle_visibility/" page-id))
    {:params {:visibility "public"}
     :handler (fn [data])}))

(defn save-badge-evidence [data state init-data ]
  (let [{:keys [id name narrative url resource_visibility properties]} (:evidence @state)
        {:keys [resource_type mime_type resource_id]} properties
        badge-id (:id data)]
    (swap! state assoc :evidence {:message nil}
           ;:input_mode nil
           )
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
                                                       :resource_visibility visibility})) }
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
                    (swap! state assoc :evidence {:message [upload-status (:status data) (:message data) (:reason data)]}))
                  #_(swap! state assoc :evidence {:message [upload-status (:status data) (:message data) (:reason data)]})
                  )


       :error-handler (fn [{:keys [status status-text]}]
                        (swap! state assoc :evidence {:message [upload-status "error" (t :file/Errorwhileuploading) (t :file/Filetoobig)]})
                        )})))

(defn files-grid [state]
  (let [files (:files @(cursor state [:files]))]
    [:div.col-md-12.resource-container
     (reduce (fn [r resource]
               (conj r [grid-element resource state :file_input])
               ) [:div [:label
                        [:span [:i.fa.fa-upload] (t :file/Upload) ]
                        [:input {:id "grid-file-upload"
                                 :type "file"
                                 :name "file"
                                 :on-change #(upload-file (cursor state [:files]) state)
                                 :style {:display "none"}}]]
                  [:div [:label {:style {:margin "5px"}} (t :badge/Orchoosefile)]]
                  ] files)]))

(defn pages-grid [state]
  (let [pages @(cursor state [:pages])]
    [:div.col-md-12.resource-container
     (reduce (fn [r resource]
               (conj r [grid-element resource state :page_input])
               ) [:div] pages)]))


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
