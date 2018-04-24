(ns salava.page.ui.view
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [ajax.core :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.helper :refer [navigate-to path-for private? js-navigate-to]]
            [salava.page.ui.helper :as ph]
            [salava.core.ui.share :as s]
            [reagent-modals.modals :as m]
            [salava.core.ui.error :as err]
            [salava.admin.ui.admintool :refer [admintool]]
            [salava.admin.ui.reporttool :refer [reporttool1]]
            [salava.core.helper :refer [dump]]
            ))

(defn check-password [password-atom page-id state]
  (ajax/POST
    (path-for (str "/obpv1/page/password/" page-id))
    {:response-format :json
     :keywords? true
     :params {:password @password-atom}
     :handler (fn [data]
                (swap! state assoc :page data)
                (swap! state assoc :ask-password false))
     :error-handler (fn [{:keys [status status-text]}]
                      (if (= status 401)
                        (swap! state assoc :password-error true)))}))

(defn toggle-visibility [page-id visibility-atom]
  (ajax/POST
    (path-for (str "/obpv1/page/toggle_visibility/" page-id))
    {:response-format :json
     :keywords? true
     :params {:visibility (if (= "private" @visibility-atom) "public" "private")}
     :handler (fn [data]
                (reset! visibility-atom data))
     :error-handler (fn [{:keys [status status-text]}]
                      (if (= status 401)
                          (navigate-to "/user/login")))}))

(defn page-password-field [state]
  (let [password-atom (cursor state [:password])]
    [:div {:id "page-password"}
     (if (:password-error @state)
       [:div {:class "alert alert-warning"}
        (t :page/Passwordincorrect)])
     [:div.row
      [:div.col-md-12
       [:h2 (t :page/Passwordrequired)]]]
     [:div.form-group
      [:input {:class "form-control"
               :type "text"
               :on-change #(reset! password-atom (.-target.value %))
               :value @password-atom
               :placeholder (t :page/Enterpassword)}]]
     [:div.form-group
      [:button {:class "btn btn-primary"
                :on-click #(check-password password-atom (:page-id @state) state)}
       (t :core/Submit)]]]))

(defn export-page-to-pdf [state]
  (let [id (:page-id @state)
        user-id  (:id (session/get :user))]
  (ajax/GET
    (path-for (str "obpv1/page/export-to-pdf/" id "/"user-id))
     {:handler (js-navigate-to (str "obpv1/page/export-to-pdf/" id"/"user-id))
                                                      })))

(defn page-content [page state]
  (let [show-link-or-embed-atom (cursor state [:show-link-or-embed-code])
        visibility-atom (cursor state [:page :visibility])]
    [:div {:id "page-view"}
     [m/modal-window]
     (if (:owner? page)
       [:div {:id "page-buttons-share"}
        [:div {:id "buttons"
               :class "text-right"}
         [:a {:class "btn btn-primary edit-btn"
              :href  (path-for (str "/page/edit/" (:id page)))}
          (t :page/Edit)]
         [:button {:class "btn btn-primary print-btn"
                   :on-click #(.print js/window)}
          (t :core/Print)]
         [:button {:class "btn btn-primary"
                   :on-click #(export-page-to-pdf state)}
          (t :badge/Exporttopdf)]]
        (if-not (private?)
          [:div {:class (str "checkbox " @visibility-atom)}
           [:label
            [:input {:name      "visibility"
                     :type      "checkbox"
                     :on-change #(toggle-visibility (:id page) visibility-atom)
                     :checked   (= @visibility-atom "public")}]
            [:i.fa]
            (if (= @visibility-atom "public")
              (t :page/Public)
              (t :core/Publishandshare))]])
        [:div {:class (str "share-wrapper " @visibility-atom) } [s/share-buttons (str (session/get :site-url) (path-for "/page/view/") (:id page)) (:name page) (= "public" (:visibility page)) false show-link-or-embed-atom]]]
       (admintool (:id page) "page"))
     [ph/view-page page]
     (if (:owner? page) "" (reporttool1 (:id page)  (:name page) "page"))
     ]))

(defn content [state]
  (let [page (:page @state)]
    (if (:ask-password @state)
      [page-password-field state]
      [:div {:id "page-container"}
       [page-content page state]])
    ))

(defn init-data [state id]
  (ajax/GET
    (path-for (str "/obpv1/page/view/" id))
    {:response-format :json
     :keywords?       true
     :handler         (fn [data]
                        (swap! state assoc :page (:page data) :ask-password (:ask-password data) :permission "success"))
     :error-handler   (fn [{:keys [status status-text]}]
                        (if (= status 401)
                          (navigate-to "/user/login")
                          (swap! state assoc :permission "error")))}
    ))

(defn handler [site-navi params]
  (let [id (:page-id params)
        state (atom {:page {}
                     :permission "initial"
                     :page-id id
                     :ask-password false
                     :password ""
                     :password-error false
                     :show-link-or-embed-code nil})
        user (session/get :user)]
    (init-data state id)
    (fn []
      (cond
        (= "initial" (:permission @state)) [:div]
        (and user (= "error" (:permission @state))) (layout/default-no-sidebar site-navi (err/error-content))
        (= "error" (:permission @state)) (layout/landing-page site-navi (err/error-content))
        (and user (= (get-in @state [:page :user_id]) (:id user))) (layout/default site-navi (content state))
        (and (= "success" (:permission @state)) user) (layout/default-no-sidebar site-navi (content state))
        :else (layout/landing-page site-navi (content state))))))
