(ns salava.page.ui.view
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [ajax.core :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.helper :refer [navigate-to path-for]]
            [salava.page.ui.helper :as ph]
            [salava.core.ui.share :as s]
            [salava.admin.ui.admintool :refer [private-this-page]]))

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

(defn page-content [page state]
  (let [show-link-or-embed-atom (cursor state [:show-link-or-embed-code])
        visibility-atom (cursor state [:page :visibility])]
    [:div {:id "page-view"}
     
     (if (:owner? page)
       [:div {:id "page-buttons-share"}
        [:div {:id "buttons"
               :class "text-right"}
         [:a {:class "btn btn-primary"
              :href  (path-for (str "/page/edit/" (:id page)))}
          (t :page/Edit)]
         [:button {:class "btn btn-primary"
                   :on-click #(.print js/window)}
          (t :core/Print)]]
        [:div.checkbox
         [:label
          [:input {:name      "visibility"
                   :type      "checkbox"
                   :on-change #(toggle-visibility (:id page) visibility-atom)
                   :checked     (= @visibility-atom "public")}]
          (t :core/Publishandshare)]]
        [s/share-buttons (str (session/get :site-url) (path-for "/page/view/") (:id page)) (:name page) (= "public" (:visibility page)) false show-link-or-embed-atom]]
       (private-this-page))
     [ph/view-page page]]))

(defn content [state]
  (let [page (:page @state)]
    (if (:ask-password @state)
      [page-password-field state]
      [:div {:id "page-container"}
       [page-content page state]])))

(defn init-data [state id]
  (ajax/GET
    (path-for (str "/obpv1/page/view/" id))
    {:response-format :json
     :keywords?       true
     :handler         (fn [data]
                        (swap! state assoc :page (:page data) :ask-password (:ask-password data)))
     :error-handler   (fn [{:keys [status status-text]}]
                        (if (= status 401)
                          (navigate-to "/user/login")))}))

(defn handler [site-navi params]
  (let [id (:page-id params)
        state (atom {:page {}
                     :page-id id
                     :ask-password false
                     :password ""
                     :password-error false
                     :show-link-or-embed-code nil})
        user (session/get :user)]
    (init-data state id)
    (fn []
      (cond (and user (= (get-in @state [:page :user_id]) (:id user))) (layout/default site-navi (content state))
            user (layout/default-no-sidebar site-navi (content state))
            :else (layout/landing-page site-navi (content state))))))
