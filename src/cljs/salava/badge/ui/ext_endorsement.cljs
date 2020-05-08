(ns salava.badge.ui.ext-endorsement
 (:require
  [clojure.string :refer [blank?]]
  [reagent.core :refer [atom cursor]]
  [reagent-modals.modals :as m]
  [reagent.session :as session]
  [salava.badge.ui.endorsement :as end :refer [process-text profile]]
  [salava.core.ui.ajax-utils :as ajax]
  [salava.core.i18n :refer [t translate-text]]
  [salava.core.ui.helper :refer [path-for]]
  [salava.core.ui.input :refer [editor markdown-editor text-field file-input]]))


(defn status-modal [{:keys [message status]}]
  (let [alert-class (if (= status "success") "alert-success" "alert-danger")]
    [:div
     [:div.modal-header
      [:button {:type "button"
                :class "close"
                :data-dismiss "modal"
                :aria-label "OK"}
       [:span {:aria-hidden "true"
               :dangerouslySetInnerHTML {:__html "&times;"}}]]]
     [:div.modal-body
      [:div {:class (str "alert " alert-class)}
       message]]
     [:div.modal-footer
      [:button {:type "button"
                :class "btn btn-primary"
                :data-dismiss "modal"}
       "OK"]]]))

(defn init-endorsement [user-badge-id state]
  (ajax/GET
   (path-for (str "/obpv1/badge/user_endorsement/ext/" user-badge-id "/"(:endorser-id @state)))
   {:handler (fn [data]
               (reset! (cursor state [:ext-endorsement]) data))}))

(defn init-ext-endorser [id state]
  (ajax/GET
   (path-for (str "/obpv1/badge/user_endorsement/ext_request/endorser/" id))
   {:handler (fn [data]
               (reset! (cursor state [:ext-endorser]) data))}))


(defn init-request [endorser-id state]
  (ajax/GET
    (path-for (str "/obpv1/badge/user_endorsement/ext_request/info/" (:id @state) "/"endorser-id))
    {:handler (fn [data]
                (when-not (empty? data)
                  (reset! (cursor state [:request]) data)
                  (init-ext-endorser endorser-id state)
                  (init-endorsement (:id @state) state)))}))

(defn delete-endorsement [state]
  (let [id @(cursor state [:ext-endorsement :id])]
    (ajax/DELETE
      (path-for (str "/obpv1/badge/user_endorsement/ext/endorsement/" id))
      {:handler (fn [data]
                  (when (= (:status data) "success")
                    (m/modal! [status-modal {:status "success" :message (t :badge/Endorsementdeletesuccess)}] {})
                    (init-request (:id @state) state)))})))


(defn decline-endorsement-request [user-badge-id state]
  (ajax/POST
   (path-for (str "/obpv1/badge/user_endorsement/ext_request/update_status/" user-badge-id))
   {:params {:status "declined"
             :email @(cursor state [:ext-endorser :email])}
    :handler (fn [data]
               (when (= "success" (:status data))
                 (m/modal! [status-modal {:status "success" :message (t :badge/Requestsuccessfullydeclined)}] {})
                 (init-request (:endorser-id @state) state)))}))

(defn save-ext-endorsement [user-badge-id state update?]
 (let [path (if update?
                (str "/obpv1/badge/user_endorsement/ext/edit/" @(cursor state [:ext-endorsement :id]) "/" user-badge-id)
                (str "/obpv1/badge/user_endorsement/ext/endorse/" user-badge-id))]
  (ajax/POST
   (path-for path)
   {:params {:content (if update? @(cursor state [:ext-endorsement :content]) @(cursor state [:endorsement-comment]))
             :endorser (-> @(cursor state [:ext-endorser]) (select-keys [:name :url :email :description :ext_id :image_file]))}
    :handler (fn [{:keys [status]}]
               (when (= "error" status) (m/modal! [status-modal {:status "error" :message (t :core/Errorpage)}] {}))
               (when (= "success" status)
                 (swap! state assoc :show-link "none"
                                    :show-content "none")
                 (init-endorsement user-badge-id state)
                 (m/modal! [status-modal {:status "success" :message (t :badge/Endorsementsuccess)}] {})))})))

(defn upload-modal [{:keys [status message reason]}]
  [:div
   [:div.modal-header
    [:button {:type "button"
              :class "close"
              :data-dismiss "modal"
              :aria-label "OK"}
     [:span {:aria-hidden "true"
             :dangerouslySetInnerHTML {:__html "&times;"}}]]
    [:h4.modal-title (translate-text message)]]
   [:div.modal-body
    [:div {:class (str "alert " (if (= status "error")
                                  "alert-warning"
                                  "alert-success"))}
     (translate-text message)]]
   [:div.modal-footer
    [:button {:type "button"
              :class "btn btn-primary"
              :data-dismiss "modal"}
     "OK"]]])

