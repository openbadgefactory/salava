(ns salava.extra.spaces.ui.creator
  (:require
   [clojure.string :refer [blank? lower-case replace]]
   [reagent.core :refer [atom cursor]]
   [reagent.session :as session]
   [reagent-modals.modals :as m]
   [salava.core.i18n :refer [t translate-text]]
   [salava.core.ui.ajax-utils :as ajax]
   [salava.core.ui.helper :refer [path-for navigate-to base-path]]
   [salava.core.ui.input :refer [text-field textarea]]
   [salava.core.ui.layout :as layout]
   [salava.core.ui.modal :as mo]
   [salava.extra.spaces.ui.helper :refer [upload-modal profile-link-inline-modal create-space edit-space generate-alias]]))

#_(defn create-space [state]
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
  (let [file (-> (.querySelector js/document id)
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
     (path-for (str "/obpv1/spaces/upload_image/" (name type)))
     {:body    form-data
      :handler (fn [data]
                 (if (= (:status data) "success")
                   (do
                     (reset! image-atom (:url data))
                     (reset! alert-atom false))
                  (if (:in-modal @state)
                      (reset! (cursor state [type :error]) (:message data))
                      (m/modal! (upload-modal data) {:hidden #(reset! alert-atom false)}))))})))
       ;:finally (fn [] (reset! (cursor state [type :error]) nil))})))

(defn create-form [state]
  (let [{:keys [logo banner name]} @(cursor state [:space])]
   [:div
    [:div.row
     #_[:div.col-md-12
        [:p (t :extra-spaces/Createinstructions)]]

     [:div.col-md-12
      [:div.panel.panel-default
       [:div.panel-heading
         [:div.panel-title
          (if @(cursor state [:in-modal])
            [:div (if logo [:img {:src (if (re-find #"^data:image" logo) logo (str "/" logo))
                                  :style {:width "40px" :height "40px"}}]
                           [:i.fa.fa-building.fa-fw.fa-2x])
                [:h4.inline " " (t :extra-spaces/Edit) "/" name]]
            (t :extra-spaces/Createmember))]]
       [:div.panel-body
        [:div.col-md-12.panel-section
         [:form.form-horizontal
          [:div.form-group
           [:label {:for "input-name"} (t :extra-spaces/Name) [:span.form-required " *"]]
           [:input#input-name.form-control ;text-field
            {:name "name"
             :value @(cursor state [:space :name])
             :placeholder (t :extra-spaces/Inputmembername)
             :on-change #(do
                           (reset! (cursor state [:space :name]) (.-target.value %))
                           (generate-alias state))}]]
                           ;(when (clojure.string/blank? @(cursor state [:space :alias])) (generate-alias state)))}]]
          (when-not (:in-modal @state)
           [:div.form-group
            [:label {:for "input-alias"} (t :extra-spaces/Alias) [:span.form-required " *"]]
            [:p (t :extra-spaces/Aboutalias)]
            [:input.form-control ;text-field
             {:name "alias"
              :value @(cursor state [:space :alias])
              :placeholder (t :extra-spaces/Inputalias)
              :on-change #(reset! (cursor state [:space :alias])
                            (as-> (.-target.value %) $
                                  (replace $ #" " "")
                                  (lower-case $)
                                  (if (> (count $) 25) (clojure.string/join (take 25 $)) $)))}]
            [:p {:style {:margin-top "5px"}} [:b (str (str (session/get :site-url) " " (session/get :base-path) "/") @(cursor state [:space :alias]))]]])
          [:div.form-group
           [:label {:for "input-description"} (t :extra-spaces/Description) [:span.form-required " *"]]
           [textarea
            {:name "description"
             :atom (cursor state [:space :description])
             :placeholder (t :extra-spaces/Inputdescription)}]]
          [:div.form-group
           [:label {:for "input-logo"} (t :extra-spaces/Logo) [:span.form-required " *"]]
           [:p (t :badge/Uploadimginstructions)]
           [:div {:style {:margin "5px"}}
            (when @(cursor state [:logo :error])
              [:div.alert.alert-warning
               (t (translate-text @(cursor state [:logo :error])))])
            (if-not @(cursor state [:uploading-logo])
              (if-not (blank? @(cursor state [:space :logo]))
               [:img {:src (if (re-find #"^data:image" logo)
                             logo
                             (str "/" logo))
                      :alt "image"
                      :style {:width "100px" :height "auto"}}]
               [:i.fa.fa-building {:style {:font-size "60px" :color "#757575"}}])
              [:span.fa.fa-spin.fa-cog.fa-2x])]
           [:div.btn-toolbar {:style {:margin "5px"}} ;:width "100px"}}
            [:div.btn-group
             [:span {:class "btn btn-primary btn-file btn-bulky"}
              [:input {:id "logo-upload"
                       :type       "file"
                       :name       "file"
                       :on-change  #(send-file "#logo-upload" state :logo)
                       :accept     "image/png"}]
              [:span (t :file/Upload)]]
             (when-not (blank? @(cursor state [:space :logo]))
               [:button.btn-warning.btn.btn-bulky
                {:on-click #(do
                             (.preventDefault %)
                             (reset! (cursor state [:space :logo]) nil))
                  :aria-label (t :extra-spaces/Remove)}
                (t :extra-spaces/Remove)])]]]
          [:div.form-group
           [:label {:for "input-banner"} (t :extra-spaces/Banner)]
           [:div [:p (t :extra-spaces/Uploadbannerinstructions)]]
           [:div {:style {:margin "5px"}}
            (when @(cursor state [:banner :error])
              [:div.alert.alert-warning
               (t (translate-text @(cursor state [:banner :error])))])
            (if-not @(cursor state [:uploading-banner])
              (if-not (blank? @(cursor state [:space :banner]))
               [:div.space-banner-container
                 [:img {:src (if (re-find #"^data:image" banner)
                                banner
                               (str "/" banner))
                        :alt "image"}]]
                      ;:style {:width "100px" :height "auto"}}]
               [:div.space-banner-container]);[:p (t :extra-spaces/Uploadbannerinstructions)]])
              [:span.fa.fa-spin.fa-cog.fa-2x])]
           [:div.btn-toolbar {:style {:margin "5px"}} ;:width "100px"}}
            [:div.btn-group
             [:span {:class "btn btn-primary btn-file btn-bulky"}
              [:input {:id "banner-upload"
                       :type       "file"
                       :name       "file"
                       :on-change  #(send-file "#banner-upload" state :banner)
                       :accept     "image/png"}]
              [:span (t :file/Upload)]]
             (when-not (blank? @(cursor state [:space :banner]))
               [:button.btn-warning.btn.btn-bulky
                {:on-click #(do
                             (.preventDefault %)
                             (reset! (cursor state [:space :banner]) nil))
                  :aria-label (t :extra-spaces/Remove)}
                (t :extra-spaces/Remove)])]]]

          [:div.colors
            [:div [:span._label.form-group (t :extra-spaces/Theme)] "-" [:i (t :extra-spaces/optional)]]

            [:div.form-group
             [:p (t :extra-spaces/Aboutcolors)]
             [:p (t :extra-spaces/Aboutcolors2)]]
            [:div.form-group
             [:label {:for "p-color"} (str (t :extra-spaces/Primarycolor) ": ")]
             [:input#p-color.form-control
              {:style {:max-width "100px" :display "inline-block" :margin "0 5px"}
               :type "color"
               :value @(cursor state [:space :css :p-color])
               :on-change #(do
                             (.preventDefault %)
                             (reset! (cursor state [:space :css :p-color]) (.-target.value %)))}]]
            [:div.form-group
             [:label {:for "s-color"} (str (t :extra-spaces/Secondarycolor) ": ")]
             [:input#s-color.form-control
              {:style {:max-width "100px" :display "inline-block" :margin "0 5px"}
               :type "color"
               :value @(cursor state [:space :css :s-color])
               :on-change #(do
                             (.preventDefault %)
                             (reset! (cursor state [:space :css :s-color]) (.-target.value %)))}]]

            [:div.form-group
             [:label {:for "t-color"} (str (t :extra-spaces/Tertiarycolor) ": ")]
             [:input#t-color.form-control
              {:style {:max-width "100px" :display "inline-block" :margin "0 5px"}
               :type "color"
               :value @(cursor state [:space :css :t-color])
               :on-change #(do
                             (.preventDefault %)
                             (reset! (cursor state [:space :css :t-color]) (.-target.value %)))}]]]

          [:div#social-tab.form-group {:style {:background-color "ghostwhite" :padding "8px"}}
           [:span._label (t :extra-spaces/Admins)]
           [:p (t :extra-spaces/Aboutadmins)]
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
                   [:div.selected-users-container] @(cursor state [:space :admins]))]]]]]]]]))

