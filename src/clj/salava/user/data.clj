(ns salava.user.data
  (:require
    [yesql.core :refer [defqueries]]
    [salava.user.db :as u]
    [salava.badge.main :as b]
    [salava.page.main :as p]
    [salava.file.db :as f]
    [salava.social.db :as so]
    [salava.core.helper :refer [dump]]
    [salava.core.util :as util]
    [clj-pdf.core :as pdf]
    [clj-pdf-markdown.core :refer [markdown->clj-pdf]]
    [salava.core.time :refer [unix-time date-from-unix-time]]
    [clojure.string :refer [ends-with? join capitalize blank? lower-case upper-case]]
    [clojure.zip :as zip]
    [net.cgrand.enlive-html :as enlive]
    [salava.core.i18n :refer [t translate-text]]
    [salava.user.schemas :refer [contact-fields]]
    [salava.badge.pdf :refer [replace-nils process-markdown]]
    ))

(defqueries "sql/badge/main.sql")
(defqueries "sql/social/queries.sql")
(defqueries "sql/admin/queries.sql")

(defn events-helper [ctx event user-id]
  (let [badge-info (select-multi-language-badge-content {:id (:object event)} (util/get-db ctx))
        badge-id (select-user-badge-id-by-badge-id-and-user-id {:id (:id event)} (util/get-db ctx))
        get-badge (b/fetch-badge ctx (:id badge-id))
        user-info (u/user-information ctx (:object event))
        user-badge-info (b/fetch-badge ctx (:object event))
        message (select-message-by-badge-id-and-user-id {:badge_id (:object event) :user_id user-id :ctime (:ctime event)} (util/get-db ctx))
        page-info (p/page-with-blocks ctx (:object event))]

    (cond
      (and (= (:verb event) "follow") (= (:type event) "badge")) {:object_name (:name (first badge-info)) :badge_id (:id badge-id)}
      (and (= (:verb event) "follow") (= (:type event) "user")) {:object_name (str (:first_name user-info) " " (:last_name user-info)) :id (:object event)}
      (and (= (:verb event) "congratulate") (= (:type event) "badge")) {:object_name (:name (first (:content user-badge-info))) :badge_id (:object event)}
      (and (= (:verb event) "message") (= (:type event) "badge")) {:object_name (:name (first badge-info)) :badge_id badge-id :message (first message)}
      (and (= (:verb event) "publish") (= (:type event) "badge")) {:object_name (:name (first (:content user-badge-info))) :badge_id (:object event)}
      (and (= (:verb event) "unpublish") (= (:type event) "badge")) {:object_name (:name (first (:content user-badge-info))) :badge_id (:object event)}
      (and (= (:verb event) "publish") (= (:type event) "page")) {:object_name (:name page-info) :page_id (:id page-info) }
      (and (= (:verb event) "unpublish") (= (:type event) "page")) {:object_name (:name page-info) :page_id (:id page-info) }
      :else nil
      )))

