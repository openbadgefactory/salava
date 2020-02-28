(ns salava.badge.ui.endorsement
  (:require [salava.core.i18n :refer [t]]
            [reagent.core :refer [atom cursor create-class]]
            [reagent.dom :as reagent]
            [salava.core.time :refer [unix-time date-from-unix-time]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for private?]]
            [salava.core.ui.modal :as mo]
            [clojure.string :refer [blank?]]
            [reagent.session :as session]
            [salava.user.ui.helper :refer [profile-picture profile-link-inline-modal]]
            [reagent-modals.modals :as m]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.error :as err]
            [salava.core.ui.grid :as g]
            ;[cljsjs.simplemde]
            [salava.translator.ui.helper :refer [translate]]
            [salava.core.ui.popover :refer [info]]
            [dommy.core :as dommy :refer-macros [sel1 sel]]
            [salava.core.ui.input :refer [editor markdown-editor]]))

(defn endorsement-row [endorsement & lang]
  (let [{:keys [issuer content issued_on]} endorsement]
    [:div {:style {:margin-bottom "20px"}}
     [:h5
      (when (:image_file issuer) [:img {:src (str "/" (:image_file issuer)) :style {:width "55px" :height "auto" :padding "7px"}}])
      [:a {:href "#"
           :on-click #(do (.preventDefault %) (mo/set-new-view [:badge :issuer] {:id (:id issuer) :lang (first lang)} #_(:id issuer)))}
       (:name issuer)]
      " "
      [:small (date-from-unix-time (* 1000 issued_on))]]
     [:div {:dangerouslySetInnerHTML {:__html content}}]]))

(defn init-badge-endorsements [state badge-id]
  (ajax/GET
   (path-for (str "/obpv1/badge/endorsement/" badge-id))
   {:handler (fn [data] (reset! state (:endorsements data)))}))

(defn init-user-badge-endorsement [state]
  (ajax/GET
   (path-for (str "/obpv1/badge/user_endorsement/" (:id @state)))
   {:handler (fn [data]
               (let [pending-endorsements-count (->> data :endorsements (filter #(= "pending" (:status %))) count)]
                 (swap! state assoc :user-badge-endorsements (:endorsements data)
                        :pending_endorsements_count pending-endorsements-count)
                 (when (some #(= (:issuer_id %) (:endorser-id @state)) (:endorsements data))
                   (swap! state assoc :show-link "none"
                          :show-content "none"
                          :show-endorsement-status "block"))))
    :finally (fn []
               (when (and @(cursor state [:pending-info-atom]) @(cursor state [:pending-endorsements-atom]))
                 (reset! @(cursor state [:pending-info-atom]) (dec @@(cursor state [:pending-info-atom])))
                 (reset! @(cursor state [:pending-endorsements-atom]) (:pending_endorsements_count @state))
                 (when @(cursor state [:reload-fn]) ((:reload-fn @state)))))}))

(defn user-badge-endorsement-content [badge-id badge-endorsements & lang]
  (let [state (atom {:id badge-id})]
    (init-user-badge-endorsement state)
    (fn []
      (let [endorsements (filter #(= (:status %) "accepted") (:user-badge-endorsements @state))
            badge-endorsements? (pos? (count @badge-endorsements))]
        (when (seq endorsements)
          [:div
           (if badge-endorsements? [:hr.line])
           [:h4 {:style {:margin-bottom "20px"}} (translate (first lang) :badge/BadgeEndorsedByIndividuals)]
           (reduce (fn [r endorsement]
                     (let [{:keys [id user_badge_id image_file name content issuer_name profile_picture profile_visibility issuer_id mtime]} endorsement
                           disabled (and (= profile_visibility "internal") (nil? (session/get :user)))]
                       (conj r [:div {:style {:margin-bottom "20px"}}

                                [:h5
                                 [:img {:src (profile-picture profile_picture) :style {:width "55px" :height "auto" :padding "7px"}}]
                                 (if (and issuer_id (not disabled)) [:a {:href "#"
                                                                         :on-click #(do (.preventDefault %) (mo/set-new-view [:profile :view] {:user-id issuer_id}))}
                                                                     issuer_name] issuer_name)
                                 " "
                                 [:small (date-from-unix-time (* 1000 mtime))]]
                                [:div {:dangerouslySetInnerHTML {:__html content}}]])))
                   [:div] endorsements)])))))

(defn badge-endorsement-content [param]
  (let [endorsements (atom [])
        badge-id (if (map? param) (:badge-id param) param)
        user-badge-id (:id param)
        lang (:lng param)]
    (init-badge-endorsements endorsements badge-id)
    (fn []
      (let [endorsement-count (count @endorsements)]
        [:div.row {:id "badge-contents"}
         (when (seq @endorsements)
           [:div.col-xs-12
            [:h4 {:style {:margin-bottom "20px"}} (translate lang :badge/BadgeEndorsedByOrganizations)]
            (into [:div]
                  (for [endorsement @endorsements]
                    (endorsement-row endorsement lang)))])
         (when user-badge-id
           [:div.col-xs-12
            [user-badge-endorsement-content user-badge-id endorsements lang]])]))))

;; User Badge Endorsements
(defn init-user-endorsements [state]
  (ajax/GET
   (path-for (str "/obpv1/badge/user_endorsement/_/all"))
   {:handler (fn [data]
               (reset! state (assoc data
                                    :initializing false
                                    :permission "success"
                                    :show (session/get! :visible-area (if (pos? (->> (:requests data) (filter #(= "pending" (:status %))) count)) "requests" "all"))
                                    :search ""
                                    :show-delete-dialogue false
                                    :order "mtime")))}

   (fn [] (swap! state assoc :permission "error"))))

(defn init-pending-endorsements [state]
  (ajax/GET
   (path-for "/obpv1/badge/user_endorsement/_/pending")
   {:handler (fn [data]
               (swap! state assoc :pending (:endorsements data)))}))

(defn edit-endorsement [id badge-id content]
  (ajax/POST
   (path-for (str "/obpv1/badge/user_endorsement/edit/" id))
   {:params {:content content
             :user_badge_id badge-id}
    :handler (fn [data]
               (when (= "success" (:status data))))}))

(defn- init-pending-requests [state]
  (ajax/GET
   (path-for (str "/obpv1/badge/user_endorsement/request/pending"))
   {:handler (fn [data] (reset! state data))}))

(defn update-request-status! [id status state reload-fn]
  (when id
    (ajax/POST
     (path-for (str "/obpv1/badge/user_endorsement/request/update_status/" id))
     {:params {:status status}
      :handler (fn [data] (when (and reload-fn (= "success" (:status data)) (reload-fn))))})))

(defn save-endorsement [state]
  (ajax/POST
   (path-for (str "/obpv1/badge/user_endorsement/" (:id @state)))
   {:params {:content @(cursor state [:endorsement-comment])}
    :handler (fn [data]
               (when (= (:status data) "success")
                 (swap! state assoc :show-link "none"
                        :show-content "none"
                        :show-endorsement-status "block")
                 (when @(cursor state [:request_id])
                   (update-request-status! @(cursor state [:request_id]) "endorsed" state nil))))}))

(defn update-status [id status user_badge_id state reload-fn]
  (ajax/POST
   (path-for (str "/obpv1/badge/user_endorsement/update_status/" id))
   {:params {:user_badge_id user_badge_id
             :status status}
    :handler (fn [data]
               (when (= "success" (:status data))
                 (when reload-fn (reload-fn state))))}))

(defn delete-endorsement [id user_badge_id state reload-fn]
  (ajax/DELETE
   (path-for (str "/obpv1/badge/user_endorsement/" user_badge_id "/" id))
   {:handler (fn [data]
               (when (= "success" (:status data))
                 (when reload-fn (reload-fn state))))}))

#_(def simplemde-toolbar (array "bold" "italic" "heading-3"
                                "quote" "unordered-list" "ordered-list"
                                "link" "horizontal-rule"
                                "preview"))

#_(def editor (atom nil))

;(def md-editor (atom {:editor nil :enabled? true}))

#_(defn init-editor [element-id value]
    (let [editor (cursor md-editor [:editor])
          md? (cursor md-editor [:enabled?])]
      (reset! editor (js/SimpleMDE. (clj->js {:element (.getElementById js/document element-id)
                                              :toolbar simplemde-toolbar
                                              :spellChecker false
                                              :forceSync true})))
      (reset! md? @md?)
      (when-not @md? (.toTextArea @editor))
      (.value @editor @value)
      (js/setTimeout (fn [] (.value @editor @value)) 200)
      (.codemirror.on @editor "change" (fn [] (reset! value (.value @editor))))))

#_(defn init-editor [element-id value]
    (reset! editor (js/SimpleMDE. (clj->js {:element (.getElementById js/document element-id)
                                            :toolbar simplemde-toolbar
                                            :spellChecker false
                                            :forceSync true})))
    (-> (sel1 [".CodeMirror" :textarea])
        (dommy/set-attr! :aria-label "CodeMirror textarea"))
    (.value @editor @value)
    (js/setTimeout (fn [] (.value @editor @value)) 200)
    (.codemirror.on @editor "change" (fn [] (reset! value (.value @editor)))))

#_(defn markdown-editor [value]
    (create-class {:component-did-mount (fn []
                                          (init-editor (str "editor" (-> (session/get :user) :id)) value))
                   :reagent-render (fn []
                                     [:div.form-group {:style {:display "block"}}
                                      [:textarea {:class "form-control"
                                                  :id (str "editor" (-> (session/get :user) :id))
                                                  :defaultValue @value
                                                  :name "content"
                                                  :on-change #(reset! value (.-target.value %))
                                                  :cols 12
                                                  :rows 12
                                                  :aria-label "Compose text"}]])
                   :component-did-update (fn [] #(reset! value (.-target.value %)))}))

#_(defn markdown-editor [value]
    (create-class {:component-did-mount (fn []
                                          (init-editor (str "editor" (-> (session/get :user) :id)) value))
                   :reagent-render (fn []
                                     [:div.form-group {:style {:display "block"}}
                                      [:textarea {:class "form-control"
                                                  :id (str "editor" (-> (session/get :user) :id))
                                                  :defaultValue @value
                                                  :on-change #(reset! value (.-target.value %))}]])}))

#_(defn toggle-markdown-editor [element-id value]
    (let [md? (cursor md-editor [:enabled?])
          editor (cursor md-editor [:editor])]
      (if @md?
        (do (reset! md? false) (.toTextArea @editor) (reset! editor nil))
        (do (reset! md? true) (init-editor element-id value)))))

#_(defn toggle-md-button [value]
    (let [md? (cursor md-editor [:enabled?])]
      [:div.pull-right [:span (str (if @md? (t :core/Disablemarkdowneditor) (t :core/Enablemarkdowneditor)) " ")]
       [:button {:type "button"
                 :aria-label "toggle markdown-editor"
                 :class "close evidence-toggle"
                 :on-click #(do (toggle-markdown-editor (str "editor" (-> (session/get :user) :id)) value))}

        [:i.fa.show-more.fa-fw {:class (if @md? (str " fa-toggle-on") (str " fa-toggle-off"))}]]]))

#_(defn process-text [s state]
    (let [element (-> js/document (.getElementById (str "editor" (-> (session/get :user) :id))))
          text (.-innerHTML element)
          endorsement-claim (str text (if (blank? text) "" "\n\n") "* " s)
          editor (cursor md-editor [:editor])
          md? (cursor md-editor [:enabled?])]
      (reset! (cursor state [:endorsement-comment]) endorsement-claim)
      (when @md? (.value @editor  @(cursor state [:endorsement-comment])))
      (when-not @md? (set! (.-value element)  @(cursor state [:endorsement-comment])))))

(defn process-text [s state]
  (let [text (-> js/document
                 (.getElementById (str "editor" (-> (session/get :user) :id)))
                 (.-innerHTML))
        endorsement-claim (str text (if (blank? text) "" "\n\n") "* " s)]
    (reset! (cursor state [:endorsement-comment]) endorsement-claim)
    (.value @editor  @(cursor state [:endorsement-comment]))))

(defn endorse-badge-content [state]
  (fn []
    [:div {:style {:display @(cursor state [:show-content])}}
     [:hr.border]
     [:div.row
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
       [:label {:for (str "editor" (-> (session/get :user) :id))} (str (t :badge/Composeyourendorsement) ":")]
       #_[:div.row.flip {:style {:margin-bottom "5px"}}
          [:label.col-md-6 {:for (str "editor" (-> (session/get :user) :id))} (str (t :badge/Composeyourendorsement) ":")]
          [:div.col-md-6 [toggle-md-button (cursor state [:endorsement-comment])]]]
       [:div [markdown-editor (cursor state [:endorsement-comment]) (str "editor" (-> (session/get :user) :id))]]]
      [:div
       [:button.btn.btn-primary {:on-click #(do
                                              (.preventDefault %)
                                              (save-endorsement state))
                                 :disabled (blank? @(cursor state [:endorsement-comment]))}

        (t :badge/Endorsebadge)]]
      [:hr.border]]]))

(defn endorsement-text [state]
  (let [user-endorsement (->> @(cursor state [:user-badge-endorsements])
                              (filter #(= (:endorser-id @state) (:issuer_id %))))]
    (if (seq user-endorsement)
      (case  (->> user-endorsement first :status)
        "accepted" [:span.label.label-success (t :badge/Youendorsebadge)]
        "declined" [:span.label.label-danger (t :badge/Declinedendorsement)]
        [:span.label.label-info (t :badge/Pendingendorsement)])

      [:span.label.label-info (t :badge/Pendingendorsement)])))

(defn endorse-badge-link [state]
  (fn []
    [:div
     [:a {:href "#"
          :style {:display @(cursor state [:show-link])}
          :on-click #(do
                       (.preventDefault %)
                       (swap! state assoc :show-link "none"
                              :show-content "block"))}
      [:i.fa.fa-thumbs-o-up {:style {:vertical-align "unset"}}] (t :badge/Endorsethisbadge)]
     [:div {:style {:display @(cursor state [:show-endorsement-status])}} [:i.fa.fa-thumbs-up] (endorsement-text state)]]))

(defn profile-link-inline [id issuer_name picture name type]
  [:div [:a {:href "#"
             :on-click #(mo/open-modal [:profile :view] {:user-id id})}
         [:img {:src (profile-picture picture) :alt (str issuer_name " " (t :user/Profilepicture))}]
         (str issuer_name " ")]  (case type
                                   "endorse" (t :badge/Hasendorsedyou)
                                   "request" (str (t :badge/requestsendorsement) " " name)
                                   (t :badge/Hasendorsedyou))])

(defn pending-endorsements []
  (let [state (atom {:user-id (-> (session/get :user) :id) :pending []})]
    (init-pending-endorsements state)
    (fn []
      [:div.endorsebadge
       (reduce (fn [r endorsement]
                 (let [{:keys [id user_badge_id image_file name content profile_picture issuer_id description issuer_name]} endorsement]
                   (conj r
                         [:div
                          [:div.col-md-12
                           [:div.thumbnail
                            [:div.endorser.col-md-12
                             [profile-link-inline issuer_id issuer_name profile_picture nil "endorse"]
                             [:hr.line]]

                            [:div.caption.row.flip
                             [:div.position-relative.badge-image.col-md-3
                              [:img {:src (str "/" image_file) :style {:padding "15px"} :alt ""}]]

                             [:div.col-md-9 [:h1.media-heading name]
                              [:div.thumbnail-description.smaller {:dangerouslySetInnerHTML {:__html content}}]]]
                            [:div.caption.card-footer.text-center
                             [:hr.line]
                             [:button.btn.btn-primary {:href "#"
                                                       :on-click #(do
                                                                    (.preventDefault %)
                                                                    (update-status id "accepted" user_badge_id state init-pending-endorsements))}
                              (t :badge/Acceptendorsement)]
                             [:button.btn.btn-warning.cancel {:href "#"
                                                              :on-click #(do
                                                                           (.preventDefault %)
                                                                           (update-status id "declined" user_badge_id state init-pending-endorsements))} (t :badge/Declineendorsement)]]]]])))
               [:div.row]
               @(cursor state [:pending]))])))

(defn endorse-badge [badge-id & params]
  (let [{:keys [request_id]} (first params)
        state (atom {:id badge-id
                     :show-link "block"
                     :show-content "none"
                     :endorsement-comment ""
                     :endorser-id (-> (session/get :user) :id)
                     :show-endorsement-status "none"
                     :request_id request_id})]
    (init-user-badge-endorsement state)
    (fn []
      [:div#endorsebadge {:style {:margin-top "10px"}}
       [endorse-badge-link state]
       [endorse-badge-content state]])))

(defn endorsement-list [badge-id & params]
  (let [{:keys [data reload-fn dataatom pending-endorsements-atom pending-info-atom]} (first params)
        state (atom {:id badge-id})]
    (init-user-badge-endorsement state)
    (fn []
      (when (seq @(cursor state [:user-badge-endorsements]))
        [:div
         [:div#endorsebadge
          (reduce (fn [r endorsement]
                    (let [{:keys [id user_badge_id image_file name content issuer_name first_name last_name profile_picture issuer_id issuer_name status]} endorsement]
                      (conj r [:div.panel.panel-default.endorsement
                               [:div.panel-heading {:id (str "heading" id)}
                                [:div.panel-title
                                 (if (= "pending" status)  [:span.label.label-info (t :social/Pending)])
                                 [:div.row.flip.settings-endorsement
                                  [:div.col-md-9
                                   (if issuer_id
                                     [:a {:href "#"
                                          :on-click #(mo/open-modal [:profile :view] {:user-id issuer_id})}
                                      [:img.small-image {:src (profile-picture profile_picture) :alt ""}]
                                      issuer_name]
                                     [:div [:img.small-image {:src (profile-picture profile_picture) :alt ""}] issuer_name (if (= "pending" status) (t :badge/Hasendorsedyou))])]]]

                                [:div [:button {:type "button"
                                                :aria-label "OK"
                                                :class "close"
                                                :on-click #(do (.preventDefault %)
                                                               (swap! state assoc :pending-endorsements-atom pending-endorsements-atom :pending-info-atom pending-info-atom :reload-fn reload-fn)
                                                               (delete-endorsement id user_badge_id state init-user-badge-endorsement))}

                                       [:i.fa.fa-trash.trash]]]]
                               [:div.panel-body
                                [:div {:dangerouslySetInnerHTML {:__html content}}]
                                (when (= "pending" status)
                                  [:div.caption
                                   [:hr.border]
                                   [:div.text-center
                                    [:ul.list-inline.buttons.buttons
                                     [:button.btn.btn-primary {:href "#"
                                                               :on-click #(do
                                                                            (.preventDefault %)
                                                                            (swap! state assoc :pending-endorsements-atom pending-endorsements-atom :pending-info-atom pending-info-atom :reload-fn reload-fn)
                                                                            (update-status id "accepted" user_badge_id state init-user-badge-endorsement))}
                                      (t :badge/Acceptendorsement)]
                                     [:button.btn.btn-warning.cancel {:href "#"
                                                                      :on-click #(do
                                                                                   (.preventDefault %)
                                                                                   (swap! state assoc :pending-endorsements-atom pending-endorsements-atom :pending-info-atom pending-info-atom :reload-fn reload-fn)
                                                                                   (update-status id "declined" user_badge_id state init-user-badge-endorsement))}
                                      (t :badge/Declineendorsement)]]]])]])))
                  [:div] @(cursor state [:user-badge-endorsements]))]]))))

#_(defn profile [element-data]
    (let [{:keys [id first_name last_name profile_picture status label issuer_name]} element-data
          current-user (session/get-in [:user :id])]
      [:div.endorsement-profile.panel-default
       (if id
         [:a {:href "#" :on-click #(mo/open-modal [:profile :view] {:user-id id})}
          [:div.panel-body.flip
           [:div.col-md-4
            [:div.profile-image
             [:img.img-responsive.img-thumbnail
              {:src (profile-picture profile_picture)
               :alt (or issuer_name (str first_name " " last_name))}]]]
           [:div.col-md-8
            [:h4 (or issuer_name (str first_name " " last_name))]
            (when (= status "pending") [:p [:span.label.label-info label]])]]]
         [:div.panel-body.flip
          [:div.col-md-4
           [:div.profile-image
            [:img.img-responsive.img-thumbnail
             {:src (profile-picture profile_picture)
              :alt (or issuer_name (str first_name " " last_name))}]]]
          [:div.col-md-8
           [:h4 (or issuer_name (str first_name " " last_name))]
           (when (= status "pending") [:p [:span.label.label-info label]])]])]))

(defn profile [element-data name]
  (let [{:keys [id profile_picture status label issuer_name]} element-data
        current-user (session/get-in [:user :id])]
    [:div.endorsement-profile.panel-default
     (if id
       [:a {:href "#" :on-click #(mo/open-modal [:profile :view] {:user-id id})}
        [:div.panel-body.fliprequest
         [:div.col-md-4
          [:div.profile-image
           [:img.img-responsive.img-thumbnail
            {:src (profile-picture profile_picture)
             :alt (str (t :user/Profilepicture) " " name)
             :title name}]]]
         [:div.col-md-8
          [:span.profileowner name]
          (when (= status "pending") [:p [:span.label.label-info label]])]]]
       [:div.panel-body.flip
        [:div.col-md-4
         [:div.profile-image
          [:img.img-responsive.img-thumbnail
           {:src (profile-picture profile_picture)
            :alt name
            :title name}]]]
        [:div.col-md-8
         [:span.profileowner name]
         (when (= status "pending") [:p [:span.label.label-info label]])]])]))

(defn- toggle-delete-dialogue [state]
  (let [show-delete-dialogue (cursor state [:show-delete-dialogue])]
    (if @show-delete-dialogue
      (reset! show-delete-dialogue false)
      (reset! show-delete-dialogue true))))

(defn- confirm-delete [state delete-fn]
  (when (and state @(cursor state [:show-delete-dialogue]))
    [:div.confirm-delete-block
     [:div {:class "alert alert-warning"}
      (t :badge/Confirmdelete)]
     [:div
      [:button {:type         "button"
                :class        "btn btn-warning"
                :data-dismiss "modal"
                :on-click     #(do
                                 (.preventDefault %)
                                 (delete-fn))}
       (t :badge/Delete)]
      [:button {:type     "button"
                :class    "btn btn-primary"
                :on-click #(do
                             (.preventDefault %)
                             (swap! state assoc :show-delete-dialogue false))}
       (t :badge/Cancel)]]]))

(defn user-endorsement-content [params]
  (fn []
    (let [{:keys [endorsement state]} @params
          {:keys [id profile_picture name first_name last_name image_file content user_badge_id issuer_id issuer_name endorsee_id status type requester_id description issued_on issuer_content_id requestee_id]} endorsement
          profile-name (cond
                         requester_id (str first_name " " last_name)
                         requestee_id issuer_name
                         endorsee_id (str first_name " " last_name)
                         issuer_id issuer_name
                         :else "")]

      [:div.row.flip {:id "badge-info"}
       [:div.col-md-3
        [:div.badge-image [:img.badge-image {:src (str "/" image_file) :alt name}]]]
       [:div.col-md-9
        [:div
         [:h1.uppercase-header name]
         (when (and description issuer_content_id issuer_name)
           [:div;#badge-info
            [:div.badge-info
             [:div {:class "issuer-data clearfix"}
              [:span._label {:class "pull-label-left"}  (t :badge/Issuedby) ":"]
              [:div {:class "issuer-links pull-label-left inline"}
               [:a {:href "#"
                    :on-click #(do (.preventDefault %)
                                   (mo/open-modal [:badge :issuer] issuer_content_id {}))} issuer_name]]]
             (when (and issued_on (pos? issued_on))
               [:div [:span._label (t :badge/Issuedon) ": "]  (date-from-unix-time (* 1000 issued_on))])
             [:div.description description]]])
         [:div (cond
                 requester_id ""
                 requestee_id (t :badge/Managesentendorsementrequest)
                 endorsee_id (t :badge/Manageendorsementtext1)
                 issuer_id (t :badge/Manageendorsementtext2)
                 :else "")]

         [:hr.line]
         [:div.row
          [:div.col-md-4.col-md-push-8  " "]
          [:div.col-md-8.col-md-pull-4 [profile {:id (or endorsee_id issuer_id requester_id requestee_id)
                                                 :profile_picture profile_picture
                                                 :issuer_name issuer_name
                                                 :status status
                                                 :label (t :social/pending)} profile-name]]]

         (cond
           endorsee_id  [:div {:style {:margin-top "15px"}}
                         [:div
                          [:label {:for (str "editor" (-> (session/get :user) :id))} (str (t :badge/Composeyourendorsement) ":")]
                          #_[:div.row.flip {:style {:margin-bottom "5px"}}
                             [:span._label.col-md-6 {:for "claim"} (str (t :badge/Composeyourendorsement) ":")]
                             [:div.col-md-6 [toggle-md-button (cursor params [:endorsement :content])]]]
                          [:div.editor [markdown-editor (cursor params [:endorsement :content])]]]
                         [:div.row.flip.control-buttons
                          (if-not @(cursor state [:show-delete-dialogue])
                            [:div.col-md-6.col-sm-6.col-xs-6.left-buttons
                             [:button.btn.btn-primary {:on-click #(do
                                                                    (.preventDefault %)
                                                                    (edit-endorsement id user_badge_id @(cursor params [:endorsement :content])))
                                                       :disabled (blank? @(cursor params [:endorsement :content]))
                                                       :data-dismiss "modal"}

                              (t :core/Save)]
                             [:button.btn.btn-warning.cancel {:data-dismiss "modal"} (t :core/Cancel)]])
                          [:div.col-md-6.col-sm-6.col-xs-6.left-buttons ""]
                          [:div.col-md-6.col-sm-6.col-xs-6.left-buttons
                           [:a.delete-btn {:style {:line-height "4" :cursor "pointer"}
                                           :on-click #(do
                                                        (.preventDefault %)
                                                        (toggle-delete-dialogue state))}
                            [:i.fa.fa-trash] (t :badge/Deleteendorsement)]]]
                         [confirm-delete state #(delete-endorsement id user_badge_id nil nil)]]

           requester_id [:div {:style {:margin-top "15px"}}
                         [:div {:dangerouslySetInnerHTML {:__html content}}]
                         [:div.caption
                          [:hr.line]
                          [endorse-badge user_badge_id {:request_id id}]
                          (when (= status "pending")
                            [:div.endorsebadge {:style {:margin "25px 0"}} [:a {:href "#"
                                                                                :on-click #(do
                                                                                             (.preventDefault %)
                                                                                             (toggle-delete-dialogue state)
                                                                                             #_(update-request-status! id "declined" state nil))}
                                                                            [:span [:i.fa.fa-trash] (t :badge/Deleteendorsementrequest)]]
                             [info {:content (t :badge/Declinerequestinfo) :placement "right" :style {:margin "0 10px"}}]])
                          [confirm-delete state #(update-request-status! id "declined" state nil)]]]
           requestee_id [:div {:style {:margin-top "15px"}}
                         [:div {:dangerouslySetInnerHTML {:__html content}}]
                         [:div.caption
                          [:hr.line]
                          [:a {:href "#"
                               :on-click #(do
                                            (.preventDefault %)
                                            (toggle-delete-dialogue state))}
                           [:span [:i.fa.fa-trash] (t :badge/Deleteendorsementrequest)]]
                          [confirm-delete state #(update-request-status! id "declined" state nil)]]]

           issuer_id  [:div {:style {:margin-top "15px"}}
                       [:div {:dangerouslySetInnerHTML {:__html content}}]

                       [:div.caption
                        [:hr.line]
                        (if (= "pending" status)
                          [:div.buttons
                           [:button.btn.btn-primary {:href "#"
                                                     :on-click #(do
                                                                  (.preventDefault %)
                                                                  (update-status id "accepted" user_badge_id state nil #_init-pending-endorsements))

                                                     :data-dismiss "modal"}  (t :badge/Acceptendorsement)]
                           [:button.btn.btn-warning.cancel {:href "#"
                                                            :on-click #(do
                                                                         (.preventDefault %)
                                                                         (update-status id "declined" user_badge_id state nil #_init-pending-endorsements))
                                                            :data-dismiss "modal"} (t :badge/Declineendorsement)]]
                          [:div.row.flip.control-buttons
                           (if-not @(cursor state [:show-delete-dialogue])
                             [:div.col-md-6.col-sm-6.col-xs-6  [:button.btn.btn-primary.cancel {:data-dismiss "modal"} (t :core/Cancel)]]
                             [:div.col-md-6.col-sm-6.col-xs-6  ""])
                           [:div.col-md-6.col-sm-6.col-xs-6 [:a.delete-btn {:style {:line-height "4" :cursor "pointer"}
                                                                            :on-click #(do
                                                                                         (.preventDefault %)

                                                                                         (toggle-delete-dialogue state)
                                                                                         #_(delete-endorsement id user_badge_id nil nil))
                                                                            :href "#"}
                                                             [:i.fa.fa-trash] (t :badge/Deleteendorsement)]]])
                        [confirm-delete state #(delete-endorsement id user_badge_id nil nil)]]]

           :else [:div])]]])))

(defn endorsements [state]
  (let [endorsements (case @(cursor state [:show])
                       "all" @(cursor state [:all-endorsements])
                       "given" @(cursor state [:given])
                       "received" @(cursor state [:received])
                       "requests" @(cursor state [:requests])
                       "sent-requests" @(cursor state [:sent-requests])
                       @(cursor state [:all-endorsements]))
        processed-endorsements (if (blank? @(cursor state [:search]))
                                 endorsements
                                 (filter #(or (re-find (re-pattern (str "(?i)" @(cursor state [:search]))) (:name %))
                                              (re-find (re-pattern (str "(?i)" @(cursor state [:search]))) (get % :first_name ""))
                                              (re-find (re-pattern (str "(?i)" @(cursor state [:search]))) (get % :last_name ""))
                                              (re-find (re-pattern (str "(?i)" @(cursor state [:search]))) (get % :issuer_name ""))
                                              (re-find (re-pattern (str "(?i)" @(cursor state [:search]))) (str (:first_name %) " " (:last_name %))))
                                         endorsements))
        order (keyword  @(cursor state [:order]))
        endorsements (case order
                       (:mtime) (sort-by order > processed-endorsements)
                       (:name) (sort-by (comp clojure.string/upper-case str order) processed-endorsements)
                       (:user) (sort-by #(str (:first_name %) " " (:last_name %)) processed-endorsements)
                       (:pending) (sort-by #(:status %) > processed-endorsements)
                       processed-endorsements)]
    [:div.panel
     [:div.panel-heading
      [:h2
       (str (t :badge/endorsements))]
      [:br]
      [:div (case @(cursor state [:show])
              "all" (t :badge/Allendorsementstext)
              "given" (t :badge/Givenendorsementstext)
              "received" (t :badge/Receivedendorsementstext)
              "requests" (t :badge/Endorsementrequesttext)
              "sent-requests" (t :badge/Sentendorsementrequesttext)
              (t :badge/Allendorsementstext))]]

     [:div.panel-body.endorsement-container
      [:div.table.endorsementlist  {:summary (t :badge/Endorsements)}
       (reduce (fn [r endorsement]
                 (let [{:keys [id endorsee_id issuer_id requester_id profile_picture issuer_name first_name last_name name image_file content status user_badge_id mtime type]} endorsement
                       endorser (or issuer_name (str first_name " " last_name))]
                   (conj r [:div.list-item.row.flip
                            [:a {:href "#" :on-click #(do
                                                        (.preventDefault %)
                                                        (mo/open-modal [:badge :userendorsement] (atom {:endorsement endorsement :state state}) {:hidden (fn [] (init-user-endorsements state))}))}

                             [:div.col-md-4.col-md-push-8
                              [:small.pull-right [:i (date-from-unix-time (* 1000 mtime) "days")]]]
                             [:div.col-md-8.col-md-pull-4
                              [:div.media
                               [:div;.row
                                [:div.labels
                                 (cond
                                   (= type "request") [:span.label.label-danger (t :badge/Endorsementrequest)]
                                   (= type "sent_request") [:span.label.label-info (t :badge/Endorsementrequest)]
                                   issuer_id [:span.label.label-success (t :badge/Endorsedyou)]
                                   endorsee_id [:span.label.label-primary (t :badge/Youendorsed)]
                                   :else [:span.label.label-success (t :badge/Endorsedyou)])
                                 (if (and (not= "sent_request" type) (= "pending" status))
                                   [:span.label.label-info
                                    (t :social/pending)])]]
                               [:div.media-left.media-top.list-item-bodyv
                                [:img.main-img.media-object {:src (str "/" image_file) :alt ""}]]

                               [:div.media-body
                                [:h3.media-heading.badge-name  name]
                                [:div.media
                                 [:div.child-profile [:div.media-left.media-top
                                                      [:img.media-object.small-img {:src (profile-picture profile_picture) :alt ""}]]
                                  [:div.media-body
                                   [:p endorser]]]]]]]]])))
               [:div] endorsements)]]]))

(defn order-opts []
  [{:value "mtime" :id "radio-date" :label (t :badge/bydate)}
   {:value "name" :id "radio-name" :label (t :core/byname)}
   {:value "user" :id "radio-issuer" :label (t :badge/byuser)}
   {:value "pending" :id "pending" :label (t :social/pending)}])

(defn user-endorsements-content [state]
  (let [pending-requests-count (count (filter #(= "pending" (:status %)) @(cursor state [:requests])))
        pending-received-count (count (filter #(= "pending" (:status %)) @(cursor state [:received])))
        sent-requests-count (count @(cursor state [:sent-requests]))]
    [:div
     [m/modal-window]
     [:div#badge-stats
      (if (or (seq @(cursor state [:received])) (seq @(cursor state [:given])) (pos? pending-requests-count) (pos? sent-requests-count))
        [:div
         [:div.form-horizontal {:id "grid-filter"}
          [g/grid-search-field (t :core/Search ":")  "endorsementsearch" (t :badge/Filterbybadgenameoruser) :search state]
          [:div.form-group.wishlist-buttons
           [:legend {:class "control-label col-sm-2"} (str (t :core/Show) ":")]
           [:div.col-md-10
            [:div.buttons
             [:button {:class (str "btn btn-default " (when (= "all" @(cursor state [:show])) "btn-active"))
                       :id "btn-all"
                       :on-click #(do (swap! state assoc :show "all"))}
              (t :core/All) #_(when (or (pos? pending-received-count) (pos? pending-requests-count)) [:span.badge.endorsement-notification (+ pending-requests-count pending-received-count)])]
             [:button {:class (str "btn btn-default " (when (= "received" @(cursor state [:show])) "btn-active"))
                       :id "btn-endorsedme"
                       :on-click #(do (swap! state assoc :show "received"))}
              (t :badge/Endorsedme) (when (pos? pending-received-count) [:span.badge.endorsement-notification pending-received-count])]
             [:button {:class (str "btn btn-default " (when (= "given" @(cursor state [:show])) "btn-active"))
                       :id "btn-iendorsed"
                       :on-click #(do (swap! state assoc :show "given"))}
              (t :badge/Iendorsed)]
             (when (pos? pending-requests-count)  [:button {:class (str "btn btn-default " (when (= "requests" @(cursor state [:show])) "btn-active"))
                                                            :id "btn-pendingrequests"
                                                            :on-click #(do (swap! state assoc :show "requests"))}
                                                   [:span (t :badge/Endorsementrequests) [:span.badge.endorsement-notification pending-requests-count]]])

             (when (pos? sent-requests-count) [:button {:class (str "btn btn-default " (when (= "sent-requests" @(cursor state [:show])) "btn-active"))
                                                        :id "btn-sentrequests"
                                                        :on-click #(do (swap! state assoc :show "sent-requests"))}
                                               [:span (t :badge/Sentendorsementrequests) [:span.badge.endorsement-notification sent-requests-count]]])]]]

          [g/grid-radio-buttons (t :core/Order ":") "order" (order-opts) :order state]]

         (endorsements state)]
        [:div (t :badge/Youhavenoendorsements)])]]))

(defn- reset-request! [state]
  (swap! state assoc :request-mode false :selected-users [] :request-comment " " :resp-message false))

(defn send-endorsement-request [state reload-fn]
  (let [request-comment (cursor state [:request-comment])
        selected-users (cursor state [:selected-users])]
    (ajax/POST
     (path-for (str "/obpv1/badge/user_endorsement/request/" (:id @state)) true)
     {:params {:user-ids (mapv :id @selected-users)
               :content @request-comment}
      :handler (fn [data]
                 (when (= "success" (:status data))
                   (reload-fn)
                   (swap! state assoc :resp-message true)
                   (js/setTimeout #(reset-request! state) 2000)))
      :finally (fn [] (mo/previous-view))})))

(defn request-endorsement [params]
  (let [{:keys [state reload-fn context]} params
        request-comment (cursor state [:request-comment])
        context (if (blank? context) "endorsement" context)
        selected-users (if (blank? context) (cursor state [:selected-users]) (cursor state [:send_request_to]))]
    (reset! request-comment (t :badge/Defaultrequestbadge))
    (fn []
      [:div.col-md-12 {:id "social-tab"}
       [:div.editor
        [:div.form-group {:style {:display "block"}}
         [:label {:for (str "editor" (-> (session/get :user) :id)) #_"claim"} [:b (str (t :badge/Composeyourendorsementrequest) ":")]]
         [:div.editor  [markdown-editor request-comment (str "editor" (-> (session/get :user) :id))]]]
        (when (seq @selected-users)
          [:div {:style {:margin "20px 0"}} [:i.fa.fa-users.fa-fw.fa-3x]
           [:a {:href "#"
                :on-click #(mo/open-modal [:gallery :profiles] {:type "pickable" :selected-users-atom selected-users :context context :user_badge_id (:id @state)})}
            (t :badge/Editselectedusers)]])
        (reduce (fn [r u]
                  (let [{:keys [id first_name last_name profile_picture]} u]
                    (conj r [:div.user-item [profile-link-inline-modal id first_name last_name profile_picture]
                             [:a {:href "#" :on-click (fn [] (reset! selected-users (->> @selected-users (remove #(= id (:id %))) vec)))}
                              [:span.close {:aria-hidden "true" :dangerouslySetInnerHTML {:__html "&times;"}}]]])))
                [:div.selected-users-container] @selected-users)
        [:div.confirmusers {:style {:margin "20px auto"}}
         (if-not (empty? @selected-users)
           [:button.btn.btn-primary {:on-click #(do
                                                  (.preventDefault %)
                                                  (if (= context "endorsement_selfie")
                                                    (mo/previous-view)
                                                    (send-endorsement-request state reload-fn)))
                                     :disabled (empty? @selected-users)}
            (if (= context "endorsement_selfie") (t :badge/Continue) (t :badge/Sendrequest))]
           [:button.btn.btn-primary.select-users-link {:href "#"
                                                       :on-click #(mo/open-modal [:gallery :profiles] {:type "pickable" :selected-users-atom selected-users :context context :user_badge_id (:id @state)})
                                                       :disabled (< (count @request-comment) 15)}
            [:i.fa.fa-users.fa-fw.fa-3x] (t :badge/Selectusers)])]]])))

(defn pending-endorsement-requests []
  (when (session/get-in [:user :id])
    (let [state (atom {:user-id (session/get-in [:user :id])})]
      (init-pending-requests state)
      (fn []
        (when (seq @state)
          ^{:key @state} [:div.endorsebadge
                          (reduce
                           (fn [r request]
                             (let [{:keys [id user_badge_id content status mtime requester_id profile_picture first_name last_name name image_file description issuer_id issuer_name issued_on]} request]
                               (conj r
                                     [:div
                                      [:div.col-md-12
                                       [:div.thumbnail
                                        [:div.endorser.col-md-12
                                         [profile-link-inline requester_id (str first_name " " last_name) profile_picture name "request"]
                                         [:hr.line]]
                                        [:div.caption.row.flip
                                         [:div.position-relative.badge-image.col-md-3
                                          [:img {:src (str "/" image_file) :style {:padding "15px"} :alt (str (t :badge/Badge) " " name)}]]

                                         [:div.col-md-9
                                          [:h1.media-heading name]
                                          [:div#badge-info
                                           [:div.badge-info
                                            [:div {:class "issuer-data clearfix"}
                                             [:span._label {:class "pull-label-left"}  (t :badge/Issuedby) ":"]
                                             [:div {:id "issuer_name" :class "issuer-links pull-label-left inline"}
                                              [:a {:href "#"
                                                   :on-click #(do (.preventDefault %)
                                                                  (mo/open-modal [:badge :issuer] issuer_id {}))} issuer_name]]]
                                            (when (and issued_on (pos? issued_on))
                                              [:div [:span._label (t :badge/Issuedon) ": "]  (date-from-unix-time (* 1000 issued_on))])
                                            [:div.description description]]
                                           [:div {:style {:margin "15px auto"}} [:i.fa.fa-hand-o-down {:style {:margin-bottom "10px"}}]
                                            [:div.thumbnail-description.smaller {:display "inline-block" :dangerouslySetInnerHTML {:__html content}}]]]]]
                                        [:div.caption.card-footer.text-center
                                         [:hr.line]
                                         [:button.btn-primary.btn {:href "#" :on-click #(do
                                                                                          (.preventDefault %)
                                                                                          (mo/open-modal [:badge :userendorsement] (atom {:endorsement request :state state}) {:hidden (fn [] (init-pending-requests state))}))}
                                          (t :badge/Endorsebadge)]
                                         [:span [:button.btn.btn-warning.cancel {:href "#"
                                                                                 :on-click #(do
                                                                                              (.preventDefault %)
                                                                                              (update-request-status! id "declined" state (fn [] (init-pending-requests state))))
                                                                                   ;(init-pending-requests state))

                                                                                 :data-dismiss "modal"}
                                                 (t :badge/Deleteendorsementrequest)]
                                          [info {:content (t :badge/Declinerequestinfo) :placement "right" :style {:margin "0 10px"}}]]]]]]))) [:div.row]
                           @state)])))))

(defn handler [site-navi]
  (let [state (atom {:initializing true
                     :permission "initial"
                     :order :mtime})
        user (session/get :user)]
    (init-user-endorsements state)
    (fn []
      (cond
        (= "initial" (:permission @state)) [:div]
        (and user (= "error" (:permission @state))) (layout/default-no-sidebar site-navi (err/error-content))
        (= "error" (:permission @state)) (layout/landing-page site-navi (err/error-content))
        :else (layout/default site-navi (user-endorsements-content state))))))
