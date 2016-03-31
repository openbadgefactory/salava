(ns salava.file.ui.my
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [clojure.set :refer [intersection]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.file.icons :refer [file-icon]]
            [salava.core.ui.helper :refer [unique-values navigate-to]]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.grid :as g]
            [salava.core.ui.tag :as tag]
            [salava.core.i18n :refer [t]]
            [salava.core.time :refer [date-from-unix-time]]))

(defn upload-modal [status title message]
  [:div
   [:div.modal-header
    [:button {:type "button"
              :class "close"
              :data-dismiss "modal"
              :aria-label "OK"}
     [:span {:aria-hidden "true"
             :dangerouslySetInnerHTML {:__html "&times;"}}]]
    [:h4.modal-title title]]
   [:div.modal-body
    (if status
      [:div {:class (str "alert " (if (= status "error")
                                    "alert-warning"
                                    "alert-success"))}
       message]
      [:div
       [:i {:class "fa fa-cog fa-spin fa-2x"}]
       [:span " " message]])]
   (if status
     [:div.modal-footer
      [:button {:type "button"
                :class "btn btn-primary"
                :data-dismiss "modal"}
       "OK"]])])

(defn send-file [files-atom]
  (let [file (-> (.querySelector js/document "input")
                 .-files
                 (.item 0))
        form-data (doto
                    (js/FormData.)
                    (.append "file" file (.-name file)))]
    (m/modal! (upload-modal nil (t :file/Uploadingfile) (t :file/Uploadinprogress)))
    (ajax/POST
      "/obpv1/file/upload"
      {:body    form-data
       :handler (fn [data]
                  (if (= (:status data) "success")
                    (reset! files-atom (conj @files-atom (:data data))))
                  (m/modal! (upload-modal (:status data) (:message data) (:reason data))))})))

(defn delete-file [id files-atom]
  (ajax/DELETE
    (str "/obpv1/file/" id)
    {:handler (fn [data]
                (when (= (:status data) "success")
                  (reset! files-atom (vec (remove #(= id (:id %)) @files-atom)))
                  (m/close-modal!)))}))

(defn delete-file-modal [file-id files-atom]
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
     (t :file/Deleteconfirm)]]
   [:div.modal-footer
    [:button {:type "button"
              :class "btn btn-primary"
              :data-dismiss "modal"}
     (t :core/Cancel)]
    [:button {:type "button"
              :class "btn btn-warning"
              :on-click #(delete-file file-id files-atom)}
     (t :core/Delete)]]])

(defn save-tags [file-atom]
  (let [{:keys [id tags]} @file-atom]
    (ajax/POST (str "/obpv1/file/save_tags/" id)
               {:params {:tags tags}
                :handler (fn [])})))

(defn edit-file-modal [tags-atom new-tag-atom]
  [:div
   [:div.modal-header
    [:button {:type "button"
              :class "close"
              :data-dismiss "modal"
              :aria-label "OK"
              }
     [:span {:aria-hidden "true"
             :dangerouslySetInnerHTML {:__html "&times;"}}]]]
   [:div.modal-body
    [tag/tags tags-atom]
    [tag/new-tag-input tags-atom new-tag-atom]]
   [:div.modal-footer
    [:button {:type "button"
              :class "btn btn-primary"
              :data-dismiss "modal"}
     (t :core/Save)]]])

(defn file-grid-form [state]
  [:div {:id "grid-filter"
         :class "form-horizontal"}
   [g/grid-buttons (t :core/Tags ":") (unique-values :tags (:files @state)) :tags-selected :tags-all state]])

(defn file-grid-element [file-atom new-tag-atom files-atom]
  (let [{:keys [id name path mime_type ctime]} @file-atom
        tags-atom (cursor file-atom [:tags])
        profile-picture-path (session/get-in [:user :profile_picture])]
    [:div {:class "col-xs-12 col-sm-6 col-md-4"
           :key id}
     [:div {:class "media grid-container"}
      [:div.media-content
       [:div.media-left
        [:i {:class (str "file-icon-large fa " (file-icon mime_type))}]]
       [:div.media-body
        [:div.media-heading
         [:a.heading-link {:href (str "/" path)
                           :target "_blank"}
          name]]
        [:div.media-description
         [:div.file-create-date
          (date-from-unix-time (* 1000 ctime) "minutes")]]]]
      [:div {:class "media-bottom"}
       [:a {:class "bottom-link"
            :on-click (fn []
                        (m/modal! [edit-file-modal tags-atom new-tag-atom]
                                  {:size :lg :hide #(save-tags file-atom)}))}
        [:i {:class "fa fa-tags"}]
        [:span (t :file/Edittags)]]
       (if-not (= path profile-picture-path)
         [:a {:class "bottom-link pull-right"
              :on-click (fn []
                          (m/modal! [delete-file-modal id files-atom]
                                    {:size :lg}))}
          [:i {:class "fa fa-trash"}]
          [:span (t :file/Delete)]])]]]))

(defn file-visible? [file-tags tags-selected tags-all]
  (boolean
    (or (< 0
           (count
             (intersection
               (into #{} tags-selected)
               (into #{} file-tags))))
        (= tags-all true))))

(defn file-grid [state]
  (let [files (:files @state)]
    [:div {:class "row"
           :id    "grid"}
     [:div {:class "col-xs-12 col-sm-6 col-md-4"
            :id "add-element"
            :key "new-file"}
      [:input {:id "grid-file-upload"
               :type "file"
               :name "file"
               :on-change #(send-file (cursor state [:files]))}]
      [:div {:class "media grid-container"}
       [:div.media-content
        [:div.media-body
         [:div {:id "add-element-icon"}
          [:i {:class "fa fa-plus"}]]
         [:div
          [:a {:id "add-element-link"}
           (t :file/Upload)]]]]]]
     (doall
       (for [index (range (count files))]
         (if (file-visible? (get-in @state [:files index :tags]) (:tags-selected @state) (:tags-all @state))
           (file-grid-element (cursor state [:files index]) (cursor state [:new-tag]) (cursor state [:files])))))]))

(defn content [state]
  [:div {:id "my-files"}
   [m/modal-window]
   [file-grid-form state]
   [file-grid state]])

(defn init-data [state]
  (ajax/GET
    "/obpv1/file"
    {:handler (fn [data]
                (swap! state assoc :files (vec data)))}))

(defn handler [site-navi]
  (let [state (atom {:files         []
                     :tags-all      true
                     :tags-selected []
                     :new-tag ""})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
