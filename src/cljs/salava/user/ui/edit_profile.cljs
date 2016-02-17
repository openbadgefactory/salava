(ns salava.user.ui.edit-profile
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.helper :refer [navigate-to]]
            [salava.file.ui.my :as file]))

(def default-picture "img/user_default.png")

(defn save-profile [state]
  (let [{:keys [profile_visibility about profile_picture]} (:user @state)
        picture_path (->> (:picture_files @state)
                          (filter #(= profile_picture (:id %)))
                          first
                          :path)]
    (ajax/POST
      "/obpv1/user/profile"
      {:params  {:profile_visibility profile_visibility
                 :about              about
                 :profile_picture    picture_path}
       :handler (fn []
                  (navigate-to (str "/user/profile/" (:user_id @state))))})))

(defn send-file [files-atom]
  (.log js/console (-> (.querySelector js/document "#profile-picture-upload")))
  (let [file (-> (.querySelector js/document "#profile-picture-upload")
                 .-files
                 (.item 0))
        form-data (doto
                    (js/FormData.)
                    (.append "file" file (.-name file)))]
    (m/modal! (file/upload-modal nil (t :file/Uploadingfile) (t :file/Uploadinprogress)))
    (ajax/POST
      "/obpv1/file/upload"
      {:body    form-data
       :handler (fn [{:keys [status message reason data]} response]
                  (if (= status "success")
                    (reset! files-atom (conj @files-atom data)))
                  (m/modal! (file/upload-modal status message reason)))})))

(defn gallery-element [picture-data profile-picture-atom]
  (let [{:keys [id path]} picture-data]
    [:div {:key path
           :class (str "profile-picture-gallery-element " (if (= @profile-picture-atom id) "element-selected"))
           :on-click #(reset! profile-picture-atom id)}
     [:img {:src (str "/" (or path default-picture))}]]))

(defn profile-picture-gallery [pictures-atom profile-picture-atom]
  [:div {:id "profile-picture-gallery" :class "row"}
   [:label.col-xs-12 (t :user/Selectprofilepicture)]
   [:div.col-xs-12
    [gallery-element {:path nil} profile-picture-atom]
    (for [picture-elem @pictures-atom]
      [gallery-element picture-elem profile-picture-atom])]
   [:div.col-xs-12 {:id "profile-picture-upload-button"}
    [:button {:class "btn btn-primary"
              :on-click #(.preventDefault %)}
     (t :core/Upload)]
    [:input {:id "profile-picture-upload"
             :type "file"
             :name "file"
             :on-change #(send-file pictures-atom)
             :accept "image/*"}]]])

(defn content [state]
  (let [visibility-atom (cursor state [:user :profile_visibility])
        profile-picture-atom (cursor state [:user :profile_picture])
        about-me-atom (cursor state [:user :about])
        pictures-atom (cursor state [:picture_files])]
    [:div.panel {:id "edit-profile"}
     [m/modal-window]
     [:div.panel-body
      [:div.row [:div.col-xs-12 [:a {:href (str "/user/profile/" (:user_id @state))} (t :user/Viewprofile)]]]
      [:form
       [:div.row [:label.col-xs-12 (t :user/Profilevisibility)]]
       [:div.radio {:id "visibility-radio-internal"}
        [:label [:input {:name "visibility"
                         :value "internal"
                         :type "radio"
                         :checked (= "internal" @visibility-atom)
                         :on-change #(reset! visibility-atom (.-target.value %))}]
         (t :user/Visibleonlytoregeistered)]]
       [:div.radio
        [:label [:input {:name "visibility"
                         :value "public"
                         :type "radio"
                         :checked (= "public" @visibility-atom)
                         :on-change #(reset! visibility-atom (.-target.value %))}                               ]
         (t :core/Public)]]
       [profile-picture-gallery pictures-atom profile-picture-atom]
       [:div.row
        [:label.col-xs-12 (t :user/Aboutme)]
        [:div.col-xs-12
         [:textarea {:class "form-control" :rows 5 :cols 60 :value @about-me-atom :on-change #(reset! about-me-atom (.-target.value %))}]]]
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
    (str "/obpv1/user/edit/profile")
    {:handler (fn [data]
                (reset! state data))}))

(defn handler [site-navi]
  (let [state (atom {})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