(defn modal-content [state]
  (let [{:keys [logo banner name]} @(cursor state [:space])]
    [:div
     [:div.row
      [create-form state]

      [:hr.border]
      (when @(cursor state [:error-message])
         [:div.alert.alert-danger @(cursor state [:error-message])])
      [:div.text-center;.col-md-12
       [:div.btn-toolbar
        [:div.btn-group {:style {:float "unset"}}
         [:button.btn.btn-primary.btn-bulky
          {:type "button"
           :on-click #(do
                        (.preventDefault %)
                        (edit-space state))}
          (t :extra-spaces/Editspace)]
         [:button.btn.btn-warning.btn-bulky
          {:type "button"
           :on-click #(do
                        (.preventDefault %)
                        ;(reset! (cursor state [:space]) nil)
                        (swap! state assoc :tab nil :tab-no 1))}
                        ;(m/close-modal!))}
          (t :core/Cancel)]]]]]]))

(defn content [state]
  (let [{:keys [logo banner]} @(cursor state [:space])]
    [:div
     [m/modal-window]
     [:div.row

      [create-form state]


      [:hr.border]
      [:div.text-center;.col-md-12
       [:div.btn-toolbar
        [:div.btn-group {:style {:float "unset"}}
         [:button.btn.btn-primary.btn-bulky
          {:type "button"
           :on-click #(do
                        (.preventDefault %)
                        (create-space state))}
          (t :extra-spaces/Createspace)]
         [:button.btn.btn-warning.btn-bulky
          {:type "button"
           :on-click #(do
                        (.preventDefault %)
                        (reset! (cursor state [:space]) nil)
                        (navigate-to "admin/spaces"))}
          (t :core/Cancel)]]]]]]))



(defn handler [site-navi]
 (let [state (atom {:space {:admins []}})]
                            ;:css {}}})]
   (fn []
    (layout/default site-navi [content state]))))
