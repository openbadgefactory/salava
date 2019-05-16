(ns salava.profile.ui.edit
  (:require [reagent.core :refer [atom cursor create-class]]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for plugin-fun navigate-to]]
            [salava.core.i18n :refer [t]]
            [reagent-modals.modals :as m]
            [salava.core.ui.field :as f]
            [salava.core.helper :refer [dump]]
            [cljs-uuid-utils.core :refer [make-random-uuid uuid-string]]
            [salava.profile.schemas :as schema :refer [additional-fields]]))

(defn random-key []
  (-> (make-random-uuid)
      (uuid-string)))

(defn init-data [state]
  (ajax/GET
    (path-for "/obpv1/profile/user/edit")
    {:handler (fn [data]
                (swap! state assoc :edit-profile data))}))

(defn refresh-profile [state]
 (ajax/GET
  (path-for (str "/obpv1/profile/" (:user-id @state)) true)
  {:handler (fn [data]
              (let [data-with-uuids (assoc data :blocks (vec (map #(assoc % :key (random-key))
                                                                  (get data :blocks))))]
                (swap! state merge data-with-uuids)))}))

(defn prepare-blocks [blocks]
 (mapv (fn [b]
         (case (:type b)
           ("showcase") (-> b
                          (dissoc :key)
                          (select-keys [:id :type])
                          (merge {:format (or (:format b) "short") :title (:title b) :badges (map #(select-keys % [:id :visibility]) (:badges b))}))
           (-> b (dissoc :key)))) blocks))

(defn save-profile [state f show-alert?]
 (let [{:keys [profile_visibility about profile_picture]} (get-in @state [:edit-profile :user])
       profile-fields (->> (get-in @state [:edit-profile :profile])
                           (filter #(not-empty (:field %)))
                           (map #(select-keys % [:field :value])))
       blocks (prepare-blocks (:blocks @state))
       theme (or (:theme @state) 0)
       tabs @(cursor state [:tabs])
       alert-atom (cursor state [:alert])]
  (ajax/POST
    (path-for "/obpv1/profile/user/edit")
    {:params  {:profile_visibility profile_visibility
               :about              about
               :profile_picture    profile_picture
               :fields             profile-fields
               :blocks blocks
               :theme theme
               :tabs tabs}
     :handler (fn [data]
                (init-data state)
                (when show-alert? (reset! alert-atom data))
                (refresh-profile state)
                (when (and f (= "success" (:status data)) (f))))
     :finally (fn [] (js/setTimeout (fn [] (reset! alert-atom nil)) 3000))})))

(defn button-logic [state]
  {:content {:previous nil
             :next :theme
             :save-and-next! (fn [] (save-profile state (fn [] (reset! (cursor state [:edit :active-tab]) :theme)) false))}
   :theme {:previous :content
           :next :settings
           :save-and-previous! (fn [] (save-profile state (fn [] (reset! (cursor state [:edit :active-tab]) :content)) false))
           :save-and-next! (fn [] (save-profile state (fn [] (reset! (cursor state [:edit :active-tab]) :settings)) false))}
   :settings {:previous :theme
              :next :preview
              :save-and-previous! (fn [] (save-profile state (fn [] (reset! (cursor state [:edit :active-tab]) :theme))false))
              :save-and-next! (fn [] (save-profile state (fn [] (reset! (cursor state [:edit :active-tab]) :preview)) false))}
   :preview {:previous :settings
             :next :nil
             :save-and-previous! (fn [] (save-profile state (fn [] (reset! (cursor state [:edit :active-tab]) :settings)) false))}})


(defn action-buttons [state]
  (let [logic (button-logic state)
        current (cursor state [:edit :active-tab])
        previous? (get-in logic [@current :previous])
        next?  (get-in logic [@current :next])]
    (create-class {:reagent-render  (fn []
                                      [:div.action-bar {:id "page-edit"}
                                       [:div.row
                                        [:div.col-md-12
                                         (when previous? [:div {:id "step-button-previous"}
                                                          [:a {:href "#" :on-click #(do
                                                                                      (.preventDefault %)
                                                                                      (as-> (get-in logic [@current :save-and-previous!]) f (f)))}
                                                              (t :core/Previous)]])
                                         [:button {:class    "btn btn-primary"
                                                   :on-click #(do
                                                                (.preventDefault %)
                                                                (swap! state assoc :spinner true :active-button "btn-1")
                                                                (js/setTimeout (fn [] (save-profile state (fn [] (swap! state assoc :spinner false)) true)) 2000))}
                                          (when (and (:spinner @state) (= "btn-1" (:active-button @state))) [:i.fa.fa-spinner.fa-spin.fa-lg {:style {:padding "0 3px"}}])(t :page/Save)]
                                         [:button.btn.btn-warning {:href "#"
                                                                   :on-click #(do
                                                                                (.preventDefault %)
                                                                                (swap! state assoc :spinner true :active-button "btn-2")
                                                                                (save-profile state (fn [] (do
                                                                                                            (swap! state assoc :spinner false)
                                                                                                            (navigate-to (str "/profile/"(:user-id @state))))) true))}

                                          (when (and (:spinner @state) (= "btn-2" (:active-button @state))) [:i.fa.fa-spinner.fa-spin.fa-lg {:style {:padding "0 3px"}}]) (t :profile/Saveandclose) #_(t :page/View)]
                                         (when next?  [:div.pull-right {:id "step-button"}
                                                       [:a {:href "#" :on-click #(do
                                                                                   (.preventDefault %)
                                                                                   (as-> (get-in logic [@current :save-and-next!]) f (f)))}
                                                        (t :core/Next)]])]]
                                       (when (= "error" (get-in @state [:alert :status]))
                                         [:div.row
                                          [:div.col-md-12
                                           [:div {:class (str "alert " (case (get-in @state [:alert :status])
                                                                         "success" "alert-success"
                                                                         "error" "alert-warning"))
                                                  :style {:display "block" :margin-bottom "20px"}}
                                            (get-in @state [:alert :message] nil)]]])])})))


(defn send-file [files-atom profile-picture-atom]
  (let [file (-> (.querySelector js/document "#profile-picture-upload")
                 .-files
                 (.item 0))
        form-data (doto
                    (js/FormData.)
                    (.append "file" file (.-name file)))
        upload-modal (first (plugin-fun (session/get :plugins) "my" "upload_modal"))]
    (m/modal! (upload-modal nil (t :file/Uploadingfile) (t :file/Uploadinprogress)))
    (ajax/POST
      (path-for "/obpv1/file/upload_image")
      {:body    form-data
       :handler (fn [{:keys [status message reason data]} response]
                  (when (= status "success")
                    (reset! files-atom (conj @files-atom data))
                    (reset! profile-picture-atom (:path data)))
                  (m/modal! (upload-modal status message reason)))})))

(defn gallery-element [picture-data profile-picture-atom pictures-atom]
  (let [{:keys [path id]} picture-data
        current-profile-picture (session/get-in  [:user :profile_picture])
        profile-picture-fn (first (plugin-fun (session/get :plugins) "helper" "profile_picture"))
        delete-fn (first (plugin-fun (session/get :plugins) "my" "delete_file_modal"))]
    [:div {:key path
           :class (str "profile-picture-gallery-element " (if (= @profile-picture-atom path) "element-selected"))
           :on-click #(reset! profile-picture-atom path)}
     [:img {:src (profile-picture-fn path)}]
     (if (and (not (nil? id)) (not (= path current-profile-picture)))
       [:a {:class    "delete-icon"
            :title    (t :file/Delete)
            :on-click (fn []
                        (m/modal! [delete-fn id pictures-atom]
                                  {:size :lg}))}
        [:i {:class "fa fa-trash"}]])]))

(defn profile-picture-gallery [pictures-atom profile-picture-atom]
  [:div {:id "profile-picture-gallery" :class "row"}
   [:label.col-xs-12 (t :user/Selectprofilepicture)]
   [:div.col-xs-12
    [gallery-element {:path nil} profile-picture-atom]
    (into [:div]
          (for [picture-elem (map first (vals (group-by :path @pictures-atom)))]
            [gallery-element picture-elem profile-picture-atom pictures-atom]))]
   [:div.col-xs-12 {:id "profile-picture-upload-button"}
    [:button {:class "btn btn-primary"
              :on-click #(.preventDefault %)}
     (t :file/Upload)]
    [:input {:id "profile-picture-upload"
             :type "file"
             :name "file"
             :on-change #(send-file pictures-atom profile-picture-atom)
             :accept "image/*"}]]])

(defn add-content-modal [profile-fields-atom index]
  (fn []
    [:div {:id "badge-content"}
     [:div.modal-header
      [:button {:type         "button"
                :class        "close"
                :data-dismiss "modal"
                :aria-label   "OK"}
       [:span {:aria-hidden             "true"
               :dangerouslySetInnerHTML {:__html "&times;"}}]]]
     [:div.modal-body
      [:div#profile
       [:div.field-list
        [:h3.block-title (t :user/Addfield)]
        (reduce-kv
          (fn [r _ v]
            (conj r [:a {:on-click #(do
                                      (.preventDefault %)
                                      (if index
                                        (f/add-field profile-fields-atom {:field (:type v) :value ""} index)
                                        (f/add-field profile-fields-atom {:field (:type v) :value ""})))
                         :data-dismiss "modal"}

                     [:div {:style {:padding "5px"}}
                      [:span (t (:key v))]]]))
          [:div.block-types]
          additional-fields)]]]]))

(defn field-modal [profile-fields-atom index]
  (create-class {:reagent-render (fn [] (add-content-modal profile-fields-atom index))
                 :component-will-unmount (fn [] (m/close-modal!))}))

(defn add-field-after [fields field-atom index]
  [:div.add-info
   [:button {:class    "btn btn-success"
             :on-click #(do
                          (.preventDefault %)
                          (m/modal! [field-modal fields index] {:size :sm}))}
    (str "+ " (t :user/Addfield))]])

(defn profile-field [index profile-fields-atom]
  (let [profile-field-atom (cursor profile-fields-atom [index])
        last? (= index (dec (count @profile-fields-atom)))
        first? (= index 0)
        type (:field @profile-field-atom)]
    [:div {:key index}
     [add-field-after profile-fields-atom profile-field-atom index]
     [:div.field.thumbnail
      [:div.field-move
       [:div.move-arrows
        (when-not first?
          [:div.move-up {:on-click #(f/move-field :up profile-fields-atom index)}
           [:i {:class "fa fa-chevron-up"}]])
        (when-not last?
          [:div.move-down {:on-click #(f/move-field :down profile-fields-atom index)}
           [:i {:class "fa fa-chevron-down"}]])]]
      [:div.field-content
       [:div.form-group
        [:div.col-xs-8
         [:span {:style {:font-size "18px" :font-weight "600"}} (some-> (filter #(= type (:type %)) additional-fields) first :key t)]]
        [:div {:class "col-xs-4 field-remove"
               :on-click #(do (f/remove-field profile-fields-atom index))}
         [:span {:class "remove-button"}
          [:i {:class "fa fa-close"}]]]]
       [:div.form-group
        [:div.col-xs-12
         [:input {:type "text"
                  :class "form-control"
                  :value (:value @profile-field-atom)
                  :on-change #(swap! profile-field-atom assoc :value (.-target.value %))}]]]]]]))

(defn profile-fields [profile-fields-atom]
  [:div#profile
   (into [:form.form-horizontal]
         (for [index (range (count @profile-fields-atom))]
           (profile-field index profile-fields-atom)))
   [add-field-after profile-fields-atom]])

(defn edit-profile [state]
      (let [pictures-atom (cursor state [:edit-profile :picture_files])
            profile-picture-atom (cursor state [:edit-profile :user :profile_picture])
            about-me-atom (cursor state [:edit-profile :user :about])
            profile-fields-atom (cursor state [:edit-profile :profile])]
       (init-data state)
       (fn []
           [:div#edit-profile
            [:form.form-horizontal
             [profile-picture-gallery pictures-atom profile-picture-atom]
             [:div {:id "about-me" :class "form-group"}
              [:label.col-xs-12 (t :user/Aboutme)]
              [:div.col-xs-12
               [:textarea {:class "form-control" :rows 5 :cols 60 :value @about-me-atom :on-change #(reset! about-me-atom (.-target.value %))}]]]
             [:div.row
              [:label.col-xs-12 (t :profile/Additionalinformation)]
              [:div.col-xs-12
               (profile-fields profile-fields-atom)]]]])))
