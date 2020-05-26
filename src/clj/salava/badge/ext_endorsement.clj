(ns salava.badge.ext-endorsement
 (:require
  [clojure.java.io :as io]
  [clojure.tools.logging :as log]
  [hiccup.page :refer [html5]]
  [postal.core :refer [send-message]]
  [salava.badge.main :refer [badge-owner? badge-exists? send-badge-info-to-obf]]
  [salava.core.util :as util :refer [publish get-site-name get-db-1 hex-digest digest bytes->base64 get-db plugin-fun get-plugins get-site-url get-data-dir md->html get-full-path get-db-col]]
  [salava.core.i18n :refer [t]]
  [slingshot.slingshot :refer :all]
  [yesql.core :refer [defqueries]]
  [salava.core.time :as time])
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
        [:div {:style "font-size: 12px !important; color: #00838f !important; text-align: center !important;"}
          [:img {:style "vertical-align: bottom; " :width "125px" :height "auto" :src (str "cid:"id)}]
          [:div {:style "margin-top: 15px;"}
            [:a {:href (str (get-full-path ctx) "/user/register") :target "_blank"}
             [:b (t :badge/Clicktojoincommunity lng)]
             #_(str (t :badge/Clicktojoin lng) " " (get-site-name ctx) " " (t :badge/community lng))]]]]]]]]])

(defn- request-template [ctx message badge-info issuer-id {:keys [bid lid fname lname oid pv lng]}]
 (let [{:keys [name image_file]} (-> badge-info :content first)
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
    [:div {:style "margin: 10px auto;"}
     [:p (if-not (= "public" pv)
           [:span (str fname " " lname " " (t :badge/Externalrequestmail1 lng) " ") [:b (t :badge/Endorsethisbadge lng)]  (str " " (t :badge/linkbelow lng) " " (t :user/or lng) " ")
            [:a {:href (str full-path "/badge/info/" (:id badge-info)"?endorser="issuer-id) :target "_blank"} (t :badge/here lng)]]
           [:span [:a {:href (str full-path "/profile/" oid) :target "_blank"} [:b (str fname " " lname)]]  (str " " (t :badge/Externalrequestmail1 lng) " ") [:b (t :badge/Endorsethisbadge lng)] (str " " (t :badge/linkbelow lng) " " (t :user/or lng) " ")
            [:a {:href (str full-path "/badge/info/" (:id badge-info)"?endorser="issuer-id) :target "_blank"} (t :badge/here lng)]])]
     [:p (t :badge/Externalrequestmail2 lng)]
     [:p (t :badge/Externalrequestmail3 lng) " " [:a {:href (str full-path "/user/external/data/" issuer-id) :target "_blank"} (str full-path "/external/mydata/" issuer-id)]]
     [:p (str (t :badge/Requestsentby lng) " ") [:a {:href (get-site-url ctx) :target "_blank"} (get-site-name ctx)]]]
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
         :align       "left",
         :cellspacing "0",
         :cellpadding "0",
         :width       "640",
         :border      "0"}
        [:tr
         [:td.emailBody.emailTile.emailMarkdown
          {:style "padding-top: 30px;", :valign "top", :align "center"}

          [:div {:style "padding: 10px; border-radius: 4px; background-color: ghostwhite !important;"}
           [:img {:style "max-width: 100%; max-height: 200px;" :src (str "cid:" bid)  :alt name}] ;:height "200px" :width "200px"}]
           [:div {:style "font-family: Arial,Helvetica,sans-serif;font-size: 25px;font-weight: 600; color: #333333;margin-top: 0;margin-bottom: 0;padding-top: 0;padding-bottom: 0; "} name]
           [:div {:style "margin: 30px auto; text-align: center !important;"} message]
           [:div {:style "font-size: 18px !important; color: #039be5 !important; text-align: center !important; padding-top: 10px;"}
            [:a {:style "font-size: 18px !important; color: #039be5 !important; text-align: center !important; padding-top: 10px; text-decoration: none;"
                 :href (str full-path "/badge/info/" (:id badge-info)"?endorser="issuer-id)
                 :target "_blank"}
              (t :badge/Endorsethisbadge lng)]]]]]

        "<!-- Footer : start -->"
        [:tr
         [:td.emailTile
          {:valign "top", :align "center"}
          (html-mail-signature ctx lng lid)]]
        "<!-- Footer : end -->"]]]
     [:br]]
    ;footer
    "<!-- Email wrapper : END -->"])))

