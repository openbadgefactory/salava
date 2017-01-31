(ns salava.user.ui.edit-profile
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.field :as f]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.helper :refer [js-navigate-to path-for private?]]
            [salava.file.ui.my :as file]
            [salava.user.schemas :refer [contact-fields]]
            [salava.user.ui.helper :refer [profile-picture]]))

(defn save-profile [state]
  (let [{:keys [profile_visibility about profile_picture]} (:user @state)
        profile-fields (->> (:profile @state)
                            (filter #(not-empty (:field %)))
                            (map #(select-keys % [:field :value])))]
    (ajax/POST
      (path-for "/obpv1/user/profile")
      {:params  {:profile_visibility profile_visibility
                 :about              about
                 :profile_picture    profile_picture
                 :fields             profile-fields}
       :handler (fn [] (js-navigate-to (str "/user/profile/" (:user_id @state))))})))

(defn send-file [files-atom profile-picture-atom]
  (let [file (-> (.querySelector js/document "#profile-picture-upload")
                 .-files
                 (.item 0))
        form-data (doto
                    (js/FormData.)
                    (.append "file" file (.-name file)))]
    (m/modal! (file/upload-modal nil (t :file/Uploadingfile) (t :file/Uploadinprogress)))
    (ajax/POST
      (path-for "/obpv1/file/upload_image")
      {:body    form-data
       :handler (fn [{:keys [status message reason data]} response]
                  (when (= status "success")
                    (reset! files-atom (conj @files-atom data))
                    (reset! profile-picture-atom (:path data)))
                  (m/modal! (file/upload-modal status message reason)))})))

(defn gallery-element [picture-data profile-picture-atom]
  (let [{:keys [path]} picture-data]
    [:div {:key path
           :class (str "profile-picture-gallery-element " (if (= @profile-picture-atom path) "element-selected"))
           :on-click #(reset! profile-picture-atom path)}
     [:img {:src (profile-picture path)}]]))

(defn profile-picture-gallery [pictures-atom profile-picture-atom]
  [:div {:id "profile-picture-gallery" :class "row"}
   [:label.col-xs-12 (t :user/Selectprofilepicture)]
   [:div.col-xs-12
    [gallery-element {:path nil} profile-picture-atom]
    (into [:div]
          (for [picture-elem (map first (vals (group-by :path @pictures-atom)))]
            [gallery-element picture-elem profile-picture-atom]))]
   [:div.col-xs-12 {:id "profile-picture-upload-button"}
    [:button {:class "btn btn-primary"
              :on-click #(.preventDefault %)}
     (t :file/Upload)]
    [:input {:id "profile-picture-upload"
             :type "file"
             :name "file"
             :on-change #(send-file pictures-atom profile-picture-atom)
             :accept "image/*"}]]])

(def empty-field {:field "" :value ""})

(defn select-field-type [profile-field-atom]
  [:select {:class "form-control"
            :on-change #(swap! profile-field-atom assoc :field (.-target.value %))
            :value (:field @profile-field-atom)}
   [:option {:value ""} (str "- " (t :core/None) " -")]
   (doall
     (for [{:keys [type key]} contact-fields]
       [:option {:value type :key type} (t key)]))])

(defn profile-field [index profile-fields-atom]
  (let [profile-field-atom (cursor profile-fields-atom [index])
        last? (= index (dec (count @profile-fields-atom)))
        first? (= index 0)]
    [:div {:key index}
     [:div.add-field-after
      [:button {:class    "btn btn-success"
                :on-click #(do
                            (.preventDefault %)
                            (f/add-field profile-fields-atom empty-field index))}
       (t :user/Addfield)]]
     [:div.field
      [:div.field-move
       [:div.move-arrows
        (if-not first?
          [:div.move-up {:on-click #(f/move-field :up profile-fields-atom index)}
           [:i {:class "fa fa-chevron-up"}]])
        (if-not last?
          [:div.move-down {:on-click #(f/move-field :down profile-fields-atom index)}
           [:i {:class "fa fa-chevron-down"}]])]]
      [:div.field-content
       [:div.form-group
        [:div.col-xs-8
         (select-field-type profile-field-atom)]
        [:div {:class "col-xs-4 field-remove"
               :on-click #(f/remove-field profile-fields-atom index)}
         [:span {:class "remove-button"}
          [:i {:class "fa fa-close"}]]]]
       [:div.form-group
        [:div.col-xs-12
         [:input {:type "text"
                  :class "form-control"
                  :value (:value @profile-field-atom)
                  :on-change #(swap! profile-field-atom assoc :value (.-target.value %))}]]]]]]))

(defn profile-fields [profile-fields-atom]
  [:div {:id "field-editor"}
   (into [:div]
         (for [index (range (count @profile-fields-atom))]
           (profile-field index profile-fields-atom)))
   [:div.add-field-after
    [:button {:class    "btn btn-success"
              :on-click #(do
                          (.preventDefault %)
                          (f/add-field profile-fields-atom empty-field))}
     (t :user/Addfield)]]])

(defn content [state]
  (let [visibility-atom (cursor state [:user :profile_visibility])
        profile-picture-atom (cursor state [:user :profile_picture])
        about-me-atom (cursor state [:user :about])
        pictures-atom (cursor state [:picture_files])
        profile-fields-atom (cursor state [:profile])]
    [:div.panel {:id "edit-profile"}
     [m/modal-window]
     [:div.panel-body
      [:div.row [:div.col-xs-12 [:a {:href (path-for (str "/user/profile/" (:user_id @state)))} (t :user/Viewprofile)]]]
      [:form.form-horizontal
       (if-not (private?)
         [:div
          [:div.row [:label.col-xs-12 (t :user/Profilevisibility)]]
          [:div.radio {:id "visibility-radio-internal"}
           [:label [:input {:name      "visibility"
                            :value     "internal"
                            :type      "radio"
                            :checked   (= "internal" @visibility-atom)
                            :on-change #(reset! visibility-atom (.-target.value %))}]
            (t :user/Visibleonlytoregistered)]]
          [:div.radio
           [:label [:input {:name      "visibility"
                            :value     "public"
                            :type      "radio"
                            :checked   (= "public" @visibility-atom)
                            :on-change #(reset! visibility-atom (.-target.value %))}                               ]
            (t :core/Public)]]])
       [profile-picture-gallery pictures-atom profile-picture-atom]
       [:div.form-group
        [:label.col-xs-12 (t :user/Aboutme)]
        [:div.col-xs-12
         [:textarea {:class "form-control" :rows 5 :cols 60 :value @about-me-atom :on-change #(reset! about-me-atom (.-target.value %))}]]]
       [:div.row
        [:label.col-xs-12 (t :user/Contactinfo)]
        [:div.col-xs-12
         (profile-fields profile-fields-atom)]]

       [:div.row {:id "save-profile-buttons"}
        [:div.col-xs-12
         [:button {:id "save-profile-button"
                   :class "btn btn-primary"
                   :on-click #(do
                               (.preventDefault %)
                               (save-profile state))}
          (t :core/Save)]]]]]]))

(defn init-data [state]
  (ajax/GET
    (path-for "/obpv1/user/edit/profile" true)
    {:handler (fn [data]
                (reset! state data))}))

(defn handler [site-navi]
  (let [state (atom {:profile-fields []})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
