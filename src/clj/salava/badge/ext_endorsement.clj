(ns salava.badge.ext-endorsement
 (:require
  [clojure.java.io :as io]
  [clojure.tools.logging :as log]
  [hiccup.page :refer [html5]]
  [postal.core :refer [send-message]]
  [salava.badge.main :refer [badge-exists?]]
  [salava.core.util :as util :refer [get-db-1 hex-digest digest bytes->base64 get-db plugin-fun get-plugins get-site-url get-data-dir md->html get-full-path get-db-col]]
  [salava.core.i18n :refer [t]]
  [slingshot.slingshot :refer :all]
  [yesql.core :refer [defqueries]])
 (:import
   [java.io ByteArrayOutputStream]

   [javax.imageio ImageIO]))

(defqueries "sql/badge/ext_endorsement.sql")

(def style-string (slurp (io/resource "public/css/email.css")))

(defn generate-external-id []
  (str "urn:uuid:" (java.util.UUID/randomUUID)))

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

(defn image->base64str [canvas]
  (let [out (ByteArrayOutputStream.)]
    (ImageIO/write canvas "png" out)
    (str "data:image/png;base64," (bytes->base64 (.toByteArray out)))))

(defn- check-image [width height]
 (and
  (some #(= width %) (range (- height 10) (+ height 10)))
  (some #(= height %) (range (- width 10) (+ width 10)))))

(defn upload-image [ctx file]
  (let [{:keys [size tempfile content-type]} file
        max-size 250000] ;;250kb
    (try+
     (when-not (= "image/png" content-type)
       (throw+ {:status "error" :message "badgeIssuer/FilenotPNG"}))
     (when (> size max-size)
       (throw+ {:status "error" :message "badgeIssuer/Filetoobig"}))
     (let [image (ImageIO/read tempfile)
           width (.getWidth image)
           height (.getHeight image)]
       (when-not (check-image width height) ;(= width height)
         (throw+ {:status "error" :message "badgeIssuer/Imagemustbesquare"}))
       {:status "success" :url (image->base64str image)})
     (catch Object _
       (log/error _)
       {:url "" :status "error" :message (:message _)}))))

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

(defn html-mail-signature [ctx lng id]
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
        [:p {:style "font-size: 14px !important; color: #00838f !important; text-align: center !important;"}
            "Powered by " [:img {:style "vertical-align: bottom; " :width "110px" :height "auto" :src (str "cid:"id)}]]]]]]]])


(defn- request-template [ctx message badge-info issuer-id {:keys [bid lid]}]
 (let [{:keys [name image_file language]} (-> badge-info :content first)
       full-path (get-full-path ctx)]
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
         [:td.emailBody.emailTile.emailMarkdown
          {:style "padding-top: 30px;", :valign "top", :align "center"}

          [:div {:style "padding: 10px; border-radius: 4px; background-color: ghostwhite !important;"}
           [:img {:style "max-width: 100%; max-height: 200px;" :src (str "cid:" bid)  :alt name}] ;:height "200px" :width "200px"}]
           [:div {:style "margin: 20px auto; text-align: center !important;"} message]
           [:p {:style "font-size: 18px !important; color: #039be5 !important; text-align: center !important; padding-top: 10px;"}
            [:a {:href (str full-path "/badge/info/" (:id badge-info)"?endorser="issuer-id)
                 :target "_blank"
                 :style "text-decoration: none;"}
              (t :badge/Endorsethisbadge language)]]]]]

        "<!-- Footer : start -->"
        [:tr
         [:td.emailTile
          {:valign "top", :align "center"}
          (html-mail-signature ctx language lid)]]
        "<!-- Footer : end -->"]]]
     [:br]]
    ;footer
    "<!-- Email wrapper : END -->"])))

(defn ext-endorser [ctx id]
  (get-external-endorser {:id id} (get-db-1 ctx)))

