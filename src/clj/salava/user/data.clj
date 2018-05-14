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
    [clojure.string :refer [ends-with? join capitalize]]
    [clojure.zip :as zip]
    [net.cgrand.enlive-html :as enlive]
    [salava.core.i18n :refer [t translate-text]]
    ))

(defqueries "sql/badge/main.sql")
(defqueries "sql/social/queries.sql")

(defn all-user-data [ctx user-id current-user-id]
  (let [all-user-info (u/user-information-and-profile ctx user-id current-user-id)
        email-addresses (u/email-addresses ctx current-user-id)
        user-badges (b/user-badges-all ctx current-user-id)
        user-pages (p/user-pages-all ctx current-user-id)
        user-files (f/user-files-all ctx  current-user-id)
        all-events (so/get-all-user-events ctx user-id)
        connections (so/get-connections-badge ctx current-user-id)
        pending-badges (b/user-badges-pending ctx user-id)
        user-followers-fn (first (util/plugin-fun (util/get-plugins ctx) "db" "get-user-followers-connections"))
        user-followers (if (fn? user-followers-fn) (user-followers-fn ctx user-id) "")
        user-following-fn (first (util/plugin-fun (util/get-plugins ctx) "db" "get-user-following-connections-user"))
        user-following (if (fn? user-followers-fn) (user-following-fn ctx user-id) "") ]


    (assoc all-user-info
      :emails email-addresses
      :user_badges user-badges
      :user_pages user-pages
      :user_files (:files user-files)
      :events all-events
      :connections connections
      :pending_badges pending-badges
      :user_followers user-followers
      :user_following user-following
      )))

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
        data-template (pdf/template
                        (let [template (cons [:paragraph]
                                             [[:heading.heading-name
                                               (str (:first_name $user) " " (:last_name $user))]
                                              [:spacer 2]
                                              [:paragraph.generic
                                               [:chunk.chunk (str (t :user/UserID)": ")] [:chunk (str (:id $user))]"\n"
                                               [:chunk.chunk (str (t :user/Role) ": ")] [:chunk (:role $user)]"\n"
                                               [:chunk.chunk (str (t :user/Firstname)": ")] [:chunk (:first_name $user)]"\n"
                                               [:chunk.chunk (str (t :user/Lastname)": ")][:chunk (:last_name $user)]"\n"
                                               (if-not (empty? (:profile_picture $user))
                                                 [:paragraph
                                                  [:chunk.chunk (str (t :user/Profilepicture)": ")] [:chunk (str site-url "/" (:profile_picture $user))]"\n"]
                                                 )
                                               [:chunk.chunk (str (t :user/Language)": ")][:chunk (str (:language $user) "  ")]
                                               [:chunk.chunk (str (t :user/Country)": ")][:chunk (:country $user)]"\n"
                                               [:chunk.chunk (str (t :user/Activated) ": ")][:chunk (str (:activated $user) "  ")]
                                               [:chunk.chunk (str (t :user/Emailnotifications) ": ")][:chunk (str (:email_notifications $user) "  ")]
                                               [:chunk.chunk (str (t :user/Profilevisibility) ": ")][:chunk (:profile_visibility $user)]"\n"
                                               [:chunk.chunk (str (t :user/Aboutme) ":")]"\n"
                                               [:paragraph (:about $user)]]
                                              [:spacer 2]
                                              [:paragraph.generic
                                               (if (> (count $emails) 1)
                                                 [:heading.heading-name (t :user/Emailaddresses)]
                                                 [:heading.heading-name (t :user/Emailaddress)]
                                                 )
                                               (into [:paragraph ]
                                                     (for [e $emails
                                                           :let [primary-address (:primary_address e)]]
                                                       [:paragraph
                                                        [:chunk.chunk (str (t :user/Emailaddress)": ")] [:chunk (:email e)]"\n"
                                                        [:chunk.chunk (str (t :social/Created) ": ")] [:chunk (str (date-from-unix-time (long (* 1000 (:ctime e))) "date")"  ")]
                                                        [:chunk.chunk (str (t :user/verified)": ")] [:chunk (str (:verified e) "  ")]
                                                        (when (= true primary-address)
                                                          [:phrase
                                                           [:chunk.chunk (str (t :user/Loginaddress)": ")] [:chunk (str (:primary_address e))] "\n"]
                                                          )
                                                        (when (not-empty (:backpack_id e))
                                                          [:chunk.chunk (str (t :user/BackpackID) ": ") (:backpack_id e)])

                                                        ]))]

                                              (when (not-empty $profile)
                                                [:paragraph.generic
                                                 [:heading.heading-name  (t :user/Myprofile)]
                                                 (into [:paragraph ] (for [p $profile]
                                                                       [:phrase
                                                                        [:chunk.chunk (capitalize (str (:field p) ": "))] [:chunk (str (:value p) "  ")]]
                                                                       ))

                                                 ])
                                              (when-not (empty? $user_badges)
                                                [:paragraph.generic
                                                 [:spacer 0]
                                                 [:heading.heading-name (t :badge/Badges)]
                                                 (into [:paragraph ] (for [b $user_badges
                                                                           :let [more-badge-info (b/get-badge ctx (:id b) user-id)
                                                                                 content (:content more-badge-info)
                                                                                 congratulated? (:congratulated? more-badge-info)
                                                                                 message-count (so/get-badge-message-count ctx (:badge_id b) user-id)
                                                                                 messages (select-badge-messages {:badge_id (:badge_id b)} (util/get-db ctx))
                                                                                 endorsements (select-badge-endorsements {:id (:badge_id b)} (util/get-db ctx))
                                                                                 template #(cons [:paragraph][[:paragraph
                                                                                                               [:chunk.chunk (str (t :badge/BadgeID) ": ")][:chunk (:badge_id b )]"\n"
                                                                                                               [:chunk.chunk (str (t :badge/Name) ": ") ][:chunk (:name %)]"\n"
                                                                                                               [:chunk.chunk (str (t :page/Description) ": ")][:chunk (:description %)]"\n"
                                                                                                               [:chunk.chunk (str (t :badge/Imagefile) ": ")][:chunk (str site-url "/"(:image_file %))]"\n"
                                                                                                               [:chunk.chunk (str (t :badge/Issuedby) ": ")] [:chunk (str (:issuer_content_name %)"  ")]
                                                                                                               [:chunk.chunk (str (t :badge/Issuerurl) ": ")][:chunk (:issuer_content_url %)]"\n"
                                                                                                               (when (not-empty (:issuer_contact %))
                                                                                                                 [:phrase
                                                                                                                  [:chunk.chunk (str (t :badge/Issuercontact) ": ")] [:chunk (:issuer_contact %)]"\n"])
                                                                                                               (when (not-empty (:creator_name %))
                                                                                                                 [:phrase
                                                                                                                  [:chunk.chunk (str (t :badge/Createdby) ": ")][:chunk (:creator_name %)]"\n"])
                                                                                                               (when (not-empty (:creator_url %))
                                                                                                                 [:phrase
                                                                                                                  [:chunk.chunk (str (t :badge/Creatorurl) ": ")][:chunk (:creator_url %)]"\n"])
                                                                                                               (when (not-empty (:creator_email %))
                                                                                                                 [:phrase
                                                                                                                  [:chunk.chunk (str (t :badge/Creatorcontact) ": ")][:chunk (:creator_email %)]"\n"])
                                                                                                               [:chunk.chunk (str (t :badge/Criteriaurl) ": ") ][:chunk.link (:criteria_url %)]"\n"
                                                                                                               [:chunk.chunk (str (t :badge/Criteria) ": ")][:paragraph (strip-html-tags (:criteria_content %))]"\n"
                                                                                                               [:chunk.chunk (str (t :user/Status) ": ")][:chunk  (str (:status b) "  ")]
                                                                                                               [:chunk.chunk (str (t :badge/Verifiedbyobf) ": ")][:chunk (str (:verified_by_obf b) "  ")]
                                                                                                               [:chunk.chunk (str (t :badge/Issuedbyobf) ": ")][:chunk (str (:issued_by_obf b))]"\n"
                                                                                                               (when (not-empty (:tags b))
                                                                                                                 [:phrase
                                                                                                                  [:chunk.chunk (str (t :badge/Tags) ": ")] [:chunk (join ", " (:tags b))]"\n"])
                                                                                                               [:chunk.chunk (str (t :badge/Issuedon) ": ")][:chunk (str (date-from-unix-time (long (* 1000 (:issued_on b))) "date") "  ")]
                                                                                                               (when (:expires_on b)
                                                                                                                 [:phrase
                                                                                                                  [:chunk.chunk (str (t :badge/Expireson) ": ")][:chunk (date-from-unix-time (long (* 1000 (:expires_on b))) "date")]"\n"])
                                                                                                               [:chunk.chunk (str (t :badge/Issuerverified) ": ")] (if (== 0 (:issuer_verified b) ) [:chunk "false  "] [:chunk "true  "])
                                                                                                               [:chunk.chunk (str (t :badge/Revoked) ": ")] [:chunk (str (:revoked b) "  ")]
                                                                                                               [:chunk.chunk (str (t :badge/Badgevisibility) ": ")] [:chunk (:visibility b)]"\n"

                                                                                                               (when-not (nil? (:obf_url b))
                                                                                                                 [:phrase
                                                                                                                  [:chunk.chunk (str (t :badge/OBFurl) ": ")] [:chunk (:obf_url b)]"\n"])
                                                                                                               [:chunk.chunk (str (t :badge/Assertionurl) ": ")] [:chunk.link (:assertion_url more-badge-info)]"\n"
                                                                                                               [:chunk.chunk (str (t :badge/Assertionjson) ": ")][:chunk.link (:assertion_json more-badge-info)]"\n"

                                                                                                               (when-not (nil? (:rating more-badge-info))
                                                                                                                 [:phrase
                                                                                                                  [:chunk.chunk (str (t :badge/Badgerating) ": ") ] [:chunk (str (:rating more-badge-info) "  ")]])
                                                                                                               (when-not (nil? (:evidence_url more-badge-info))
                                                                                                                 [:phrase
                                                                                                                  [:chunk.chunk (str (t :badge/Evidenceurl) ": ")] [:chunk (str (:evidence_url more-badge-info) " ")]]"\n"
                                                                                                                 )
                                                                                                               (when (not-empty (:alignment %))
                                                                                                                 [:paragraph
                                                                                                                  [:chunk.chunk (str (t :badge/Alignments) ": ")]"\n"
                                                                                                                  (into [:paragraph ]
                                                                                                                        (for [a (:alignment %)]
                                                                                                                          [:paragraph
                                                                                                                           [:chunk.chunk (str (t :badge/Name) ": ")] [:chunk (:name a)]"\n"
                                                                                                                           [:chunk.chunk (str (t :page/Description) ": ")] [:chunk (:description a)]"\n"
                                                                                                                           [:chunk.chunk (str (t :badge/Url) ": ")] [:chunk.link (:url a)]]))
                                                                                                                  ])

                                                                                                               [:chunk.chunk (str (t :badge/Viewed) ": ")] [:chunk (str (:view_count more-badge-info) " " (t :badge/times)" ")]", "
                                                                                                               [:chunk.chunk (str (t :badge/Recipientcount) ": ")][:chunk (str (:recipient_count more-badge-info) " ")]", "
                                                                                                               [:chunk.chunk (str (t :badge/Congratulated) "?: ")][:chunk (str congratulated?  #_(:congratulated? more-badge-info ) " ")]"\n"
                                                                                                               (when (= true  congratulated?
                                                                                                                        [:paragraph
                                                                                                                         (into [:paragraph] (for [c (:congratulations more-badge-info)]
                                                                                                                                              [:chunk (str c)]))]))
                                                                                                               [:chunk.chunk (str (capitalize (t :badge/endorsements)) ": ")][:chunk (str (:endorsement_count %))]"\n"
                                                                                                               (when (not-empty endorsements)
                                                                                                                 (into [:paragraph {:indent 0}]
                                                                                                                       (for [e endorsements]
                                                                                                                         [:paragraph {:indent 0}
                                                                                                                          #_[:chunk.chunk "Endorser: " ] (:issuer_name e) "\n"
                                                                                                                          #_[:chunk.chunk "Issuer url: "] [:anchor {:target (:issuer_url e) :style{:family :times-roman :color [66 100 162]}} (:issuer_url e)] "\n"
                                                                                                                          #_[:chunk.chunk "Issued on: "][:chunk (date-from-unix-time (long (* 1000 (:issued_on e))))] "\n"
                                                                                                                          (markdown->clj-pdf (:content e))])
                                                                                                                       ))
                                                                                                               [:chunk.chunk (str (t :social/Messages) ": ")] [:chunk (str (:all-messages message-count))]"\n"
                                                                                                               (when (> (:all-messages message-count) 0)
                                                                                                                 (into [:paragraph]
                                                                                                                       (for [m messages]
                                                                                                                         [:paragraph
                                                                                                                          [:chunk (:message m)]"\n"
                                                                                                                          [:chunk (str (:first_name m) " " (:last_name m))]"\n"
                                                                                                                          [:chunk (date-from-unix-time (long (* 1000 (:ctime m))) "date")]
                                                                                                                          [:spacer 0]])))
                                                                                                               [:chunk.chunk (str (t :social/Lastmodified) ": ")][:chunk (str (date-from-unix-time (long (* 1000 (:mtime b))) "date")"  ")]
                                                                                                               [:chunk.chunk (str (t :badge/URL) ": ")] [:chunk.link {:style :italic} (str site-url "/badge/info/" (:id b))]
                                                                                                               [:spacer 1]]])]
                                                                           ]
                                                                       (reduce into [:paragraph] (-> (mapv template content)
                                                                                                     (conj [[:line {:dotted true}]])))
                                                                       ))])

                                              (when (not-empty $pending_badges)
                                                [:paragraph.generic
                                                 [:heading.heading-name "Pending Badges"]
                                                 (into [:paragraph] (for [pb $pending_badges]
                                                                      [:paragraph
                                                                       [:chunk.chunk (str (t :badge/BadgeID) ": ")] [:chunk (str (:badge_id pb))]"\n"
                                                                       [:chunk.chunk (str (t :badge/Name) ": ")] [:chunk (:name pb)]"\n"
                                                                       [:chunk.chunk (str (t :page/Description) ": ")] [:chunk (:description pb)]"\n"
                                                                       [:chunk.chunk (str (t :badge/Imagefile) ": ")] [:chunk.link (str site-url "/" (:image_file pb)) ]"\n"
                                                                       [:chunk.chunk (str (t :badge/Assertionurl) ": ")] [:chunk.link (:assertion_url pb)]"\n"
                                                                       (when (not-empty (:tags pb))
                                                                         [:phrase
                                                                          [:chunk.chunk (str (t :badge/Tags) ": ")] [:chunk (join ", " (:tags pb))]"\n"])
                                                                       [:chunk.chunk (str (t :badge/Badgevisibility) ": ")] [:chunk (:visibility pb)]"\n"
                                                                       [:chunk.chunk (str (t :badge/Issuedon) ": ")] [:chunk (str (date-from-unix-time (long (* 1000 (:issued_on pb))) "date") ", ")]
                                                                       (when (:expires_on pb)
                                                                         [:phrase
                                                                          [:chunk.chunk (str (t :badge/Expireson) ": ")] [:chunk (str (date-from-unix-time (long (* 1000 (:expires_on pb))) "date") "")]])
                                                                       [:spacer 0]]))])

                                              (when (not-empty $user_pages)
                                                [:paragraph.generic
                                                 [:heading.heading-name "Pages"]
                                                 (into [:paragraph ] (for [p $user_pages
                                                                           :let [ page-owner? (p/page-owner? ctx (:id p) user-id)
                                                                                  page-blocks (p/page-blocks ctx (:id p))]]
                                                                       [:paragraph
                                                                        [:chunk.chunk (str (t :page/PageID) ": ")][:chunk (str (:id p))]"\n"
                                                                        [:chunk.chunk (str (t :badge/Name) ": ")][:chunk (:name p)]"\n"
                                                                        [:chunk.chunk (str (t :page/Owner) "?: ")][:chunk (str page-owner?)] "\n"
                                                                        (when (and (:password p) (not-empty (:password p)))
                                                                          [:phrase
                                                                           [:chunk.chunk (str (t :page/Pagepassword) ": ")][:chunk (:password p)]"\n"])
                                                                        [:chunk.chunk (str (t :page/Description) ": ")][:chunk (:description p)]"\n"
                                                                        [:chunk.chunk (str (t :social/Created) ": ")][:chunk (str (date-from-unix-time (long (* 1000 (:ctime p))) "date") "  ")]
                                                                        [:chunk.chunk (str (t :social/Lastmodified) ": ")][:chunk (str (date-from-unix-time (long (* 1000 (:mtime p))) "date"))]"\n"
                                                                        (when (not-empty (:tags p))
                                                                          [:phrase
                                                                           [:chunk.chunk (str (t :badge/Tags) ": ")] [:chunk (join ", " (:tags p))]"\n"])
                                                                        [:chunk.chunk (str (t :page/Theme) ": ")][:chunk (str (:theme p) "  ")]
                                                                        [:chunk.chunk (str (t :page/Border) ": ")][:chunk (str (:border p) "  ")]
                                                                        [:chunk.chunk (str (t :page/Padding) ": ")][:chunk (str (:padding p))]"\n"
                                                                        [:spacer 1]
                                                                        (when (not-empty page-blocks)
                                                                          (into [:paragraph
                                                                                 [:phrase.chunk (t :page/Pageblocks)]
                                                                                 [:spacer 0]
                                                                                 ] (for [pb page-blocks]
                                                                                     [:paragraph
                                                                                      (when (= "heading"  (:type pb))
                                                                                        (case (:size pb)
                                                                                          "h1" [:phrase.generic {:align :left }
                                                                                                #_[:spacer 0]
                                                                                                [:chunk.chunk (str (t :page/Heading)": ")] [:chunk (:content pb)]
                                                                                                ]
                                                                                          "h2" [:phrase.generic {:align :left}
                                                                                                #_[:spacer 0]
                                                                                                [:chunk.chunk (str (t :page/Subheading) ": ")] [:chunk (:content pb)]] ))
                                                                                      (when (= "badge" (:type pb))
                                                                                        [:phrase
                                                                                         [:phrase.chunk (str (t :badge/Badge) ": ")]
                                                                                         [:anchor {:target (str site-url "/badge/info/" (:badge_id pb))} [:chunk.link (:name pb)]]"\n"
                                                                                         ]
                                                                                        )
                                                                                      (when (= "html" (:type pb))
                                                                                        [:phrase
                                                                                         [:spacer 0]
                                                                                         [:phrase.chunk (str (t :page/HTML) ": ")]
                                                                                         [:spacer 0]
                                                                                         (:content pb)])
                                                                                      (when (= "file" (:type pb))
                                                                                        [:paragraph
                                                                                         [:spacer 0]
                                                                                         [:phrase.chunk (str (t :file/Files) ": ")]"\n"
                                                                                         (into [:paragraph]
                                                                                               (for [file (:files pb)]
                                                                                                 [:phrase
                                                                                                  [:chunk.bold (str (t :badge/Name) ": ")] [:chunk (:name file)]"\n"
                                                                                                  [:chunk.bold (str (t :badge/URL) ": ")][:anchor {:target (str site-url "/"(:path file)) :style{:family :times-roman :color [66 100 162]}} (str site-url "/"(:path file))]]
                                                                                                 ))])
                                                                                      (when (= "tag" (:type pb))
                                                                                        [:paragraph
                                                                                         [:phrase.chunk (str (t :page/Badgegroup) ": ")]
                                                                                         [:spacer 0]
                                                                                         (into [:phrase ] (for [b (:badges pb)]
                                                                                                            [:phrase
                                                                                                             [:anchor {:target (str site-url "/badge/info/" (:id b))} [:chunk.link (:name b)]]"\n"]))
                                                                                         ]
                                                                                        )
                                                                                      ]
                                                                                     )))]))])

                                              (when (or (not-empty $user_followers) (not-empty $user_following))
                                                [:paragraph.generic
                                                 [:heading.heading-name (str (t :user/Socialconnections) ": ")]
                                                 [:spacer 0]
                                                 (when-not (empty? $user_followers)
                                                   (into [:paragraph
                                                          [:phrase.chunk (str (t :social/Followerusers) ": ")] [:spacer 0]] (for [follower $user_followers
                                                                                                           :let [follower-id (:owner_id follower)
                                                                                                                 fname (:first_name follower)
                                                                                                                 lname (:last_name follower)
                                                                                                                 status (:status follower)]]
                                                                                                       [:paragraph
                                                                                                        [:anchor {:target (str site-url "/" "user/profile/" follower-id)} [:chunk.link (str fname " " lname ",  ")]]
                                                                                                        [:chunk.chunk (str (t :user/Status) ": ")] [:chunk status]])))
                                                 (when-not (empty? $user_following)
                                                   (into [:paragraph
                                                          [:phrase.chunk (str (t :social/Followedusers) ": ")][:spacer 0]] (for [f $user_following
                                                                                                          :let [followee-id (:user_id f)
                                                                                                                fname (:first_name f)
                                                                                                                lname (:last_name f)
                                                                                                                status (:status f)]]
                                                                                                      [:paragraph
                                                                                                       [:anchor {:target (str site-url "/" "user/profile/" followee-id)} [:chunk.link (str fname " " lname ", ")]]
                                                                                                       [:chunk.chunk (str (t :user/Status) ": ")] [:chunk status]])))
                                                 ])

                                              (when-not (empty? $connections)
                                                [:paragraph.generic
                                                 [:heading.heading-name (str (t :user/Badgeconnections) ": ")]
                                                 [:spacer 0]
                                                 (into [:paragraph]
                                                       (for [c $connections]
                                                         [:paragraph
                                                          [:chunk.chunk (str (t :badge/BadgeID) ": ")] [:chunk (str (:id c))]"\n"
                                                          [:chunk.chunk (str (t :badge/Name) ": ")][:chunk (:name c)]"\n"
                                                          [:chunk.chunk (str (t :page/Description) ": ")][:chunk (:description c)]"\n"
                                                          [:chunk.chunk (str (t :badge/Imagefile) ": ")] [:chunk.link (str site-url "/"(:image_file c))]"\n"
                                                          ]))])

                                              (when-not (empty? $events)
                                                [:paragraph.generic
                                                 [:heading.heading-name (t :user/Activityhistory)]
                                                 ])

                                              (when-not (empty? $events)
                                                (into [:table.generic {:header [(t :social/Action) (t :social/Object) (t :social/Objecttype) (t :social/Created)]}]
                                                      (for [e (reverse $events)]
                                                        [[:cell (:verb e)]
                                                         [:cell  (:object e)]
                                                         [:cell (:type e)]
                                                         [:cell (date-from-unix-time (long (* 1000 (:ctime e))) "date")]
                                                         ])))
                                              ])]
                          (into [] template)))]

    (fn [output-stream]
      (pdf/pdf (into [pdf-settings] (data-template user-data)) output-stream)
      )))


