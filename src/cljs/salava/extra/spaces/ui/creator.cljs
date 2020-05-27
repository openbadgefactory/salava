(ns salava.extra.spaces.ui.creator
  (:require
   [clojure.string :refer [blank?]]
   [reagent.core :refer [atom cursor]]
   [reagent-modals.modals :as m]
   [salava.core.i18n :refer [t]]
   [salava.core.ui.ajax-utils :as ajax]
   [salava.core.ui.helper :refer [path-for navigate-to]]
   [salava.core.ui.input :refer [text-field textarea]]
   [salava.core.ui.layout :as layout]
   [salava.core.ui.modal :as mo]
   [salava.extra.spaces.ui.helper :refer [upload-modal profile-link-inline-modal]]))

(defn create-space [state]
  (ajax/POST
   (path-for "/obpv1/spaces/create")
   {:params (-> @(cursor state [:space])
                (assoc :admins (mapv :id @(cursor state [:space :admins]))))
    :handler (fn [data]
              (when (= (:status data) "error")
                (m/modal! (upload-modal data) {}))
              (when (= (:status data) "success")
                (navigate-to "admin/spaces")))}))

(defn send-file [id state type]
  (let [file (-> (.querySelector js/document "#picture-upload")
                 .-files
                 (.item 0))
        form-data (doto
                   (js/FormData.)
                   (.append "file" file (.-name file)))
        alert-atom  (case type
                      :logo (cursor state [:uploading-logo])
                      :banner (cursor state [:uploading-banner])
                      nil)
        image-atom (case type
                     :logo (cursor state [:space :logo])
                     :banner (cursor state [:space :banner])
                     nil)]
    (reset! alert-atom true)
    (ajax/POST
     (path-for (str "/obpv1/badge/user_endorsement/ext_request/endorser/upload_image"))
     {:body    form-data
      :handler (fn [data]
                 (if (= (:status data) "success")
                   (do
                     (reset! image-atom (:url data))
                     (reset! alert-atom false))
                  (m/modal! (upload-modal data) {:hidden #(reset! alert-atom false)})))})))


(defn content [state]
 (let [{:keys [logo banner]} @(cursor state [:space])]
   [:div
    [:div.row
     [m/modal-window]
     [:div.col-md-12
      [:p (t :extra-spaces/Createinstructions)]]

     [:div.col-md-12
      [:div.panel.panel-default
       [:div.panel-heading]
       [:div.panel-body
        [:div.col-md-12.panel-section
         [:form.form-horizontal
          [:div.form-group
           [:label {:for "input-name"} (t :extra-spaces/Organizationname) [:span.form-required " *"]]
           [text-field
            {:name "name"
             :atom (cursor state [:space :name])
             :placeholder (t :extra-spaces/Inputorganizationname)}]]
          [:div.form-group
           [:label {:for "input-alias"} (t :extra-space/Alias) [:span.form-required " *"]]
           [text-field
            {:name "name"
             :atom (cursor state [:space :alias])
             :placeholder (t :extra-spaces/Inputalias)}]]
          [:div.form-group
           [:label {:for "input-description"} (t :extra-spaces/Description) [:span.form-required " *"]]
           [textarea
            {:name "description"
             :atom (cursor state [:space :description])
             :placeholder (t :extra-spaces/Inputdescription)}]]
          [:div.form-group
           [:label {:for "input-logo"} (t :extra-spaces/Logo)]
           [:p (t :extra-spaces/Uploadlogoinstructions)]
           [:div {:style {:margin "5px"}}
            (if-not @(cursor state [:uploading-logo])
              (if-not (blank? @(cursor state [:space :logo]))
               [:img {:src (if (re-find #"^data:image" logo)
                             logo
                             (str "/" logo))
                      :alt "image"
                      :style {:width "100px" :height "auto"}}]
               [:i.fa.fa-building {:style {:font-size "60px" :color "#757575"}}])
              [:span.fa.fa-spin.fa-cog.fa-2x])]
           [:div {:style {:margin "5px" :width "100px"}}
            [:span {:class "btn btn-primary btn-file btn-bulky"}
             [:input {:id "picture-upload"
                      :type       "file"
                      :name       "file"
                      :on-change  #(send-file nil state :logo)
                      :accept     "image/png"}]
             [:span (t :file/Upload)]]]]
          [:div.form-group
           [:label {:for "input-banner"} (t :extra-spaces/Banner)]
           [:p (t :extra-spaces/Uploadbannerinstructions)]
           [:div {:style {:margin "5px"}}
            (if-not @(cursor state [:uploading-banner])
              (if-not (blank? @(cursor state [:space :banner]))
               [:img {:src (if (re-find #"^data:image" banner)
                             banner
                             (str "/" banner))
                      :alt "image"}]
                      ;:style {:width "100px" :height "auto"}}]
               [:i.fa.fa-building {:style {:font-size "60px" :color "#757575"}}])
              [:span.fa.fa-spin.fa-cog.fa-2x])]
           [:div {:style {:margin "5px" :width "100px"}}
            [:span {:class "btn btn-primary btn-file btn-bulky"}
             [:input {:id "picture-upload"
                      :type       "file"
                      :name       "file"
                      :on-change  #(send-file nil state :banner)
                      :accept     "image/png"}]
             [:span (t :file/Upload)]]]]
          [:div.form-group
           [:label {:for "p-color"} (str (t :extra-spaces/Primarycolor) ": ")]
           [:input#p-color.form-control
            {:style {:max-width "100px" :display "inline-block" :margin "0 5px"}
             :type "color"
             ;:value "#000000"
             :on-change #(do
                           (.preventDefault %)
                           (reset! (cursor state [:space :properties :css :p-color]) (.-target.value %)))}]]
          [:div.form-group
           [:label {:for "s-color"} (str (t :extra-spaces/Secondarycolor) ": ")]
           [:input#s-color.form-control
            {:style {:max-width "100px" :display "inline-block" :margin "0 5px"}
             :type "color"
             ;:value "#000000"
             :on-change #(do
                           (.preventDefault %)
                           (reset! (cursor state [:space :properties :css :s-color]) (.-target.value %)))}]]

          [:div.form-group
           [:label {:for "t-color"} (str (t :extra-spaces/Tertiarycolor) ": ")]
           [:input#t-color.form-control
            {:style {:max-width "100px" :display "inline-block" :margin "0 5px"}
             :type "color"
             ;:value "#000000"
             :on-change #(do
                           (.preventDefault %)
                           (reset! (cursor state [:space :properties :css :t-color]) (.-target.value %)))}]]

          [:div#social-tab.form-group {:style {:background-color "ghostwhite" :padding "8px"}}
           [:p (t :extra-space/Aboutadmins)]
           [:div
             [:a {:href "#"
                  :on-click #(do
                               (.preventDefault %)
                               (mo/open-modal [:gallery :profiles]
                                {:type "pickable"
                                 :selected-users-atom (cursor state [:space :admins])
                                 :context "space_admins"} {}))}

              [:span [:i.fa.fa-user-plus.fa-fw.fa-lg] (t :extra-spaces/Addadmins)]]]
           #_(when (seq @(cursor state [:space :admins]))
               [:div {:style {:margin "20px 0"}} [:i.fa.fa-users.fa-fw]
                [:a {:href "#"
                     :on-click #(mo/open-modal [:gallery :profiles] {:type "pickable" :selected-users-atom (cursor state [:space :admins]) :context "space_admins"})}
                 (t :badge/Editselectedusers)]])
           (reduce (fn [r u]
                     (let [{:keys [id first_name last_name profile_picture]} u]
                       (conj r [:div.user-item [profile-link-inline-modal id first_name last_name profile_picture]
                                [:a {:href "#" :on-click (fn [] (reset! (cursor state [:space :admins]) (->> @(cursor state [:space :admins]) (remove #(= id (:id %))) vec)))}
                                 [:span.close {:aria-hidden "true" :dangerouslySetInnerHTML {:__html "&times;"}}]]])))
                   [:div.selected-users-container] @(cursor state [:space :admins]))]

          [:hr.border]
          [:div.text-center;.col-md-12
           [:div.btn-toolbar
            [:div.btn-group {:style {:float "unset"}}
             [:button.btn.btn-primary.btn-bulky
              {:type "button"
               :on-click #(do
                           (.preventDefault %)
                           (create-space state))}
              (t :extra-spaces/Createorganization)]
             [:button.btn.btn-warning.btn-bulky
              {:type "button"
               :on-click #(do
                           (.preventDefault %)
                           (reset! (cursor state [:space]) nil)
                           (navigate-to "admin/spaces"))}
              (t :core/Cancel)]]]]]]]]]]]))



(defn handler [site-navi]
 (let [state (atom {:space {:admins []}})]
   (fn []
    (layout/default site-navi [content state]))))
