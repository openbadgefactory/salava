(ns salava.user.ui.external
 (:require
  [clojure.string :refer [blank?]]
  [reagent.core :refer [atom cursor]]
  [salava.core.i18n :refer [t]]
  [salava.core.time :refer [date-from-unix-time]]
  [salava.core.ui.ajax-utils :as ajax]
  [salava.core.ui.helper :refer [path-for disable-background-image navigate-to]]
  [salava.core.ui.layout :as layout]
  [salava.core.ui.modal :as mo]
  [salava.user.ui.helper :refer [profile-picture]]))

(defn init-endorsements [id state]
  (ajax/POST
   (path-for (str "obpv1/badge/user_endorsement/ext/all/" id))
   {:handler (fn [data]
               (swap! state assoc :ext-endorsement data))}

   (ajax/POST
      (path-for (str "obpv1/badge/user_endorsement/ext_request/all/" id))
      {:handler (fn [data]
                  (swap! state assoc :request data))})))

(defn init-data [id state]
 (ajax/GET
  (path-for (str "obpv1/user/external/data/" id))
  {:handler (fn [data]
              (swap! state assoc :ext-user data)
              (init-endorsements id state))}))

(defn endorsement-list [ext_id data heading type]
  (when (seq data)
    [:div.col-md-12
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
 (let [{:keys [id ext_id image_file name email description url]} @(cursor state [:ext-user])
       data (->> (concat))]
   [:div.well.well-lg
    [:div.row.flip {:id "badge-stats"}
     [:div.col-md-12
      [:h1.uppercase-header (t :user/Mydata)]
      [:p {:style {:margin-bottom "25px"}} [:b "This page contains a summary of your personal data that is tied to the email address below."]]
      [:div.col-md-3.text-center
       [:div.profile-picture-wrapper
        [:img.profile-picture
         {:src (profile-picture image_file)
          :alt (or name "external user")}]]]
      [:div.col-md-9.col-sm-9.col-xs-9 {:style {:line-height "2.5"}}
       [:div [:span._label (str "ID" ": ")] ext_id]
       (when-not (blank? name) [:div [:span._label (str (t :badge/name) ": ")] name])
       [:div [:span._label (str (t :badge/Email) ": ")] email]
       (when-not (blank? url) [:div [:span._label (str (t :badge/URL) ": ")] url])]
       ;(when-not (blank? description) [:div [:span._label (str "decription" ": ") description]])]
      [:div.col-md-12 {:style {:margin "10px auto"}}
       [:hr.border]]
      [endorsement-list ext_id @(cursor state [:ext-endorsement]) (t :badge/endorsement) nil]
      [endorsement-list ext_id @(cursor state [:request]) (t :badge/Endorsementrequests) "request"]]]]))


(defn handler [site-navi params]
 (let [id (:id params)
       state (atom {:ext_id id})]
   (init-data id state)
   (disable-background-image)
   (fn []
     (layout/landing-page site-navi (content state)))))
