(ns salava.mail.mail
  (:require [clojure.java.io :as io]
            [hiccup.core :refer :all]
            [postal.core :refer [send-message]]
            [salava.core.util :refer [get-site-url get-base-path get-site-name get-plugins plugin-fun]]
            [hiccup.page :refer [html5 include-css]]
            [slingshot.slingshot :refer :all]
            [salava.core.i18n :refer [t]]))

(defn send-mail [ctx subject message recipients]
  (try+
    (let [mail-host-config (get-in ctx [:config :core :mail-host-config])
          data {:from    (get-in ctx [:config :core :mail-sender])
                :bcc      recipients
                :subject subject
                :body    [{:type    "text/plain; charset=utf-8"
                           :content message}]}]
      (if (nil? mail-host-config)
        (send-message data)
        (send-message mail-host-config data)))
    (catch Object _
      ;TODO log an error
      )))

(defn send-html-mail [ctx subject message recipients]
  (try+
    (let [mail-host-config (get-in ctx [:config :core :mail-host-config])
          data {:from    (get-in ctx [:config :core :mail-sender])
                :bcc      recipients
                :subject subject
                :body    [{:type "text/html; charset=utf-8"
                           :content message}]}]
      (if (nil? mail-host-config)
        (send-message data)
        (send-message mail-host-config data)))
    (catch Object _
      ;TODO log an error
      )))

(defn send-activation-message [ctx site-url activation-link login-link fullname email-address lng]
  (let [site-name (get-in ctx [:config :core :site-name])
        subject (str (t :core/Welcometo lng) " " site-name (t :core/Service lng))
        message (str fullname
                     ",\n\n" (t :core/Emailactivation2 lng) " " site-url  ".\n" (t :core/Emailactivation4 lng) ":\n\n"
                     activation-link
                     "\n\n" (t :core/Emailactivation5 lng) "\n" (t :core/Emailactivation6 lng) ".\n\n" (t :core/Emailactivation7 lng) "\n"
                     login-link
                     " " (t :core/Emailactivation8 lng) ".\n\n--  "site-name " -"(t :core/Team lng))]
    (send-mail ctx subject message [email-address])))

(defn send-password-reset-message [ctx site-url activation-link fullname email-address lng]
  (let [site-name (get-in ctx [:config :core :site-name])
        subject (str  site-name " " (t :core/Emailresetheader lng))
        message (str fullname ",\n\n" (t :core/Emailresetmessage1 lng) " " site-url
                     ".\n\n" (t :core/Emailactivation4 lng)":\n\n"
                     activation-link
                     "\n\n" (t :core/Emailactivation5 lng) "\n" (t :core/Emailactivation6 lng) ".\n\n" (t :core/Emailresetmessage2 lng) ".\n\n--  "
                      site-name " -"(t :core/Team lng))]
    (send-mail ctx subject message [email-address])))

(defn send-verification [ctx site-url email-verification-link fullname email lng]
  (let [site-name (get-in ctx [:config :core :site-name])
        subject (str (t :core/Emailverification1 lng) " " site-name )
        message (str fullname "\n\n" (t :core/Emailverification2 lng) " '" email "' " (t :core/Emailverification3 lng) " " site-url".\n" (t :core/Emailverification4 lng) ":\n\n"
                     email-verification-link
                     "\n\n" (t :core/Emailverification6 lng)".\n")]
    (send-mail ctx subject message [email])))






(defn get-fragments
  ([ctx type] (get-fragments ctx nil nil type))
  ([ctx user lng type]
   (let [funs (plugin-fun (get-plugins ctx) "mail" "get-fragment")] 
     (remove nil? (map (fn [f] (try (f ctx user lng type) (catch Throwable _))) funs))
     ))
  )


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
  (let [banner (or (first (get-fragments ctx "mail-banner")) (html-mail-banner ctx))]
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





(defn html-mail-template [ctx user lng subject type]
  (let [full-name (str (:first_name user) " " (:last_name user))
        background-color "#FFFFFF"
        body (first (get-fragments ctx  user lng type))
        footer (first (get-fragments ctx user lng (str type "-footer")))]
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