(defn send-file [id state]
  (let [file (-> (.querySelector js/document "#picture-upload")
                 .-files
                 (.item 0))
        form-data (doto
                   (js/FormData.)
                   (.append "file" file (.-name file)))]
    (swap! state assoc :uploading-image true)
    (ajax/POST
     (path-for (str "/obpv1/badge/user_endorsement/ext_request/endorser/upload_image"))
     {:body    form-data
      :handler (fn [data]
                 (if (= (:status data) "success")
                   (do
                     (reset! (cursor state [:ext-endorser :image_file]) (:url data))
                     (reset! (cursor state [:uploading-image]) false))
                  (m/modal! (upload-modal data) {:hidden #(reset! (cursor state [:uploading-image]) false)})))})))

(defn endorser-info [id state]
  (let [{:keys [ext_id image_file name url description]} @(cursor state [:ext-endorser])]
   [:div.well.well-lg {:style {:padding-bottom "5px"}}
    [:form.form-horizontal
     [:div.form-group
      [:label {:for "input-name"} (t :badge/Name) [:span.form-required " *"] #_[info {:content (t :badgeIssuer/Badgenameinfo) :placement "right"}]]
      [text-field
       {:name "name"
        :atom (cursor state [:ext-endorser :name])
        :placeholder "input your name or the name of your organization" #_(t :badgeIssuer/Inputbadgename)}]]
     [:div.form-group
      [:label {:for "input-url"} (t :badge/URL)]
      [text-field
       {:name "url"
        :atom (cursor state [:ext-endorser :url])}]]
     [:div.form-group
      [:label {:for "input-image"} (t :badge/Logoorpicture)]
      [:p (t :badge/Uploadimginstructions)]
      [:div {:style {:margin "5px"}}
       (if-not @(cursor state [:uploading-image])
         (if-not (blank? image_file)
          [:img {:src (if (re-find #"^data:image" image_file)
                        image_file
                        (str "/" image_file))
                 :alt "image"
                 :style {:width "100px" :height "auto"}}]
          [:i.fa.fa-file-image-o {:style {:font-size "60px" :color "#757575"}}])
         [:span.fa.fa-spin.fa-cog.fa-2x])]
      [:div.text-center {:style {:margin "5px" :width "100px"}}
       [:span {:class "btn btn-primary btn-file btn-bulky"}
             [:input {:id "picture-upload"
                      :type       "file"
                      :name       "file"
                      :on-change  #(send-file ext_id state)
                      :accept     "image/png"}]
        [:span #_[:i.fa.fa-file-image-o.fa-lg.fa-fw {:style {:color "inherit"} }](t :file/Upload)]]]]]]))



(defn endorse-badge-content [state]
 (let [{:keys [ext_id image_file name url description]} @(cursor state [:ext-endorser])]
  (fn []
    [:div {:style {:display @(cursor state [:show-content])}}
     [:hr.border]
     #_[:div.row
        [:div.col-xs-12 {:style {:margin-bottom "10px"}} [:a.close {:aria-label "Cancel" :href "#" :on-click #(do
                                                                                                                (.preventDefault %)
                                                                                                                (swap! state assoc :show-link "block"
                                                                                                                       :show-content "none"))} [:i.fa.fa-remove {:title (t :core/Cancel)}]]]]


     [:div.endorse {:style {:margin "5px"}} (t :badge/Endorsehelptext)]

     [:div.row
      [:div.col-xs-12
       [:div.list-group
        [:a.list-group-item {:id "phrase1" :href "#" :on-click #(do
                                                                  (.preventDefault %)
                                                                  (process-text (t :badge/Endorsephrase1) state))}
         [:i.fa.fa-plus-circle] [:span (t :badge/Endorsephrase1)]]
        [:a.list-group-item {:href "#" :on-click #(do
                                                    (.preventDefault %)
                                                    (process-text (t :badge/Endorsephrase2) state))}
         [:i.fa.fa-plus-circle] [:span (t :badge/Endorsephrase2)]]
        [:a.list-group-item {:href "#" :on-click #(do
                                                    (.preventDefault %)
                                                    (process-text (t :badge/Endorsephrase3) state))}
         [:i.fa.fa-plus-circle] [:span (t :badge/Endorsephrase3)]]
        [:a.list-group-item {:href "#" :on-click #(do
                                                    (.preventDefault %)
                                                    (process-text (t :badge/Endorsephrase4) state))}
         [:i.fa.fa-plus-circle] [:span (t :badge/Endorsephrase4)]]
        [:a.list-group-item {:href "#" :on-click #(do
                                                    (.preventDefault %)
                                                    (process-text (t :badge/Endorsephrase5) state))}
         [:i.fa.fa-plus-circle] [:span (t :badge/Endorsephrase5)]]
        [:a.list-group-item {:href "#" :on-click #(do
                                                    (.preventDefault %)
                                                    (process-text (t :badge/Endorsephrase6) state))}
         [:i.fa.fa-plus-circle] [:span (t :badge/Endorsephrase6)]]]]]

     [:div.editor
      [:div.form-group
       [:label {:for (str "editor" ext_id)} (str (t :badge/Composeyourendorsement) ":") [:span.form-required " *"]]

       [:div [markdown-editor  (cursor state [:endorsement-comment]) (str "editor" ext_id)]]]

      [endorser-info ext_id state]
      [:div.text-center.btn-toolbar
       [:div.btn-group {:style {:float "unset"}}
        [:button.btn.btn-primary.btn-bulky {:on-click #(do
                                                         (.preventDefault %)
                                                         (save-ext-endorsement (:id @state) state nil))
                                            :disabled (or (blank? @(cursor state [:endorsement-comment])) (blank? @(cursor state [:ext-endorser :name])))}

         (t :badge/Endorsebadge)]

        [:button.btn.btn-danger.btn-bulky {:on-click #(do
                                                        (decline-endorsement-request (:id @state) state)
                                                        (.preventDefault %))}
          (t :badge/Declineendorsement)]]]


      [:hr.border]]])))


(defn ext-endorse-form [id state]
   (swap! state assoc :endorsement-comment "" :show-content "block" :show-link "none")
   ;(init-ext-endorser id state)
   (fn []
     [:div#endorsebadge {:style {:margin "20px auto"}}
       [endorse-badge-content state]]))

(defn endorsement [state]
 (let [{:keys [first_name last_name profile_picture status]} @(cursor state [:ext-endorsement])
       ext_id @(cursor state [:ext-endorser :ext_id])]
   [:div.well.well-lg {:style {:margin "10px auto" :background-image "none"}}
    [:div#badge-info
     [:div;.col-md-12
      [:p [:b (t :badge/Manageendorsementtext1)]]
      [:hr.border]
      [:div.row
       [:div.col-md-4.col-md-push-8  " "]
       [:div.col-md-8.col-md-pull-4  [profile {:status status :label (t :social/pending) :profile_picture profile_picture} (str first_name " " last_name)]]]
      [:div {:style {:margin-top "15px"}}
       [:div.editor
        [:div.form-group
         [:label {:for (str "editor" :ext_id)} (str (t :badge/Composeyourendorsement) ":") [:span.form-required " *"]]

         [:div [markdown-editor  (cursor state [:ext-endorsement :content]) (str "editor" ext_id)]]
         [endorser-info ext_id state]
         [:div.btn-toolbar.text-center
          [:div.btn-group {:style {:float "unset"}}
           [:button.btn.btn-primary.btn-bulky
            {:on-click #(save-ext-endorsement (:id @state) state true)
             :disabled (or (blank? @(cursor state [:ext-endorsement :content]))
                           (blank? @(cursor state [:ext-endorser :name])))}
            (t :badge/Save)]
           [:button.btn.btn-danger.btn-bulky
            {:on-click #(delete-endorsement state)}
            [:i.fa.fa-trash] (t :badge/Deleteendorsement)]]]]]]]]]))

(defn ext-endorse-badge [state]
  (let [{:keys [endorser-id user-logged-in?]} @state]
   (when (and (false? user-logged-in?) (not (nil? endorser-id)))
       (swap! state assoc :endorsement-comment "" :show-content "block" :show-link "none")
       (init-request endorser-id state)
       (fn []
        (if-not (empty?  @(cursor state [:ext-endorsement]))
         [endorsement state]
         (case @(cursor state [:request :status])
           "pending" [ext-endorse-form endorser-id state]
           "endorsed" [:div ""]
           [:div ""]))))))

(defn language-switcher [state]
 (let [{:keys [endorser-id user-logged-in?]} @state
       current-lang (session/get-in [:user :language] "en")
       languages (session/get :languages)]
  (when (and (false? user-logged-in?) (not (nil? endorser-id)))
    [:div.pull-right
     (doall
      (map (fn [lang]
             ^{:key lang} [:a {:style (if (= current-lang lang) {:font-weight "bold" :text-decoration "underline"} {})
                               :href "#"
                               :on-click #(session/assoc-in! [:user :language] lang)}
                           (clojure.string/upper-case lang) " "])
           languages))])))
