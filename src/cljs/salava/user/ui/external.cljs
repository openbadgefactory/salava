(ns salava.user.ui.external
 (:require
  [clojure.string :refer [blank?]]
  [reagent.core :refer [atom cursor]]
  [reagent-modals.modals :as m]
  [reagent.session :as session]
  [salava.core.i18n :refer [t]]
  [salava.core.time :refer [date-from-unix-time]]
  [salava.core.ui.ajax-utils :as ajax]
  [salava.core.ui.helper :refer [path-for disable-background-image navigate-to js-navigate-to]]
  [salava.core.ui.layout :as layout]
  [salava.core.ui.modal :as mo]
  [salava.user.ui.helper :refer [profile-picture]]))

(defn delete-information [state]
  (ajax/DELETE
   (path-for (str "obpv1/user/external/" @(cursor state [:ext-user :ext_id])))
   {:handler (fn [data]
               (when (= "success" data)
                 (reset! (cursor state [:message]) [:div.alert.alert-success "Your data was successfully deleted!"])))}))

(defn init-endorsements [id state]
  (ajax/POST
   (path-for (str "obpv1/badge/user_endorsement/ext/all/" id) true)
   {:handler (fn [data]
               (swap! state assoc :ext-endorsement data))}

   (ajax/POST
      (path-for (str "obpv1/badge/user_endorsement/ext_request/all/" id) true)
      {:handler (fn [data]
                  (swap! state assoc :request data))})))

(defn init-data [id state]
 (ajax/GET
  (path-for (str "obpv1/user/external/data/" id) true)
  {:handler (fn [data]
              (swap! state assoc :ext-user data)
              (init-endorsements id state))}))

(defn export-data [state]
 (js-navigate-to (str "/obpv1/user/external/data/export/" (session/get-in [:user :language] "en")"/" @(cursor state [:ext-user :ext_id]))))

(defn language-switcher []
 (let [current-lang (session/get-in [:user :language] "en")
       languages (session/get :languages)]
    [:div.text-center
     (doall
      (map (fn [lang]
             ^{:key lang} [:a {:style (if (= current-lang lang) {:font-weight "bold" :text-decoration "underline"} {})
                               :href "#"
                               :on-click #(session/assoc-in! [:user :language] lang)}
                           (clojure.string/upper-case lang) " "])
           languages))]))

(defn delete-modal [state]
  [:div
   [:div.modal-header
    [:button {:type "button"
              :class "close"
              :data-dismiss "modal"
              :aria-label "OK"}
     [:span {:aria-hidden "true"
             :dangerouslySetInnerHTML {:__html "&times;"}}]]]
   ; [:h4.modal-title (translate-text message)]]
   [:div.modal-body
    [:div.alert.alert-warning
     (t :badge/Confirmdelete)]]
   [:div.modal-footer.btn-toolbar
    [:div.btn-group
     [:button.btn.btn-danger
       {:type "button" :data-dismiss "modal" :on-click #(delete-information state)}
       (t :core/Delete)]
     [:button {:type "button"
                :class "btn btn-primary"
                :data-dismiss "modal"}
       (t :core/Cancel)]]]])

(defn endorsement-list [ext_id data heading type]
  (when (seq data)
    [:div.col-md-12
     [:hr.border]
     ;[:h2.uppercase-header heading]
     [:div.panel.panel-default
      [:div.panel.panel-heading
       [:h2.uppercase-header heading]]

      [:div.panel-body.endorsement-container
       [:div.table.endorsementlist  {:summary (t :badge/Endorsements)}
         (reduce (fn [r endorsement]
                     (let [{:keys [id endorsee_id issuer_id requester_id profile_picture issuer_name first_name last_name name image_file content status user_badge_id mtime type]} endorsement
                           endorser (or issuer_name (str first_name " " last_name))]
                       (conj r [:div.list-item.row.flip
                                [:a {:href (path-for (str "/badge/info/" user_badge_id "?endorser=" ext_id)) :target "_blank"} ;:on-click #(do)
                                                                             ;(.preventDefault %)
                                                                             ;(navigate-to (str "/badge/info/" user_badge_id "?endorser=" ext_id))}

                                 [:div.col-md-4.col-md-push-8
                                  [:small.pull-right [:i (date-from-unix-time (* 1000 mtime) "days")]]]
                                 [:div.col-md-8.col-md-pull-4
                                  [:div.media
                                   [:div;.row
                                    [:div.labels
                                     (if (= type "request") [:span.label.label-danger (t :badge/Endorsementrequest)] [:span.label.label-primary (t :badge/Youendorsed)])
                                     (if (and (not= "sent_request" type) (= "pending" status))
                                       [:span.label.label-info
                                        (t :social/pending)])]]
                                   [:div.media-left.media-top.list-item-body
                                    [:img.main-img.media-object {:src (str "/" image_file) :alt ""}]]

                                   [:div.media-body
                                    [:h3.media-heading.badge-name  name]
                                    [:div.media
                                     [:div.child-profile [:div.media-left.media-top
                                                          [:img.media-object.small-img {:src (profile-picture profile_picture) :alt ""}]]
                                      [:div.media-body
                                       [:p endorser]]]]]]]]])))
                 [:div] data)]]]]))

(defn content [state]
 (let [{:keys [id ext_id image_file name email description url]} @(cursor state [:ext-user])]
  [:div
   [m/modal-window]
   [:div.col-md-12 {:style {:margin "20px auto"}}
    [language-switcher]]
   (if (empty? @(cursor state [:ext-user]))
     [:div.col-md-12
      [:b "404 not found"]]
     (if @(cursor state [:message])
       [:div @(cursor state [:message])]
       [:div.well.well-lg
        [:div.row.flip {:id "badge-stats"}
         [:div.col-md-12
          [:div.btn-toolbar.pull-right
           [:div.btn-group
            [:button.btn.btn-danger
             {:type "button"
              :on-click #(m/modal! [delete-modal state] {})}
             (t :core/Delete)]
            [:button.btn.btn-primary
             {:type "button"
              :on-click #(export-data state)}
             (t :user/Exportdata)]]]

          [:h1.uppercase-header (t :user/Mydata)]
          [:p {:style {:margin-bottom "25px"}} [:b "This page contains a summary of your personal data that is tied to the email address below."]]
          [:div.col-md-3.text-center
           [:div.profile-picture-wrapper
            [:img.profile-picture
             {:src (profile-picture image_file)
              :alt (or name "external user")}]]]
          [:div.col-md-9 {:style {:line-height "2.5"}}
           [:div [:span._label (str "ID" ": ")] ext_id]
           (when-not (blank? name) [:div [:span._label (str (t :badge/Name) ": ")] name])
           [:div [:span._label (str (t :badge/Email) ": ")] email]
           (when-not (blank? url) [:div [:span._label (str (t :badge/URL) ": ")] url])]

          [endorsement-list ext_id @(cursor state [:ext-endorsement]) (t :badge/endorsement) nil]
          [endorsement-list ext_id @(cursor state [:request]) (t :badge/Endorsementrequests) "request"]]]]))]))


(defn handler [site-navi params]
 (let [id (:id params)
       state (atom {:ext_id id :message nil})]
   (init-data id state)
   (disable-background-image)
   (fn []
     (layout/landing-page site-navi (content state)))))
