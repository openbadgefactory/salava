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
        user-followers (user-followers-fn ctx user-id)
        user-following-fn (first (util/plugin-fun (util/get-plugins ctx) "db" "get-user-following-connections-user"))
        user-following (user-following-fn ctx user-id)
        ]

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
      )
    ))

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
                                               [:chunk.chunk "ID: "] [:chunk (str (:id $user))]"\n"
                                               [:chunk.chunk "Role:"] [:chunk (:role $user)]"\n"
                                               [:chunk.chunk "First name: "] [:chunk (:first_name $user)]"\n"
                                               [:chunk.chunk "Last name: "][:chunk (:last_name $user)]"\n"
                                               (if-not (empty? (:profile_picture $user))
                                                 [:paragraph
                                                  [:chunk.chunk "Profile picture: "] [:chunk (str site-url "/" (:profile_picture $user))]"\n"]
                                                 )
                                               [:chunk.chunk "Language: "][:chunk (str (:language $user) "  ")]
                                               [:chunk.chunk "Country: "][:chunk (:country $user)]"\n"
                                               [:chunk.chunk "Activated: "][:chunk (str (:activated $user) "  ")]
                                               [:chunk.chunk "Email Notifications? "][:chunk (str (:email_notifications $user) "  ")]
                                               [:chunk.chunk "Profile Visibility: "][:chunk (:profile_visibility $user)]"\n"
                                               [:chunk.chunk "About: "]"\n"
                                               [:paragraph (:about $user)]]
                                              [:spacer 2]
                                              [:paragraph.generic
                                               (if (> (count $emails) 1)
                                                 [:heading.heading-name "Emails"]
                                                 [:heading.heading-name "Email"]
                                                 )
                                               (into [:paragraph ]
                                                     (for [e $emails
                                                           :let [primary-address (:primary_address e)]]
                                                       [:paragraph
                                                        [:chunk.chunk "Email Address: "] [:chunk (:email e)]"\n"
                                                        [:chunk.chunk "Created: "] [:chunk (str (date-from-unix-time (long (* 1000 (:ctime e))) "date")"  ")]
                                                        [:chunk.chunk "Verified: "] [:chunk (str (:verified e) "  ")]
                                                        (when (= true primary-address)
                                                          [:phrase
                                                           [:chunk.chunk "Primary Address: "] [:chunk (str (:primary_address e))] "\n"]
                                                          )
                                                        (when (not-empty (:backpack_id e))
                                                          [:chunk.chunk "Backpack id: " (:backpack_id e)])

                                                        ]))]

                                              (when (not-empty $profile)
                                                [:paragraph.generic
                                                 [:heading.heading-name "Profile"]
                                                 (into [:paragraph ] (for [p $profile]
                                                                       [:phrase
                                                                        [:chunk.chunk (capitalize (str (:field p) ": "))] [:chunk (str (:value p) "  ")]]
                                                                       ))

                                                 ])
                                              (when-not (empty? $user_badges)
                                                [:paragraph.generic
                                                 [:spacer 0]
                                                 [:heading.heading-name "Badges"]
                                                 (into [:paragraph ] (for [b $user_badges
                                                                           :let [more-badge-info (b/get-badge ctx (:id b) user-id)
                                                                                 content (:content more-badge-info)
                                                                                 congratulated? (:congratulated? more-badge-info)
                                                                                 message-count (so/get-badge-message-count ctx (:badge_id b) user-id)
                                                                                 messages (select-badge-messages {:badge_id (:badge_id b)} (util/get-db ctx))
                                                                                 endorsements (select-badge-endorsements {:id (:badge_id b)} (util/get-db ctx))
                                                                                 template #(cons [:paragraph][[:paragraph
                                                                                                               [:chunk.chunk "Badge-id: "][:chunk (:badge_id b )]"\n"
                                                                                                               [:chunk.chunk "Name: " ][:chunk (:name %)]"\n"
                                                                                                               [:chunk.chunk "Description: "][:chunk (:description %)]"\n"
                                                                                                               [:chunk.chunk "Image file: "][:chunk (str site-url "/"(:image_file %))]"\n"
                                                                                                               [:chunk.chunk "Issuer: "] [:chunk (str (:issuer_content_name %)"  ")]
                                                                                                               [:chunk.chunk "Issuer Url: "][:chunk (:issuer_content_url %)]"\n"
                                                                                                               (when (not-empty (:issuer_contact %))
                                                                                                                 [:phrase
                                                                                                                  [:chunk.chunk "Issuer Contact: "] [:chunk (:issuer_contact %)]"\n"])
                                                                                                               (when (not-empty (:creator_name %))
                                                                                                                 [:phrase
                                                                                                                  [:chunk.chunk "Creator: "][:chunk (:creator_name %)]"\n"])
                                                                                                               (when (not-empty (:creator_url %))
                                                                                                                 [:phrase
                                                                                                                  [:chunk.chunk "Creator-url: "][:chunk (:creator_url %)]"\n"])
                                                                                                               (when (not-empty (:creator_email %))
                                                                                                                 [:phrase
                                                                                                                  [:chunk.chunk "Creator: "][:chunk (:creator_email %)]"\n"])
                                                                                                               [:chunk.chunk "Criteria-url: " ][:chunk.link (:criteria_url %)]"\n"
                                                                                                               [:chunk.chunk "Criteria: "][:paragraph (strip-html-tags (:criteria_content %))]"\n"
                                                                                                               [:chunk.chunk "Status: "][:chunk  (str (:status b) "  ")]
                                                                                                               [:chunk.chunk "Verified by obf: "][:chunk (str (:verified_by_obf b) "  ")]
                                                                                                               [:chunk.chunk "Issued by obf: "][:chunk (str (:issued_by_obf b))]"\n"
                                                                                                               (when (not-empty (:tags b))
                                                                                                                 [:phrase
                                                                                                                  [:chunk.chunk "Tags: "] [:chunk (join ", " (:tags b))]"\n"])
                                                                                                               [:chunk.chunk "Issued on: "][:chunk (str (date-from-unix-time (long (* 1000 (:issued_on b))) "date") "  ")]
                                                                                                               (when (:expires_on b)
                                                                                                                 [:phrase
                                                                                                                  [:chunk.chunk "Expires on: "][:chunk (date-from-unix-time (long (* 1000 (:expires_on b))) "date")]"\n"])
                                                                                                               [:chunk.chunk "Issuer Verified:  "] (if (== 0 (:issuer_verified b) ) [:chunk "false  "] [:chunk "true  "])
                                                                                                               [:chunk.chunk "Revoked: "] [:chunk (str (:revoked b) "  ")]
                                                                                                               [:chunk.chunk "Visibility: "] [:chunk (:visibility b)]"\n"

                                                                                                               (when-not (nil? (:obf_url b))
                                                                                                                 [:phrase
                                                                                                                  [:chunk.chunk "Obf url: "] [:chunk (:obf_url b)]"\n"])
                                                                                                               [:chunk.chunk "Assertion-url: "] [:chunk.link (:assertion_url more-badge-info)]"\n"
                                                                                                               [:chunk.chunk "Assertion-json: "][:chunk.link (:assertion_json more-badge-info)]"\n"

                                                                                                               (when-not (nil? (:rating more-badge-info))
                                                                                                                 [:phrase
                                                                                                                  [:chunk.chunk "Rating: " ] [:chunk (str (:rating more-badge-info) "  ")]])
                                                                                                               (when-not (nil? (:evidence_url more-badge-info))
                                                                                                                 [:phrase
                                                                                                                  [:chunk.chunk "Evidence-url: "] [:chunk (str (:evidence_url more-badge-info) " ")]]"\n"
                                                                                                                 )
                                                                                                               (when (not-empty (:alignment %))
                                                                                                                 [:paragraph
                                                                                                                  [:chunk.chunk "Alignments: "]"\n"
                                                                                                                  (into [:paragraph ]
                                                                                                                        (for [a (:alignment %)]
                                                                                                                          [:paragraph
                                                                                                                           [:chunk.chunk "Name: "] [:chunk (:name a)]"\n"
                                                                                                                           [:chunk.chunk "Description: "] [:chunk (:description a)]"\n"
                                                                                                                           [:chunk.chunk "Url: "] [:chunk.link (:url a)]]))
                                                                                                                  ])

                                                                                                               [:chunk.chunk "View count: "] [:chunk (str (:view_count more-badge-info) " ")]", "
                                                                                                               [:chunk.chunk "Recipient count: "][:chunk (str (:recipient_count more-badge-info) " ")]", "
                                                                                                               [:chunk.chunk "Congratulated "][:chunk (str congratulated?  #_(:congratulated? more-badge-info ) " ")]"\n"
                                                                                                               (when (= true  congratulated?
                                                                                                                        [:paragraph
                                                                                                                         (into [:paragraph] (for [c (:congratulations more-badge-info)]
                                                                                                                                              [:chunk (str c)]))]))
                                                                                                               [:chunk.chunk "Endorsement-count: "][:chunk (str (:endorsement_count %))]"\n"
                                                                                                               (when (not-empty endorsements)
                                                                                                                 (into [:paragraph {:indent 0}]
                                                                                                                       (for [e endorsements]
                                                                                                                         [:paragraph {:indent 0}
                                                                                                                          #_[:chunk.chunk "Endorser: " ] (:issuer_name e) "\n"
                                                                                                                          #_[:chunk.chunk "Issuer url: "] [:anchor {:target (:issuer_url e) :style{:family :times-roman :color [66 100 162]}} (:issuer_url e)] "\n"
                                                                                                                          #_[:chunk.chunk "Issued on: "][:chunk (date-from-unix-time (long (* 1000 (:issued_on e))))] "\n"
                                                                                                                          (markdown->clj-pdf (:content e))])
                                                                                                                       ))
                                                                                                               [:chunk.chunk "Message count: "] [:chunk (str (:all-messages message-count))]"\n"
                                                                                                               (when (> (:all-messages message-count) 0)
                                                                                                                 (into [:paragraph]
                                                                                                                       (for [m messages]
                                                                                                                         [:paragraph
                                                                                                                          [:chunk (:message m)]"\n"
                                                                                                                          [:chunk (str (:first_name m) " " (:last_name m))]"\n"
                                                                                                                          [:chunk (date-from-unix-time (long (* 1000 (:ctime m))) "date")]
                                                                                                                          [:spacer 0]])))
                                                                                                               [:chunk.chunk "Last Modified: "][:chunk (str (date-from-unix-time (long (* 1000 (:mtime b))) "date")"  ")]
                                                                                                               [:chunk.chunk "Badge Url: "] [:chunk.link {:style :italic} (str site-url "/badge/info/" (:id b))]
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
                                                                       [:chunk.chunk "Badge ID: "] [:chunk (str (:badge_id pb))]"\n"
                                                                       [:chunk.chunk "Badge name: "] [:chunk (:name pb)]"\n"
                                                                       [:chunk.chunk "Description: "] [:chunk (:description pb)]"\n"
                                                                       [:chunk.chunk "Image file: "] [:chunk.link (str site-url "/" (:image_file pb)) ]"\n"
                                                                       [:chunk.chunk "Assertion-url: "] [:chunk.link (:assertion_url pb)]"\n"
                                                                       (when (not-empty (:tags pb))
                                                                         [:phrase
                                                                          [:chunk.chunk "Tags: "] [:chunk (join ", " (:tags pb))]"\n"])
                                                                       [:chunk.chunk "Visibility: "] [:chunk (:visibility pb)]"\n"
                                                                       [:chunk.chunk "Issued on: "] [:chunk (str (date-from-unix-time (long (* 1000 (:issued_on pb))) "date") ", ")]
                                                                       (when (:expires_on pb)
                                                                         [:phrase
                                                                          [:chunk.chunk "Expires on: "] [:chunk (str (date-from-unix-time (long (* 1000 (:expires_on pb))) "date") "")]])
                                                                       [:spacer 0]]))])

                                              (when (not-empty $user_pages)
                                                [:paragraph.generic
                                                 [:heading.heading-name "Pages"]
                                                 (into [:paragraph ] (for [p $user_pages
                                                                           :let [ page-owner? (p/page-owner? ctx (:id p) user-id)
                                                                                  page-blocks (p/page-blocks ctx (:id p))]]
                                                                       [:paragraph
                                                                        [:chunk.chunk "Page-id: "][:chunk (str (:id p))]"\n"
                                                                        [:chunk.chunk "Name: "][:chunk (:name p)]"\n"
                                                                        [:chunk.chunk "Owner?: "][:chunk (str page-owner?)] "\n"
                                                                        (when (and (:password p) (not-empty (:password p)))
                                                                          [:phrase
                                                                           [:chunk.chunk "Password: "][:chunk (:password p)]"\n"])
                                                                        [:chunk.chunk "Description: "][:chunk (:description p)]"\n"
                                                                        [:chunk.chunk "Created on: "][:chunk (str (date-from-unix-time (long (* 1000 (:ctime p))) "date") "  ")]
                                                                        [:chunk.chunk "Last Modified: "][:chunk (str (date-from-unix-time (long (* 1000 (:mtime p))) "date"))]"\n"
                                                                        (when (not-empty (:tags p))
                                                                          [:phrase
                                                                           [:chunk.chunk "Tags"] [:chunk (join ", " (:tags p))]"\n"])
                                                                        [:chunk.chunk "Theme: "][:chunk (str (:theme p) "  ")]
                                                                        [:chunk.chunk "Border: "][:chunk (str (:border p) "  ")]
                                                                        [:chunk.chunk "Padding: "][:chunk (str (:padding p))]"\n"
                                                                        [:spacer 1]
                                                                        (when (not-empty page-blocks)
                                                                          (into [:paragraph
                                                                                 [:phrase.chunk "Page blocks"]
                                                                                 [:spacer 0]
                                                                                 ] (for [pb page-blocks]
                                                                                     [:paragraph
                                                                                      (when (= "heading"  (:type pb))
                                                                                        (case (:size pb)
                                                                                          "h1" [:phrase.generic {:align :left }
                                                                                                #_[:spacer 0]
                                                                                                [:chunk.chunk "Heading: "] [:chunk (:content pb)]
                                                                                                ]
                                                                                          "h2" [:phrase.generic {:align :left}
                                                                                                #_[:spacer 0]
                                                                                                [:chunk.chunk "Sub-heading: "] [:chunk (:content pb)]] ))
                                                                                      (when (= "badge" (:type pb))
                                                                                        [:phrase
                                                                                         [:phrase.chunk "Badge: "]
                                                                                         [:anchor {:target (str site-url "/badge/info/" (:badge_id pb))} [:chunk.link (:name pb)]]"\n"
                                                                                         ]
                                                                                        )
                                                                                      (when (= "html" (:type pb))
                                                                                        [:phrase
                                                                                         [:spacer 0]
                                                                                         [:phrase.chunk "HTML: "]
                                                                                         [:spacer 0]
                                                                                         (:content pb)])
                                                                                      (when (= "file" (:type pb))
                                                                                        [:paragraph
                                                                                         [:spacer 0]
                                                                                         [:phrase.chunk "Files: "]"\n"
                                                                                         (into [:paragraph]
                                                                                               (for [file (:files pb)]
                                                                                                 [:phrase
                                                                                                  [:chunk.bold "Filename: "] [:chunk (:name file)]"\n"
                                                                                                  [:chunk.bold "Url: "][:anchor {:target (str site-url "/"(:path file)) :style{:family :times-roman :color [66 100 162]}} (str site-url "/"(:path file))]]
                                                                                                 ))])
                                                                                      (when (= "tag" (:type pb))
                                                                                        [:paragraph
                                                                                         [:phrase.chunk "Badge-group"]
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
                                                 [:heading.heading-name "Social Connections: "]
                                                 [:spacer 0]
                                                 (when-not (empty? $user_followers)
                                                   (into [:paragraph
                                                          [:phrase.chunk "Followers: "] [:spacer 0]] (for [follower $user_followers
                                                                                                           :let [follower-id (:owner_id follower)
                                                                                                                 fname (:first_name follower)
                                                                                                                 lname (:last_name follower)
                                                                                                                 status (:status follower)]]
                                                                                                       [:paragraph
                                                                                                        [:anchor {:target (str site-url "/" "user/profile/" follower-id)} [:chunk.link (str fname " " lname ",  ")]]
                                                                                                        [:chunk.chunk "Status: "] [:chunk status]])))
                                                 (when-not (empty? $user_following)
                                                   (into [:paragraph
                                                          [:phrase.chunk "Following: "][:spacer 0]] (for [f $user_following
                                                                                                          :let [followee-id (:user_id f)
                                                                                                                fname (:first_name f)
                                                                                                                lname (:last_name f)
                                                                                                                status (:status f)]]
                                                                                                      [:paragraph
                                                                                                       [:anchor {:target (str site-url "/" "user/profile/" followee-id)} [:chunk.link (str fname " " lname ", ")]]
                                                                                                       [:chunk.chunk "Status: "] [:chunk status]])))
                                                 ])

                                              (when-not (empty? $connections)
                                                [:paragraph.generic
                                                 [:heading.heading-name "Badge Connections: "]
                                                 [:spacer 0]
                                                 (into [:paragraph]
                                                       (for [c $connections]
                                                         [:paragraph
                                                          [:chunk.chunk "ID: "] [:chunk (str (:id c))]"\n"
                                                          [:chunk.chunk "Name: "][:chunk (:name c)]"\n"
                                                          [:chunk.chunk "Description "][:chunk (:description c)]"\n"
                                                          [:chunk.chunk "Image: "] [:chunk.link (str site-url "/"(:image_file c))]"\n"
                                                          ]))])

                                              (when-not (empty? $events)
                                                [:paragraph.generic
                                                 [:heading.heading-name "Activity History! "]
                                                 ])

                                              (when-not (empty? $events)
                                                (into [:table.generic {:header ["Action" "Object" "Type" " Time"]}]
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


