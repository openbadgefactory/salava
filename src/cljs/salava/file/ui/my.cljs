(ns salava.file.ui.my
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.set :refer [intersection]]
            [ajax.core :as ajax]
            [salava.file.icons :refer [file-icon]]
            [salava.core.ui.helper :as h :refer [unique-values navigate-to]]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.grid :as g]
            [salava.core.ui.tag :as tag]
            [salava.core.i18n :refer [t]]
            [salava.core.time :refer [date-from-unix-time]]
            [salava.core.helper :refer [dump]]))

(defn delete-file [id]
  (ajax/DELETE
    (str "/obpv1/file/" id)
    {:handler (fn [data]
                (let [data-with-kws (keywordize-keys data)]
                  (if (= (:status data-with-kws) "success")
                    (navigate-to "/page/files"))))}))

(defn delete-file-modal [file-id]
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
              :on-click #(delete-file file-id)}
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
   [g/grid-buttons (str (t :core/Tags) ":") (unique-values :tags (:files @state)) :tags-selected :tags-all state]])

(defn file-grid-element [file-atom new-tag-atom]
  (let [{:keys [id name path mime_type ctime]} @file-atom
        tags-atom (cursor file-atom [:tags])]
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
                                  {:size :lg
                                   :hide #(save-tags file-atom)}))}
        [:i {:class "fa fa-tags"}]
        [:span (t :file/Edittags)]]
       [:a {:class "bottom-link pull-right"
            :on-click (fn []
                        (m/modal! [delete-file-modal id]
                                  {:size :lg
                                   :hide #(save-tags file-atom)}))}
        [:i {:class "fa fa-trash"}]
        [:span (t :file/Delete)]]]]]))

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
      [:div {:class "media grid-container"}
       [:div.media-content
        [:div.media-body
         [:div {:id "add-element-icon"}
          [:i {:class "fa fa-plus"}]]
         [:div
          [:a {:id "add-element-link"
               :href "/file/upload"}
           (t :file/Upload)]]]]]]
     (doall
       (for [index (range (count files))]
         (if (file-visible? (get-in @state [:files index :tags]) (:tags-selected @state) (:tags-all @state))
           (file-grid-element (cursor state [:files index]) (cursor state [:new-tag])))))]))

(defn content [state]
  [:div {:class "my-files"}
   [m/modal-window]
   [file-grid-form state]
   [file-grid state]])

(defn init-data [state]
  (ajax/GET
    "/obpv1/file/1"
    {:handler (fn [data]
                (let [data-with-kws (map keywordize-keys data)]
                  (swap! state assoc :files (vec data-with-kws))))}))

(defn handler [site-navi]
  (let [state (atom {:files         []
                     :tags-all      true
                     :tags-selected []
                     :new-tag ""})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