(defn all-user-data
  ([ctx user-id current-user-id]
   (let [all-user-info (u/user-information-and-profile ctx user-id current-user-id)
         email-addresses (u/email-addresses ctx current-user-id)
         user-badges (b/user-badges-all ctx current-user-id)
         user-pages (p/user-pages-all ctx current-user-id)
         user-files (f/user-files-all ctx  current-user-id)
         events (map #(-> %
                          (assoc :info (events-helper ctx % user-id))) (so/get-all-user-events ctx user-id))
         connections (so/get-connections-badge ctx current-user-id)
         pending-badges (b/user-badges-pending ctx user-id)
         user-followers-fn (first (util/plugin-fun (util/get-plugins ctx) "db" "get-user-followers-connections"))
         user-followers (if-not (nil? user-followers-fn) (user-followers-fn ctx user-id) nil)
         user-following-fn (first (util/plugin-fun (util/get-plugins ctx) "db" "get-user-following-connections-user"))
         user-following (if-not (nil? user-followers-fn) (user-following-fn ctx user-id) nil) ]

     (assoc (replace-nils (assoc all-user-info
                            :emails email-addresses
                            :user_badges user-badges
                            :user_pages user-pages
                            :user_files (:files user-files)
                            ;:events events
                            :connections connections
                            :pending_badges pending-badges
                            :user_followers user-followers
                            :user_following user-following
                            )) :events events)))

  ([ctx user-id current-user-id _]
   (let [all-user-info (u/user-information-and-profile ctx user-id current-user-id)
         email-addresses (u/email-addresses ctx current-user-id)
         user-badges (map (fn [b]
                            {:id (:id b)
                             :badge_id (:badge_id b)
                             :name (:name b)})(b/user-badges-all ctx current-user-id))
         pending-badges (map (fn [pb]
                               {:name (:name pb)
                                :description (:description pb)}) (b/user-badges-pending ctx user-id))
         user-pages (map (fn [p]
                           {:id (:id p)
                            :name (:name p)})(p/user-pages-all ctx current-user-id))
         user-files (map (fn [f]
                           {:name (:name f)
                            :path (:path f)}) (:files (f/user-files-all ctx  current-user-id)))
         events (map #(-> %
                          (assoc :info (events-helper ctx % user-id))) (so/get-all-user-events ctx user-id))
         connections (count (so/get-connections-badge ctx current-user-id))
         user-followers-fn (first (util/plugin-fun (util/get-plugins ctx) "db" "get-user-followers-connections"))
         user-followers (if-not (nil? user-followers-fn) (user-followers-fn ctx user-id) ())
         user-following-fn (first (util/plugin-fun (util/get-plugins ctx) "db" "get-user-following-connections-user"))
         user-following (if-not (nil? user-followers-fn) (user-following-fn ctx user-id) ())]
     (replace-nils (assoc all-user-info
                          :emails email-addresses
                          :user_badges user-badges
                          :user_pages user-pages
                          :user_files user-files
                          :events events
                          :connections connections
                          :pending_badges pending-badges
                          :user_followers user-followers
                          :user_following user-following
                     )))))

(defn strip-html-tags [s]
  (->> s
       java.io.StringReader.
       enlive/html-resource
       first
       zip/xml-zip
       (iterate zip/next)
       (take-while (complement zip/end?))
       (filter (complement zip/branch?))
       (map zip/node)
       (apply str)
       ))

