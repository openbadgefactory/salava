(ns salava.badge.ext-endorsement
 (:require
  [clojure.java.io :as io]
  [clojure.tools.logging :as log]
  [hiccup.core :refer [html]]
  [hiccup.page :refer [html5 include-css]]
  [postal.core :refer [send-message]]
  [salava.core.util :refer [digest bytes->base64 get-db plugin-fun get-plugins get-site-url get-data-dir md->html]]
  [salava.core.i18n :refer [t]]
  [slingshot.slingshot :refer :all]
  [yesql.core :refer [defqueries]])
 (:import
   [java.io ByteArrayOutputStream]
   [javax.imageio ImageIO]))

(defqueries "sql/badge/ext_endorsement.sql")

(def style-string (slurp (io/resource "public/css/email.css")))

(defn get-fragments
  ([ctx type] (get-fragments ctx nil nil type))
  ([ctx user lng type]
   (let [funs (plugin-fun (get-plugins ctx) "mail" "get-fragment")]
     (remove nil? (map (fn [f] (try (f ctx user lng type) (catch Throwable _))) funs)))))

(defn html-mail-banner [ctx]
  (let [site-url (get-site-url ctx)]
    [:div
     {:style
      "margin-top: 10px;margin-bottom: 50px;padding-top: 0;padding-bottom: 0;"}
     [:a {:href site-url :target "_blank" :style "text-decoration: none;"}
      [:img.banner
       {:alt    "Salava",
        :style  "max-width: 640px;",
        :src    "cid:012345678" #_(str site-url "/img/logo.png"),
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


(defn- image->base64str [file]
 (let [img (ImageIO/read (io/file file))
       out (ByteArrayOutputStream.)]
   (ImageIO/write img "png" out)
   (str "data:image/png;base64," (bytes->base64 (.toByteArray out)))))

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
  (let [];site-name (get-site-name ctx)]
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
          {:style  "padding: 10px; font-family: Arial,sans-serif; background-color: #f9f9f9;",
           :valign "top",
           :align  "center"}
          [:p {:style "font-size: 14px !important; color: #00838f !important; text-align: center !important;"} "Powered by " [:img {:style "vertical-align: bottom; " :width "110px" :height "auto" :src "cid:012345678"}]]]]]]]])) ;(str (t :user/Emailnotificationtext4 lng) ",")]
          ;[:p site-name " - "(t :core/Team lng)]]]]]]]))


(defn- request-template [ctx message badge-info]
 (let [{:keys [name image_file language]} (-> badge-info :content first)
       message (md->html message)]
  (html5
   [:head
    [:meta {:http-equiv "Content-Type" :content "text/html; charset=UTF-8"}]
    [:meta {:name "viewport" :content "initial-scale=1.0"}]
    [:style {:type "text/css"}
     style-string]]
   [:body
    {:style        "margin:0; padding:0; -webkit-text-size-adjust:none; -ms-text-size-adjust:none;",
     ;:bgcolor      background-color,
     :marginheight "0",
     :marginwidth  "0",
     :topmargin    "0",
     :leftmargin   "0"}
    [:table#bodyTable
     {:style       "border-collapse: collapse;table-layout: fixed;margin:0 auto;",
      ;:bgcolor     background-color,
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
         [:td.emailBody.emailTile
          {:style "padding-top: 30px;", :valign "top", :align "center"}

          [:div {:style "padding: 10px; border-radius: 4px; background-color: ghostwhite !important;"}
           [:img {:style "max-width: 100%; max-height: 200px;" :src "cid:0123456789"  :alt name}] ;:height "200px" :width "200px"}]
           [:div {:style "margin: 20px auto;"} message]
           [:p {:style "font-size: 18px !important; color: #039be5 !important; text-align: center !important; padding-top: 10px;"} "Endorse this badge"]]]]

        "<!-- Footer : start -->"
        [:tr
         [:td.emailTile
          {:valign "top", :align "center"}
          (html-mail-signature ctx language)]]
        "<!-- Footer : end -->"]]]
     [:br]]
    ;footer
    "<!-- Email wrapper : END -->"])))


(defn send-request [ctx user-badge-id owner-id message to]
  (let [mail-host-config (get-in ctx [:config :core :mail-host-config])
        {:keys [last_name first_name]} (as-> (first (plugin-fun (get-plugins ctx) "db" "user-information")) $
                                             (if $ ($ ctx owner-id) {}))
        badge-info (as-> (first (plugin-fun (get-plugins ctx) "main" "get-badge-p")) $
                         (if $ ($ ctx user-badge-id owner-id)))
        {:keys [name image_file language]} (-> badge-info :content first)
        data {:from    (get-in ctx [:config :core :mail-sender])
              :subject (str first_name " " last_name " " (t :badge/requestsendorsement language) " " name)
              :body    [{:type    "text/html"
                         :content (request-template ctx message badge-info)}
                        {:content-type "image/png"
                         :type :inline
                         :content (io/file (str (get-data-dir ctx) "/" image_file))
                         :content-id "0123456789"}
                        {:content-type "image/png"
                         :type :inline
                         :content (io/file  "resources/public/img/logo.png") ;(str (get-data-dir ctx) "/" image_file))
                         :content-id "012345678"}]}]


    (try+
      (log/info "sending to" to)
      (-> (if (nil? mail-host-config)
            (send-message (assoc data :to to))
            (send-message mail-host-config (assoc data :to to)))
          log/info)
      (catch Object ex
        (log/error ex)))))

(defn- ext-request-sent?
 [ctx user-badge-id email]
 (select-external-request-by-email {:user_badge_id user-badge-id :email email} (into {:result-set-fn first :row-fn :issuer_email} (get-db ctx))))

(defn request-external-endorsements [ctx user-badge-id owner-id emails content]
  (doseq [email emails]
    (log/info "preparing to send request to email: " email)
    (if-let [check (-> (ext-request-sent? ctx user-badge-id email))]
      (throw+ {:status "error" :message "Request already sent to email"})
      (let [issuer-id (-> (digest "sha256" email) (bytes->base64))]
       (request-endorsement-ext! {:id user-badge-id
                                  :content content
                                  :email email
                                  :issuer_id issuer-id} (get-db ctx))))))
       ;()))))
