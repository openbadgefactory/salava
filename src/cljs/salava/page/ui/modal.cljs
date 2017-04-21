(ns salava.page.ui.modal
  (:require [reagent.core :refer [atom create-class]]
            [reagent-modals.modals :as m]
            [markdown.core :refer [md->html]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.helper :refer [navigate-to path-for]]
            [salava.badge.ui.helper :as bh]
            [salava.core.time :refer [date-from-unix-time]]
            [salava.file.icons :refer [file-icon]]
            [salava.core.helper :refer [dump]]
            [salava.admin.ui.reporttool :refer [reporttool]]
            [reagent.session :as session]
            [salava.core.ui.error :as err]
            [salava.core.ui.modal :refer [set-new-view]]
            [salava.page.ui.helper :refer [badge-block html-block file-block heading-block tag-block]]
            ))



(defn content [state]  
  (let [{:keys [id name description mtime user_id first_name last_name blocks theme border padding visibility qr_code]} (:page @state)]
    [:div {:id    (str "theme-" (or theme 0))
           :class "page-content"}
     (if id
       [:div.panel
        [:div.panel-left
         [:div.panel-right
          [:div.panel-content
           (if (and qr_code (= visibility "public"))
             [:div.row
              [:div {:class "col-xs-12 text-center"}
               [:img#print-qr-code {:src (str "data:image/png;base64," qr_code)}]]])
           (if mtime
             [:div.row
              [:div {:class "col-md-12 page-mtime"}
               (date-from-unix-time (* 1000 mtime))]])
           [:div.row
            [:div {:class "col-md-12 page-title"}
             [:h1 name]]]
           [:div.row
            [:div {:class "col-md-12 page-author"}
             [:a {:href "#"
                  :on-click #(set-new-view [:user :profile] {:user-id user_id})} (str first_name " " last_name)]]]
           [:div.row
            [:div {:class "col-md-12 page-summary"}
             description]]
           (into [:div.page-blocks]
                 (for [block blocks]
                   [:div {:class "block-wrapper"
                          :style {:border-top-width (:width border)
                                  :border-top-style (:style border)
                                  :border-top-color (:color border)
                                  :padding-top (str padding "px")
                                  :margin-top (str padding "px")}}
                    (case (:type block)
                      "badge" (badge-block block)
                      "html" (html-block block)
                      "file" (file-block block)
                      "heading" (heading-block block)
                      "tag" (tag-block block)
                      nil)]))]]]])]))





(defn init-data [page-id state]
  (let [reporttool-init {:description ""
                         :report-type "bug"
                         :item-id ""
                         :item-content-id ""
                         :item-url   ""
                         :item-name "" ;
                         :item-type "" ;badge/user/page/badges
                         :reporter-id ""
                         :status "false"}]
    
    (ajax/GET
     (path-for (str "/obpv1/page/view/" page-id) true)
     {:handler (fn [data]
                 
                 (reset! state (assoc data
                                      :page-id page-id
                                      :show-link-or-embed-code nil
                                      :permission "success"
                                      :badge-small-view false
                                      :reporttool reporttool-init)))}
     (fn [] (swap! state assoc :permission "error")))))


(defn handler [params]
  
  (let [page-id (:page-id params)
        state (atom {:page {}
                     :initializing true
                     :permission "initial"
                     :reporttool {}})
        user (session/get :user)]
    
    (init-data page-id state)
    
    (fn []
      (cond
        (= "initial" (:permission @state)) [:div ""]
        (and user (= "error" (:permission @state))) (err/error-content)
        (= "error" (:permission @state)) (err/error-content)
        (= "success" (:permission @state)) (content state) 
        (and (= "success" (:permission @state)) user) (content state) 
        :else (content state) ))
    ))

(def ^:export modalroutes
  {:page {:view handler}})