(defn send-request [ctx user-badge-id owner-id message to issuer-id]
  (let [mail-host-config (get-in ctx [:config :core :mail-host-config])
        {:keys [last_name first_name]} (as-> (first (plugin-fun (get-plugins ctx) "db" "user-information")) $
                                             (if $ ($ ctx owner-id) {}))
        badge-info (as-> (first (plugin-fun (get-plugins ctx) "main" "get-badge-p")) $
                         (if $ ($ ctx user-badge-id owner-id)))
        {:keys [name image_file language]} (-> badge-info :content first)
        image (as-> (first (plugin-fun (get-plugins ctx) "main" "png-convert-url")) $
                    (if (ifn? $) ($ ctx image_file) image_file))
        logo (if-let [path (first (mapcat #(get-in ctx [:config % :logo] []) (get-plugins ctx)))]
               path
               "resources/public/img/logo.png")
        lid (util/random-token)
        bid (util/random-token)
        data {:from    (get-in ctx [:config :core :mail-sender])
              :subject (str first_name " " last_name " " (t :badge/requestsendorsement language) " " name)
              :body    [{:type    "text/html"
                         :content (request-template ctx message badge-info issuer-id {:lid lid :bid bid})}
                        {:type :inline
                         :content (io/file (str (get-data-dir ctx) "/" (util/file-from-url-fix ctx image)))
                         :content-id bid
                         :file-name (str name ".png")}
                        {:type :inline
                         :content (io/file logo)
                         :content-id lid}]}]

    (try+
      (log/info "sending to" to)
      (-> (if (nil? mail-host-config)
            (send-message (assoc data :to to))
            (send-message mail-host-config (assoc data :to to)))
          log/info)
      (when-let [check (empty? (ext-endorser ctx issuer-id))]
        (insert-external-user! {:ext_id issuer-id :email to} (get-db ctx)))
      (catch Object ex
        (log/error ex)))))

(defn- ext-request-sent?
 [ctx user-badge-id email]
 (select-external-request-by-email {:user_badge_id user-badge-id :email email} (into {:result-set-fn first :row-fn :id} (get-db ctx))))

(defn request-external-endorsements [ctx user-badge-id owner-id emails content]
 (let [user-emails (select-badge-owner-emails {:id owner-id} (get-db-col ctx :email))]
  (doseq [email emails]
    (log/info "preparing to send request to email: " email)
    (when-let [check  (ext-request-sent? ctx user-badge-id email)]
      (throw+ {:status "error" :message "Request already sent to email"}))
    (when-let [check (some #(= % email) user-emails)]
      (throw+ {:status "error" :message "Users cannot request endorsements from themselves"}))
    (let [issuer-id (hex-digest "md5" (clojure.string/trim email))] ;;TODO use a stronger algorithm
     (request-endorsement-ext! {:id user-badge-id
                                :content content
                                :email email
                                :issuer_id issuer-id} (get-db ctx))
     (send-request ctx user-badge-id owner-id content email issuer-id)))))

(defn ext-pending-requests [ctx user-badge-id]
  (sent-pending-ext-requests-by-badge-id {:id user-badge-id} (get-db ctx)))

(defn endorse! [ctx user-badge-id data]
 (let [{:keys [content endorser]} data]
  (try+
    (when-not (badge-exists? ctx user-badge-id)
     (throw+ {:status "error" :message (str "badge with id " user-badge-id " does not exist")}))
    ;;check if user tries to endorse himself
    (let [{:keys [ext_id name url description image_file]} endorser
          existing-info (ext-endorser ctx ext_id)]
      (when-let [id (->> (insert-external-endorsement<! {:user_badge_id user-badge-id
                                                         :external_id (generate-external-id)
                                                         :issuer_id ext_id
                                                         :issuer_name name
                                                         :issuer_url url
                                                         :content content} (get-db ctx))
                         :generated_key)]
          (when-not (= (select-keys existing-info [:name :url :image_file :description]) (select-keys endorser [:name :url :image_file :description]))
           (update-external-user! {:ext_id ext_id
                                   :name name
                                   :url url
                                   :image_file (if (re-find #"^data:image" image_file)
                                                 (util/file-from-url-fix ctx image_file)
                                                 image_file)} (get-db ctx)))
          {:id id :status "success"}))
    (catch Object _
      (log/error _)
      _))))