(defn ext-endorser [ctx id]
  (get-external-endorser {:id id} (get-db-1 ctx)))

(defn send-request [ctx user-badge-id owner-id message to issuer-id]
  (let [mail-host-config (get-in ctx [:config :core :mail-host-config])
        {:keys [id last_name first_name profile_visibility language]} (as-> (first (plugin-fun (get-plugins ctx) "db" "user-information")) $
                                                                            (if $ ($ ctx owner-id) {}))
        badge-info (as-> (first (plugin-fun (get-plugins ctx) "main" "get-badge-p")) $
                         (if $ ($ ctx user-badge-id owner-id)))
        {:keys [name image_file]} (-> badge-info :content first)
        image (as-> (first (plugin-fun (get-plugins ctx) "main" "png-convert-url")) $
                    (if (ifn? $) ($ ctx image_file) image_file))
        logo (if-let [path (first (mapcat #(get-in ctx [:config % :logo] []) (get-plugins ctx)))]
               (str (get-site-url ctx) path)
               (str (get-site-url ctx) "/img/logo.png"))
        lid (util/random-token)
        bid (util/random-token)
        data {:from    (get-in ctx [:config :core :mail-sender])
              :subject (t :badge/Endorsementrequest language) ;(str first_name " " last_name " " (t :badge/requestsendorsement language) " " name)
              :body    [[:alternative
                         [:related
                          {:type    "text/html;charset=utf-8"
                           :content (request-template ctx message badge-info issuer-id {:lng language :oid owner-id :pv profile_visibility :lid lid :bid bid :fname first_name :lname last_name})}
                          {:type :inline
                           :content (io/file (str (get-data-dir ctx) "/" (util/file-from-url-fix ctx image)))
                           :content-id bid
                           :content-type "image/png"}
                          {:type :inline
                           :content (io/file (str (get-data-dir ctx) "/" (util/file-from-url-fix ctx logo)))
                           :content-id lid
                           :content-type "image/png"}]]]}]

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

(defn is-already-endorsed? [ctx user-badge-id issuer-id]
 (if-let [id (select-existing-endorsement {:issuer issuer-id :ubid user-badge-id} (into {:result-set-fn first :row-fn :id} (get-db ctx)))] true false))

(defn email-already-endorsed? [ctx user-badge-id email]
 (if-let [id (select-existing-endorsement-by-email {:issuer email :ubid user-badge-id} (into {:result-set-fn first :row-fn :id} (get-db ctx)))] true false))

(defn external-request-by-issuerid [ctx user-badge-id issuer-id]
 (if-let [_ (select-external-badge-request-by-issuerid {:ubid user-badge-id :isid issuer-id}  (get-db-1 ctx))] _ {}))

(defn request-external-endorsements [ctx user-badge-id owner-id emails content]
 ;;TODO check badge visibility and set to public
 (let [user-emails (select-badge-owner-emails {:id owner-id} (get-db-col ctx :email))]
  (doseq [email emails]
    (log/info "preparing to send request to email: " email)
    (when-let [check  (ext-request-sent? ctx user-badge-id email)]
      (throw+ {:status "error" :message "Request already sent to email"}))
    (when-let [check (some #(= % email) user-emails)]
      (throw+ {:status "error" :message "Users cannot request endorsements from themselves"}))
    (when-let [check (email-already-endorsed? ctx user-badge-id email)]
      (throw+ {:status "error" :message (str "Badge already endorsed by email: " email)}))
    (let [issuer-id (util/hmac-sha256-hex email (get-in ctx [:config :factory :secret]))
          badge-info (as-> (first (plugin-fun (get-plugins ctx) "main" "get-badge-p")) $
                           (if $ ($ ctx user-badge-id owner-id)))
          {:keys [name image_file language visibility]} (-> badge-info :content first)]
     (when-not (= "public" visibility)
       (as-> (first (plugin-fun (get-plugins ctx) "main" "set-visibility!")) $
             (when (ifn? $) ($ ctx user-badge-id "public" owner-id))))
     (request-endorsement-ext! {:id user-badge-id
                                :content content
                                :email email
                                :issuer_id issuer-id} (get-db ctx))
     (send-request ctx user-badge-id owner-id content email issuer-id)))))

(defn ext-pending-requests [ctx user-badge-id]
  (sent-pending-ext-requests-by-badge-id {:id user-badge-id} (get-db ctx)))

(defn update-request-status [ctx user-badge-id issuer-email status]
 (try+
  (update-request-status! {:e issuer-email :ubid user-badge-id :status status} (get-db ctx))
  {:status "success"}
  (catch Object _
   (log/error _)
   {:status "error"})))

(defn endorse! [ctx user-badge-id data]
 (let [{:keys [content endorser]} data]
  (try+
    (when-not (badge-exists? ctx user-badge-id)
     (throw+ {:status "error" :message (str "badge with id " user-badge-id " does not exist")}))
    (when (is-already-endorsed? ctx user-badge-id (:ext_id endorser))
      (throw+ {:status "error" :message "badge already endorsed by user"}))
    ;;check if user tries to endorse himself
    (let [{:keys [ext_id name url description image_file email]} endorser
          existing-info (ext-endorser ctx ext_id)]
      (when-let [id (->> (insert-external-endorsement<! {:user_badge_id user-badge-id
                                                         :external_id (generate-external-id)
                                                         :issuer_id ext_id
                                                         :issuer_name name
                                                         :issuer_url url
                                                         :content content} (get-db ctx))
                         :generated_key)]
          (when-not (= (select-keys existing-info [:name :url :image_file :description]) (select-keys endorser [:name :url :image_file :description]))
           (update-external-user! {:id (:id existing-info)
                                   :ext_id ext_id
                                   :name name
                                   :url url
                                   :email email
                                   :ctime (:ctime existing-info)
                                   :description description
                                   :image_file (if (clojure.string/blank? image_file)
                                                   nil
                                                   (if (re-find #"^data:image" image_file)
                                                     (util/file-from-url-fix ctx image_file)
                                                     image_file))}
                                  (get-db ctx)))
          (update-request-status ctx user-badge-id email "endorsed")
          (publish ctx :endorse_badge_ext {:subject (:id existing-info) :verb "endorse_badge_ext" :object id :type "badge"})
          {:id id :status "success"}))
    (catch Object _
      (log/error _)
      _))))

(defn update-endorsement! [ctx id user-badge-id data]
  (let [{:keys [content endorser]} data]
   (try+
     (when-not (badge-exists? ctx user-badge-id)
      (throw+ {:status "error" :message (str "badge with id " user-badge-id " does not exist")}))
     (let [{:keys [ext_id name url description image_file email]} endorser
           existing-info (ext-endorser ctx ext_id)]

       (update-external-endorsement! {:ubid user-badge-id
                                      :id id
                                      ;:external_id (generate-external-id)
                                      :isid ext_id
                                      :issuer_name name
                                      :issuer_url url
                                      :content content} (get-db ctx))
       (when-not (= (select-keys existing-info [:name :url :image_file :description]) (select-keys endorser [:name :url :image_file :description]))
        (update-external-user! {:id (:id existing-info)
                                :ext_id ext_id
                                :name name
                                :url url
                                :email email
                                :ctime (:ctime existing-info)
                                :description description
                                :image_file (if (clojure.string/blank? image_file)
                                               nil
                                               (if (re-find #"^data:image" image_file)
                                                   (util/file-from-url-fix ctx image_file)
                                                   image_file))}

                      (get-db ctx))))
     {:id id :status "success"}
     (catch Object _
       (log/error _)
       {:status "error"}))))

(defn delete-endorsement! [ctx id]
 ;;TODO check if endorsement owner
 (try+
  (delete-external-endorsement! {:id id} (get-db ctx))
  {:status "success"}
  (catch Object _
   (log/error _)
   {:status "error"})))

(defn update-endorsement-status! [ctx user-id user-badge-id id status]
   (try+
     (when (badge-owner? ctx user-badge-id user-id)
       (update-ext-endorsement-status! {:id id :status status} (get-db ctx))
       (case status
         "accepted" (send-badge-info-to-obf ctx user-badge-id user-id)
         "declined" (delete-endorsement! ctx id);(delete! ctx user-badge-id endorsement-id user-id)
         nil)
       {:status "success"})
     (catch Object _
       (log/error _)
       {:status "error"})))

(defn given-user-badge-endorsement [ctx user-badge-id issuer-id]
 (select-user-badge-issuer-endorsement {:ubid user-badge-id :issuer issuer-id} (get-db-1 ctx)))

(defn all-endorsements
 ([ctx issuer-id]
  (select-all-issuer-endorsements {:issuer issuer-id} (get-db ctx)))
 ([ctx issuer-id export?]
  {:endorsements (->> (all-endorsements ctx issuer-id)
                      (mapv #(:content %)))}))
                      ;(mapv #(hash-map :endorsement (:content %))))}))

(defn all-requests
 ([ctx issuer-id]
  (->> (select-all-endorsement-requests {:issuer issuer-id} (get-db ctx))
       (mapv #(assoc % :type "request"))
       (filter #(= (:status %) "pending"))))
 ([ctx issuer-id export?]
  {:Endorsementrequests (->> (all-requests ctx issuer-id)
                             (mapv #(hash-map :Endorsementrequest (:content %))))}))

(defn delete-external-user-endorsements [ctx issuer-id]
 (let [email (:email (ext-endorser ctx issuer-id))]
  (delete-all-user-endorsements! {:issuer issuer-id} (get-db ctx))
  (delete-all-user-requests! {:issuer email} (get-db ctx))))

(defn insert-ext-endorse-event! [ctx data]
  (insert-ext-endorsement-event<! data (get-db ctx)))

(defn insert-ext-endorsement-owner! [ctx data]
 (let [owner-id (select-ext-endorsement-receiver-by-badge-id {:id (:object data)} (into {:result-set-fn first :row-fn :id} (get-db ctx)))]
   (insert-event-owner! (assoc data :object owner-id) (get-db ctx))))

(defn endorsements-received
 ([ctx user-id]
  (map (fn [e] (-> e (update :content md->html)))
       (select-ext-received-endorsements {:user_id user-id} (get-db ctx))))
 ([ctx user-id md?]
  (select-ext-received-endorsements {:user_id user-id} (get-db ctx))))

(defn sent-endorsement-requests [ctx user-id]
  (map (fn [r] (-> r (update :content md->html))) (select-sent-ext-endorsement-requests {:user_id user-id} (get-db ctx))))

(defn delete-sent-request! [ctx id]
 (try+
  (delete-sent-external-request!  {:id id} (get-db ctx))
  {:status "success"}
  (catch Object _
   (log/error _)
   {:status "error"})))

(defn delete-pending-request! [ctx user-badge-id]
 (let [requests (sent-pending-ext-requests-by-badge-id {:id user-badge-id} (get-db ctx))]
  (try+
   (doseq [r requests
           :let [days-pending (time/no-of-days-passed (long (:mtime r)))]]
    (when (>= days-pending 30)
      (log/info "Expired pending external request id" (:id r))
      (log/info "Deleting pending external request id " (:id r))
      (delete-sent-request!  ctx (:id r))
      (log/info "Pending external request deleted!")))
   (catch Object _
     (log/error _)))))

(defn delete-badge-endorsements! [db id]
  (delete-user-badge-ext-endorsements! {:id id} db))

(defn delete-badge-endorsement-requests! [db id]
  (delete-user-badge-ext-endorsement-requests! {:id id} db))