(defn export-data-to-pdf [ctx user-id current-user-id]
  (let [data-dir (get-in ctx [:config :core :data-dir])
        site-url (get-in ctx [:config :core :site-url])
        user-data (conj () (all-user-data ctx user-id current-user-id))
        ul (let [lang (get-in (first user-data) [:user :language])]
             (if (= "-" lang) "en" lang))
        font-path  (first (mapcat #(get-in ctx [:config % :font] []) (util/get-plugins ctx)))
        font  {:ttf-name (str site-url font-path)}
        stylesheet {:heading-name {:color [127 113 121]
                                   :family :times-roman
                                   :align :center}

                    :generic {:family :times-roman
                              :color [127 113 121]
                              :indent 20}
                    :link {:family :times-roman
                           :color [66 100 162]}
                    :chunk {:size 11
                            :style :bold}}
        pdf-settings  (if (empty? font-path) {:stylesheet stylesheet  :bottom-margin 0 :footer {:page-numbers false :align :right}} {:font font :stylesheet stylesheet  :bottom-margin 0 :footer {:page-numbers false :align :right}})
        user-info-template (pdf/template
                             (let [template (cons [:paragraph]
                                                  [[:heading.heading-name
                                                    (str (capitalize (:first_name $user)) " " (capitalize (:last_name $user)))]
                                                   [:spacer 2]
                                                   [:paragraph.generic
                                                    [:chunk.chunk (str (t :user/UserID ul)": ")] [:chunk (str (:id $user))]"\n"
                                                    [:chunk.chunk (str (t :user/Role ul) ": ")] [:chunk (if (= "user" (:role $user)) (t :social/User ul) (t :admin/Admin ul))]"\n"
                                                    [:chunk.chunk (str (t :user/Firstname ul)": ")] [:chunk (capitalize (:first_name $user))]"\n"
                                                    [:chunk.chunk (str (t :user/Lastname ul)": ")][:chunk (capitalize (:last_name $user))]"\n"
                                                    [:chunk.chunk (str (t :user/Profilepicture ul)": ")] [:chunk.link (str site-url "/" (:profile_picture $user))]"\n"
                                                    [:chunk.chunk (str (t :user/Language ul)": ")][:chunk (str (upper-case (:language $user)) "  ")]
                                                    [:chunk.chunk (str (t :user/Country ul)": ")][:chunk (str (:country $user))]"\n"
                                                    [:chunk.chunk (str (t :user/Activated ul) ": ")][:chunk (str (if (true? (:activated $user)) (t :core/Yes ul) (t :core/No ul)) "  ")]
                                                    [:chunk.chunk (str (t :user/Emailnotifications ul) ": ")][:chunk (str (if (true? (:email_notifications $user)) (t :core/Yes ul) (t :core/No ul)) "  ")]
                                                    [:chunk.chunk (str (t :user/Profilevisibility ul) ": ")][:chunk (t (keyword (str "core/"(capitalize (:profile_visibility $user)))) ul)]"\n"
                                                    [:paragraph
                                                     [:chunk.chunk (str (t :user/Aboutme ul) ":")]"\n"
                                                     [:paragraph (str (:about $user)) ]]]
                                                   [:spacer 2]])]
                               template))

        email-template (pdf/template
                         [:paragraph.generic
                          (if (> (count $emails) 1) [:heading.heading-name (t :user/Emailaddresses ul)] [:heading.heading-name (t :user/Emailaddress ul)])
                          [:spacer 0]
                          (into [:paragraph]
                                (for [e $emails
                                      :let [primary-address (:primary_address e)]]
                                  [:paragraph
                                   [:chunk.chunk (str (t :user/Emailaddress ul)": ")] [:chunk  (:email e)]"\n"
                                   [:chunk.chunk (str (t :social/Created ul) ": ")] [:chunk (if (number? (:ctime e)) (str (date-from-unix-time (long (* 1000 (:ctime e))) "date")"  ") (str (:ctime e) " "))]
                                   [:chunk.chunk (str (t :user/verified ul)": ")] [:chunk (str (if (true? (:verified e)) (t :core/Yes ul) (t :core/No ul)) "  ")]
                                   (when (= true primary-address)
                                     [:phrase
                                      [:chunk.chunk (str (t :user/Loginaddress ul)": ")] [:chunk (t :core/Yes ul)] "\n"]
                                     )
                                   [:chunk.chunk (str (t :user/BackpackID ul) ": ")][:chunk (str (:backpack_id e))]]))])

        profile-template (pdf/template
                           [:paragraph.generic
                            (when (not-empty $profile)
                              [:paragraph.generic
                               [:heading.heading-name  (t :user/Myprofile ul)]
                               [:spacer 0]
                               (into [:paragraph ] (for [p (sort-by :order $profile)
                                                         :let[ k (->> contact-fields
                                                                      (filter #(= (:type %) (:field p)))
                                                                      first
                                                                      :key)]]
                                                     [:phrase
                                                      [:chunk.chunk (str (t k ul)": ")] [:chunk  (str (:value p) "  ")]]
                                                     ))])])
        badge-template (pdf/template
                         [:paragraph.generic
                          (when-not (empty? $user_badges)
                            [:spacer 0]
                            [:heading.heading-name (t :badge/Mybadges ul)]
                            (into [:paragraph]
                                  (for [b $user_badges
                                        :let [more-badge-info (replace-nils (b/get-badge ctx (:id b) user-id))
                                              content (:content more-badge-info)
                                              congratulated? (:congratulated? more-badge-info)
                                              message-count (so/get-badge-message-count ctx (:badge_id b) user-id)
                                              messages (replace-nils (select-badge-messages {:badge_id (:badge_id b)} (util/get-db ctx)))
                                              endorsements (replace-nils (select-badge-endorsements {:id (:badge_id b)} (util/get-db ctx)))

                                              template #(cons [:paragraph][[:paragraph
                                                                            [:chunk.chunk (str (t :badge/BadgeID ul) ": ")][:chunk (str (or (:badge_id b ) "-"))]"\n"
                                                                            [:chunk.chunk (str (t :badge/Name ul) ": ") ][:chunk (or (:name %) "-")]"\n"
                                                                            [:chunk.chunk (str (t :page/Description ul) ": ")][:chunk (str (:description %))]"\n"
                                                                            [:chunk.chunk (str (t :badge/Imagefile ul) ": ")][:chunk.link (str site-url "/"(:image_file %))]"\n"
                                                                            [:chunk.chunk (str (t :badge/Issuedby ul) ": ")] [:chunk (str (:issuer_content_name %)"  ")]
                                                                            [:chunk.chunk (str (t :badge/Issuerurl ul) ": ")][:chunk.link (str (:issuer_content_url %))]"\n"
                                                                            [:chunk.chunk (str (t :badge/Issuercontact ul) ": ")] [:chunk.link (str (:issuer_contact %))]"\n"
                                                                            (when-not (= "-" (:creator_name %))
                                                                              [:phrase
                                                                               [:chunk.chunk (str (t :badge/Createdby ul) ": ")][:chunk (str (:creator_name %))]"\n"])
                                                                            (when-not (= "-" (:creator_url %))
                                                                              [:phrase
                                                                               [:chunk.chunk (str (t :badge/Creatorurl ul) ": ")][:chunk.link (str (:creator_url %))]"\n"])
                                                                            (when-not (= "-" (:creator_email %))
                                                                              [:phrase
                                                                               [:chunk.chunk (str (t :badge/Creatorcontact ul) ": ")][:chunk.link (str (:creator_email %))]"\n"])
                                                                            [:chunk.chunk (str (t :badge/Criteriaurl ul) ": ") ][:chunk.link (str (:criteria_url %))]"\n"
                                                                            [:paragraph
                                                                             [:chunk.chunk (str (t :badge/Criteria ul) ": ")][:paragraph (strip-html-tags (:criteria_content %))]"\n"]
                                                                            [:chunk.chunk (str (t :user/Status ul) ": ")][:chunk  (capitalize (str (t (keyword (str "social/"(:status b))) ul) "  "))]

                                                                            [:chunk.chunk (str (t :badge/Verifiedbyobf ul) ": ")][:chunk (str (if (true? (:verified_by_obf b)) (t :core/Yes ul) (t :core/No ul)) "  ")]

                                                                            [:chunk.chunk (str (t :badge/Issuedbyobf ul) ": ")][:chunk (str (if (true? (:issued_by_obf b)) (t :core/Yes ul) (t :core/No ul)))]"\n"
                                                                            (when (not-empty (:tags b))
                                                                              [:phrase
                                                                               [:chunk.chunk (str (t :badge/Tags ul) ": ")] [:chunk (join ", " (:tags b))]"\n"])
                                                                            [:chunk.chunk (str (t :badge/Issuedon ul) ": ")][:chunk (if (number? (:issued_on b))(str (date-from-unix-time (long (* 1000 (:issued_on b))) "date") "  ") (str (:issued_on b) " "))]
                                                                            [:chunk.chunk (str (t :badge/Expireson ul) ": ")][:chunk (if (number? (:expires_on b)) (date-from-unix-time (long (* 1000 (:expires_on b))) "date") (:expires_on b))]"\n"
                                                                            [:chunk.chunk (str (t :badge/Issuerverified ul) ": ")] (if (== 0 (:issuer_verified b) ) [:chunk (str (t :core/No ul) " ")] [:chunk (str (t :core/Yes ul) " ")])
                                                                            [:chunk.chunk (str (t :badge/Revoked ul) ": ")] [:chunk (str (if (true? (:revoked b)) (t :core/Yes ul) (t :core/No ul)) "  ")]
                                                                            [:chunk.chunk (str (t :badge/Badgevisibility ul) ": ")] [:chunk (t (keyword (str "core/" (capitalize (:visibility b)))) ul)]"\n"
                                                                            [:chunk.chunk (str (t :badge/OBFurl ul) ": ")] [:chunk.link (:obf_url b)]"\n"
                                                                            [:chunk.chunk (str (t :badge/Assertionurl ul) ": ")] [:chunk.link (str (:assertion_url more-badge-info))]"\n"
                                                                            [:chunk.chunk (str (t :badge/Assertionjson ul) ": ")][:chunk.link (str (:assertion_json more-badge-info))]"\n"
                                                                            [:chunk.chunk (str (t :badge/Badgerating ul) ": ") ] [:chunk (str (:rating more-badge-info) "  ")]
                                                                            [:chunk.chunk (str (t :badge/Evidenceurl ul) ": ")] [:chunk.link (str (:evidence_url more-badge-info) " ")]"\n"
                                                                            (when (not-empty (:alignment %))
                                                                              [:paragraph
                                                                               [:chunk.chunk (str (t :badge/Alignments ul) ": " (count (:alignment %)))]"\n"
                                                                               (into [:paragraph ]
                                                                                     (for [a (:alignment %)]
                                                                                       [:paragraph
                                                                                        [:chunk (:name a)]"\n"
                                                                                        [:chunk (:description a)]"\n"
                                                                                        [:chunk.link (:url a)]
                                                                                        [:spacer 0]]))])
                                                                            [:chunk.chunk (str (t :badge/Viewed ul) ": ")] [:chunk (str (:view_count more-badge-info) " " (t :badge/times ul)" ")]", "
                                                                            [:chunk.chunk (str (t :badge/Recipientcount ul) ": ")][:chunk (str (:recipient_count more-badge-info) " ")]", "
                                                                            [:chunk.chunk (str (t :badge/Congratulated ul) "?: ")][:chunk (str (if (true? congratulated?) (t :core/Yes ul) (t :core/No ul))" ")]"\n"
                                                                            (when-not (empty? (:congratulations more-badge-info))
                                                                              [:paragraph
                                                                               [:chunk.chunk (str (t :badge/Congratulations ul) ": ")]"\n"
                                                                               [:paragraph
                                                                                (into [:paragraph] (for [c (:congratulations more-badge-info)]
                                                                                                     [:phrase
                                                                                                      [:chunk (str (:first_name c) " " (:last_name c))] "\n"]))]])
                                                                            (when (> (:endorsement_count %) 0) (blank? (str (:endorsement_count %)))
                                                                              [:phrase
                                                                               [:chunk.chunk (str (capitalize (t :badge/endorsements ul)) ": ")][:chunk (str (:endorsement_count %))]"\n"])
                                                                            (when (not-empty endorsements)
                                                                              (into [:paragraph {:indent 0}]
                                                                                    (for [e endorsements]
                                                                                      [:paragraph {:indent 0}
                                                                                       (:issuer_name e) "\n"
                                                                                       [:anchor {:target (:issuer_url e) :style{:family :times-roman :color [66 100 162]}} (:issuer_url e) ] "\n"
                                                                                       [:chunk (if (number? (:issued_on e)) (date-from-unix-time (long (* 1000 (:issued_on e)))) (:issued_on e))] "\n"
                                                                                       (process-markdown (:content e))])
                                                                                    ))
                                                                            (when (> (:all-messages message-count) 0)
                                                                              [:phrase
                                                                               [:chunk.chunk (str (t :social/Messages ul) ": ")] [:chunk (str (:all-messages message-count))]"\n"])
                                                                            (when (> (:all-messages message-count) 0)
                                                                              (into [:paragraph]
                                                                                    (for [m messages]
                                                                                      [:paragraph
                                                                                       [:chunk(:message m)]"\n"
                                                                                       [:chunk (str (:first_name m) " " (:last_name m))]"\n"
                                                                                       [:chunk (if (number? (:ctime m)) (date-from-unix-time (long (* 1000 (:ctime m))) "date") (:ctime m))]
                                                                                       [:spacer 0]])))

                                                                            [:chunk.chunk (str (t :social/Lastmodified ul) ": ")][:chunk (if (number? (:mtime b))(str (date-from-unix-time (long (* 1000 (:mtime b))) "date")"  ") (str (:mtime b) " "))]
                                                                            [:chunk.chunk (str (t :badge/URL ul) ": ")] [:chunk.link {:style :italic} (str site-url "/app/badge/info/" (:id b))]
                                                                            [:spacer 1]]])]
                                        ]
                                    (reduce into [:paragraph] (-> (mapv template content)
                                                                  (conj [[:line {:dotted true}]]))))))])

        pending-badges-template  (pdf/template
                                   [:paragraph.generic
                                    (when-not (empty? $pending_badges)
                                      [:paragraph
                                       [:heading.heading-name (t :badge/Pendingbadges ul)]
                                       [:spacer 0]
                                       (into [:paragraph]
                                             (for [pb $pending_badges]
                                               [:paragraph
                                                [:chunk.chunk (str (t :badge/BadgeID ul) ": ")] [:chunk (str (:badge_id pb))]"\n"
                                                [:chunk.chunk (str (t :badge/Name ul) ": ")] [:chunk (str (:name pb))]"\n"
                                                [:chunk.chunk (str (t :page/Description ul) ": ")] [:chunk  (str (:description pb))]"\n"
                                                [:chunk.chunk (str (t :badge/Imagefile ul) ": ")] [:chunk.link (str site-url "/" (:image_file pb)) ]"\n"
                                                [:chunk.chunk (str (t :badge/Assertionurl ul) ": ")] [:chunk.link (:assertion_url pb) ]"\n"
                                                (when (not-empty (:tags pb))
                                                  [:phrase
                                                   [:chunk.chunk (str (t :badge/Tags ul) ": ")] [:chunk (join ", " (:tags pb))]"\n"])
                                                [:chunk.chunk (str (t :badge/Badgevisibility ul) ": ")] [:chunk (t (keyword (str "core/" (capitalize (:visibility pb)))))]"\n"
                                                [:chunk.chunk (str (t :badge/Issuedon ul) ": ")] [:chunk (if (number? (:issued_on pb)) (str (date-from-unix-time (long (* 1000 (:issued_on pb))) "date") ", ") (str (:issued_on pb) ", "))]
                                                [:chunk.chunk (str (t :badge/Expireson ul) ": ")] [:chunk (if (number? (:expires_on pb)) (str (date-from-unix-time (long (* 1000 (:expires_on pb))) "date") ", ") (str (:expires_on pb) ", "))]
                                                ]))])])
        page-template (pdf/template
                        [:paragraph.generic
                         (when-not (empty? $user_pages)
                           [:paragraph
                            [:heading.heading-name (t :page/Mypages ul)]
                            [:spacer 0]
                            (into [:paragraph]
                                  (for [p $user_pages
                                        :let [ page-owner? (p/page-owner? ctx (:id p) user-id)
                                               page-blocks (p/page-blocks ctx (:id p))]]
                                    [:paragraph
                                     [:chunk.chunk (str (t :page/PageID ul) ": ")][:chunk (str (:id p))]"\n"
                                     [:chunk.chunk (str (t :badge/Name ul) ": ")][:chunk (str (:name p))]"\n"
                                     [:chunk.chunk (str (t :page/Owner ul) "?: ")][:chunk (if (= true page-owner?) (t :core/Yes ul) (t :core/No ul))] "\n"
                                     [:chunk.chunk (str (t :page/Pagepassword ul) ": ")][:chunk (str (:password p))]"\n"
                                     [:chunk.chunk (str (t :page/Description ul) ": ")][:chunk (str (:description p))]"\n"
                                     [:chunk.chunk (str (t :social/Created ul) ": ")][:chunk (str (if (number? (:ctime p)) (date-from-unix-time (long (* 1000 (:ctime p))) "date") (:ctime p)) "  ")]
                                     [:chunk.chunk (str (t :social/Lastmodified ul) ": ")][:chunk (str (if (number? (:mtime p))(date-from-unix-time (long (* 1000 (:mtime p))) "date") (:mtime p)))]"\n"
                                     (when (not-empty (:tags p))
                                       [:phrase
                                        [:chunk.chunk (str (t :badge/Tags ul) ": ")] [:chunk (join ", " (:tags p))]"\n"])
                                     [:chunk.chunk (str (t :page/Theme ul) ": ")][:chunk (str (:theme p) "  ")]
                                     [:chunk.chunk (str (t :page/Border ul) ": ")][:chunk (str (:border p) "  ")]
                                     [:chunk.chunk (str (t :page/Padding ul) ": ")][:chunk (str (:padding p))]"\n"
                                     [:spacer 1]
                                     (when (not-empty page-blocks)
                                       (into [:paragraph
                                              [:phrase.chunk (t :page/Pageblocks ul)]
                                              [:spacer 0]
                                              ] (for [pb page-blocks]
                                                  [:paragraph
                                                   (when (= "heading"  (:type pb))
                                                     (case (:size pb)
                                                       "h1" [:phrase.generic {:align :left }
                                                             [:chunk.chunk (str (t :page/Heading ul)": ")] [:chunk (str (:content pb))]
                                                             ]
                                                       "h2" [:phrase.generic {:align :left}
                                                             [:chunk.chunk (str (t :page/Subheading ul) ": ")] [:chunk (str (:content pb))]] ))
                                                   (when (= "badge" (:type pb))
                                                     [:phrase
                                                      [:phrase.chunk (str (t :badge/Badge ul) ": ")]
                                                      [:anchor {:target (str site-url "/app/badge/info/" (:badge_id pb))} [:chunk.link (str (:name pb))]]"\n"
                                                      ]
                                                     )
                                                   (when (= "html" (:type pb))
                                                     [:phrase
                                                      [:spacer 0]
                                                      [:phrase.chunk (str (t :page/Html ul) ": ")]
                                                      [:spacer 0]
                                                      (:content pb)])
                                                   (when (= "file" (:type pb))
                                                     [:paragraph
                                                      [:spacer 0]
                                                      [:phrase.chunk (str (t :file/Files ul) ": ")]"\n"
                                                      (into [:paragraph]
                                                            (for [file (:files pb)]
                                                              [:phrase
                                                               [:chunk.bold (str (t :badge/Name ul) ": ")] [:chunk (:name file)]"\n"
                                                               [:chunk.bold (str (t :badge/URL ul) ": ")][:anchor {:target (str site-url "/"(:path file)) :style{:family :times-roman :color [66 100 162]}} (str site-url "/"(:path file))]]
                                                              ))])
                                                   (when (= "tag" (:type pb))
                                                     [:paragraph
                                                      [:phrase.chunk (str (t :page/Badgegroup ul) ": ")]
                                                      [:spacer 0]
                                                      (into [:phrase ] (for [b (:badges pb)]
                                                                         [:phrase
                                                                          [:anchor {:target (str site-url "/app/badge/info/" (:id b))} [:chunk.link (str (:name b))]]"\n"]))])]
                                                  )))[:line {:dotted true}] [:spacer 0]]))])])
        user-connections-template (pdf/template
                                    [:paragraph.generic
                                     (when (or (not-empty $user_followers) (not-empty $user_following))
                                       [:paragraph
                                        [:heading.heading-name (str (t :user/Socialconnections ul) ": ")]
                                        [:spacer 0]
                                        (when-not (empty? $user_followers)
                                          (into [:paragraph
                                                 [:phrase.chunk (str (t :social/Followersusers ul) ": ")] [:spacer 0]] (for [follower $user_followers
                                                                                                                             :let [follower-id (:owner_id follower)
                                                                                                                                   fname (:first_name follower)
                                                                                                                                   lname (:last_name follower)
                                                                                                                                   status (:status follower)]]
                                                                                                                         [:paragraph
                                                                                                                          [:anchor {:target (str site-url "/app/user/profile/" follower-id)} [:chunk.link (str fname " " lname ",  ")]]
                                                                                                                          [:chunk.chunk (str (t :user/Status ul) ": ")] [:chunk (str (t (keyword (str "social/"status)) ul))]])))
                                        (when-not (empty? $user_following)
                                          (into [:paragraph
                                                 [:phrase.chunk (str (t :social/Followedusers ul) ": ")][:spacer 0]] (for [f $user_following
                                                                                                                           :let [followee-id (:user_id f)
                                                                                                                                 fname (:first_name f)
                                                                                                                                 lname (:last_name f)
                                                                                                                                 status (:status f)]]
                                                                                                                       [:paragraph
                                                                                                                        [:anchor {:target (str site-url "/app/user/profile/" followee-id)} [:chunk.link (str fname " " lname ", ")]]
                                                                                                                        [:chunk.chunk (str (t :user/Status ul) ": ")] [:chunk (str (t (keyword (str "social/"status)) ul))]])))
                                        ])])

        badge-connections-template (pdf/template
                                     [:paragraph.generic
                                      (when-not (empty? $connections)
                                        [:paragraph
                                         [:heading.heading-name (str (t :user/Badgeconnections ul) ": ")]
                                         [:spacer 0]
                                         (into [:paragraph]
                                               (for [c $connections]
                                                 [:paragraph
                                                  [:chunk.chunk (str (t :badge/BadgeID ul) ": ")] [:chunk (str (:id c))]"\n"
                                                  [:chunk.chunk (str (t :badge/Name ul) ": ")][:chunk (str (:name c))]"\n"
                                                  [:chunk.chunk (str (t :page/Description ul) ": ")][:chunk (str (:description c))]"\n"
                                                  [:chunk.chunk (str (t :badge/Imagefile ul) ": ")] [:chunk.link (str site-url "/" (:image_file c))]"\n"
                                                  ]))])])
        events-template (pdf/template
                          (let [template (cons [:paragraph]
                                               [[:paragraph.generic
                                                 (when-not (empty? $events)
                                                   [:heading.heading-name (t :user/Activityhistory ul)])]
                                                (when-not (empty? $events)
                                                  (into [:table.generic {:header [(t :social/Action ul) (t :social/Object ul) (t :social/Objecttype ul) (t :social/Created ul)]}]
                                                        (for [e (reverse $events)]
                                                          [[:cell (if-not (= "-" (:verb e)) (t (keyword (str "social/"(:verb e))) ul) "-")]
                                                           [:cell  [:phrase
                                                                    [:chunk (get-in e [:info :object_name])]"\n"
                                                                    (if (contains? (:info e) :message)
                                                                      [:chunk {:style :italic} (get-in e [:info :message :message])])]]
                                                           [:cell
                                                            (when-not (blank? (str (:type e)))
                                                              (cond
                                                                (= "page" (:type e)) (t :social/Emailpage ul)
                                                                (= "badge" (:type e)) (t :social/Emailbadge ul)
                                                                (= "user" (:type e)) (lower-case (t :social/User ul))
                                                                (= "admin" (:type e)) (lower-case (t :admin/Admin ul))
                                                                :else "-"))]
                                                           [:cell (if (number? (:ctime e))(date-from-unix-time (long (* 1000 (:ctime e))) "date") (:ctime e))]
                                                           ])))])]
                            template))]
    (fn [output-stream]
      (pdf/pdf (into [pdf-settings] (concat (user-info-template user-data)
                                            (email-template user-data)
                                            (profile-template user-data)
                                            (badge-template user-data)
                                            (pending-badges-template user-data)
                                            (page-template user-data)
                                            (user-connections-template user-data)
                                            (badge-connections-template user-data)
                                            (events-template user-data))) output-stream)
      )))


