(ns salava.mail.email-notifications
  (:require [yesql.core :refer [defqueries]]
            [clojure.tools.logging :as log]
            [clojure.set :refer [rename-keys]]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [salava.core.helper :refer [dump]]
            [slingshot.slingshot :refer :all]
            [salava.mail.mail :refer [send-html-mail]]
            [salava.core.i18n :refer [t]]
            [salava.core.util :refer [get-db get-datasource get-site-url get-base-path get-site-name get-email-notifications get-plugins plugin-fun]]
            ;[salava.social.db :refer [email-new-messages-block]]
            [salava.core.time :refer [get-day-of-week]]
            [hiccup.page :refer [html5 include-css]]
            [salava.user.db :refer [get-user-and-primary-email get-user-ids-from-event-owners]] ))



(defn email-message [ctx full-name events lng site-name]
  (let [site-url (get-site-url ctx)
        base-path (get-base-path ctx)
        social-url (str site-url base-path "/social")
        user-url (str site-url base-path "/user/edit")]
    (str  (t :user/Emailnotificationtext1 lng) " " full-name ",\n\n"
          (t :user/Emailnotificationtext2 lng)  ": " "\n\n" events "\n"
          (t :user/Emailnotificationtext3 lng) " " social-url "\n\n"
          (t :user/Emailnotificationunsubscribetext  lng) ":\n" user-url "\n\n"
          (t :user/Emailnotificationtext4 lng) ",\n\n-- " site-name " - "(t :core/Team lng))
    ))










(def style-string (slurp (io/resource "public/css/email.css")))

(defn html-mail-banner [ctx]
  (let [site-url (get-site-url ctx)]
    [:div
     {:style
      "margin-top: 10px;margin-bottom: 50px;padding-top: 0;padding-bottom: 0;"}
     [:a {:href site-url :target "_blank" :style "text-decoration: none;"}
      [:img.banner
       {:alt    "Salava",
        :style  "max-width: 640px;",
        :src    (str site-url "/img/logo.png"),
        :height "auto",
        :width  "auto"}]]]))

(defn html-mail-header [ctx]
  (let [banner (html-mail-banner ctx)]
    [:table
     {:width "100%", :border "0", :cellspacing "0", :cellpadding "0"}
     [:tr
      [:td
       {:valign "top", :align "left"}
       [:table
        {:width "100%", :border "0", :cellspacing "0", :cellpadding "0"}
        [:tr.emailTitle
         [:td
          banner]]]]]]))

(defn html-mail-header-title [text]
  [:table
   {:width "100%", :border "0", :cellspacing "0", :cellpadding "0"}
   [:tr
    [:td
     {:valign "top", :align "left"}
     [:table
      {:width "100%", :border "0", :cellspacing "0", :cellpadding "0"}
      [:tr.emailTitle
       [:td
        [:h1
         {:style
          "font-family: Arial,Helvetica,sans-serif;font-size: 30px;font-weight: normal;line-height: 40px;color: #333333;margin-top: 0;margin-bottom: 0;padding-top: 0;padding-bottom: 0;"}
         text]]]]]]])

(defn html-mail-signature [ctx lng]
  (let [site-name (get-site-name ctx)]
    [:table
     {:style       "max-width: 640px;margin-left:auto;margin-right: auto;",
      :align       "center",
      :cellspacing "0",
      :cellpadding "0",
      :width       "100%",
      :border      "0"}
     [:tr [:td {:style "font-size: 1px;line-height: 15px;"} " "]]
     [:tr
      [:td
       [:table
        {:style       "max-width: 640px;margin-left:auto;margin-right: auto;",
         :align       "center",
         :cellspacing "0",
         :cellpadding "0",
         :width       "100%",
         :border      "0"}
        [:tr
         [:td.emailPoweredBy
          {:style  "padding-top: 13px; padding-bottom: 40px; font-family: Arial,sans-serif;",
           :valign "top",
           :align  "left"}
          [:p (str (t :user/Emailnotificationtext4 lng) ",") ]
          [:p site-name " - "(t :core/Team lng)]
          ]]]]]]))


(defn get-fragment [ctx user lng type]
  (let [funs (plugin-fun (get-plugins ctx) "mail" "get-fragment")] 
    (map (fn [f] (f ctx user lng type)) funs)
    ))


(defn html-mail-template [ctx user lng subject type]
  (let [full-name (str (:first_name user) " " (:last_name user))
        background-color "#FFFFFF"
        body (get-fragment ctx  user lng type)
        footer (get-fragment ctx user lng (str type "-footer"))]
    (if body
      (html5
       [:head
        [:meta {:http-equiv "Content-Type" :content "text/html; charset=UTF-8"}]
        [:style {:type "text/css"}
         style-string]
        [:title subject]
        [:meta {:name "viewport" :content "initial-scale=1.0"}]]
       [:body
        {:style        "margin:0; padding:0; -webkit-text-size-adjust:none; -ms-text-size-adjust:none;",
         :bgcolor      background-color,
         :marginheight "0",
         :marginwidth  "0",
         :topmargin    "0",
         :leftmargin   "0"}
        [:table#bodyTable
         {:style       "border-collapse: collapse;table-layout: fixed;margin:0 auto;",
          :bgcolor     background-color,
          :width       "100%",
          :height      "100%",
          :border      "0",
          :cellspacing "0",
          :cellpadding "0"}
         [:tr
          [:td
           "<!-- Email wrapper : BEGIN -->"
           [:table.emailContainer
            {:style       "max-width: 640px;margin: auto;",
             :align       "center",
             :cellspacing "0",
             :cellpadding "0",
             :width       "640",
             :border      "0"}
            [:tr
             [:td
              {:style "padding-top: 12px", :valign "top", :align "left"}
              "<!-- Email header : start -->"
              (html-mail-header ctx)
              (html-mail-header-title (str (t :user/Emailnotificationtext1 lng) " " full-name ","))
              "<!-- Email header : end -->"]]
            [:tr
             [:td.emailBody.emailTile
              {:style "padding-top: 30px;", :valign "top", :align "left"}
              body
              ]]
            "<!-- Footer : start -->"
            [:tr
             [:td.emailTile
              {:valign "top", :align "left"}
              [:br]
              [:br]
              (html-mail-signature ctx lng)]]
            "<!-- Footer : end -->"]]]]
         [:br]
        [:br]
        footer
        "<!-- Email wrapper : END -->"]
       
       
       ))
    ))

(defn email-reminder-body [ctx user]
  (try
    (Thread/sleep 50)
    (catch InterruptedException _));lisää sleeppiä
  (try+
   (let [lng (:language user)
        
         site-name (get-in ctx [:config :core :site-name] "Open Badge Passport")
         full-name (str (:first_name user) " " (:last_name user))
         subject (str site-name ": " (t :user/Emailnotificationsubject lng))
         message (html-mail-template ctx user lng subject "email-notifications")
         ]
     (if (and message user)
       (do
         (println "-----------------------")
         (println "\n")
         (println "email:" (:email user))
         (println subject)
         (println message)
         (send-html-mail ctx subject message [(:email user)])
         )))
     (catch Object ex
       (log/error "failed to send email notification to user:")
       (log/error (.toString ex)))))


(defn email-sender [ctx]
  (if (get-email-notifications ctx)    
    (let [event-owners      (get-user-ids-from-event-owners ctx)
          day               (dec (get-day-of-week))
          current-day-users (filter #(= day (rem (:id %) 7)) event-owners)
          ]
      (doseq [user current-day-users]
        (email-reminder-body ctx user)))))





