(ns salava.badge.pdf
  (:require [yesql.core :refer [defqueries]]
            [salava.core.time :refer [unix-time date-from-unix-time]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump private?]]
            [salava.badge.main :refer [user-badges-to-export fetch-badge badge-url]]
            [salava.core.util :as u :refer [get-db plugin-fun get-plugins md->html str->qr-base64]]
            [clj-pdf.core :as pdf]
            [clj-pdf-markdown.core :refer [markdown->clj-pdf]]
            [clojure.string :refer [ends-with? blank?]]
            [salava.user.db :as ud]
            [clojure.java.io :as io]))

(defqueries "sql/badge/main.sql")

(defn replace-nils [data]
  (clojure.walk/postwalk
    (fn [d]
      (if (map? d)
        (let [m (into {} (map (fn [k v] (if (blank? (str v)) {k "-"}{k v})) (keys d) (vals d)))]
          (when (seq m) m))
        d))
    data))

(defn pdf-generator-helper [ctx user-id input]
  (let [badges-for-export (user-badges-to-export ctx user-id)
        filtered-badges (filter (fn [b] (some #(= % (:id b)) input)) badges-for-export)
        badge-with-content (map #(-> (fetch-badge ctx (:id %))
                                     (assoc :tags (:tags %))) filtered-badges)
        badge-ids (map #(:badge_id (first (get-in % [:content]))) badge-with-content)
        temp (map #(select-multi-language-badge-content {:id %} (u/get-db ctx)) badge-ids)
        badges (map #(-> %1
                         (dissoc :content)
                         (assoc :qr_code (str->qr-base64 (badge-url ctx (:id %1)))
                           :endorsements (vec (select-badge-endorsements {:id (:badge_id %1)} (u/get-db ctx)))
                           :content %2)) badge-with-content temp)]
    (replace-nils badges)))

(defn generatePDF [ctx user-id input lang]
  (let [data-dir (get-in ctx [:config :core :data-dir])
        site-url (get-in ctx [:config :core :site-url])
        badges (pdf-generator-helper ctx user-id input)
        user-data (ud/user-information ctx user-id)
        ul (if (blank? (:language user-data)) "en" (:language user-data))
        font-path  (first (mapcat #(get-in ctx [:config % :font] []) (get-plugins ctx)))
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
        badge-template (pdf/template
                         (let [template #(cons [:paragraph]  [ (if (and (not (= "-" (:image_file %)))(ends-with? (:image_file %) "png"))
                                                                 [:image {:width 100 :height 100 :align :center} (str data-dir "/" (:image_file %))]
                                                                 [:image {:width 100 :height 100 :align :center :base64 true} $qr_code ])

                                                               [:heading.heading-name (:name %)]
                                                               [:paragraph {:indent 20 :align :center} [:spacer]]
                                                               [:paragraph.generic {:align :left :style :italic} (:description %)][:spacer]
                                                               [:paragraph.generic
                                                                [:chunk.chunk (str (t :badge/Recipient ul)": ")] (str $first_name  " " $last_name ) "\n"
                                                                [:chunk.chunk (str (t :badge/Issuedby ul)": ")] (:issuer_content_name %)"\n"
                                                                (when-not (= "-" (:creator_name %))
                                                                  [:paragraph.generic
                                                                   [:chunk.chunk (str (t :badge/Createdby ul)": ")] (:creator_name %)])

                                                                [:chunk.chunk (str (t :badge/Issuedon ul)": ")] (if (number? $issued_on) (date-from-unix-time (long (* 1000 $issued_on)) "date") $issued_on) "\n"

                                                                [:paragraph
                                                                 [:chunk.chunk (str (t :badge/Expireson ul)": ")] (if (number? $expires_on) (date-from-unix-time (long (* 1000 $expires_on)) "date") $expires_on)]

                                                                (when-not (empty? $tags)
                                                                  [:paragraph.generic
                                                                   [:chunk.chunk (str (t :badge/Tags ul)": ")] (into [:phrase ] (for [t $tags] (str t " ")))])
                                                                [:paragraph
                                                                 (let [alignments (replace-nils (select-alignment-content {:badge_content_id (:badge_content_id %)} (u/get-db ctx)))]
                                                                   (when-not (empty? alignments)
                                                                     [:paragraph.generic
                                                                      [:spacer 0]
                                                                      [:phrase {:size 12 :style :bold} (str (t :badge/Alignments ul)": " (count alignments))]"\n"
                                                                      (into [:paragraph ] (for [a alignments]
                                                                                            [:paragraph
                                                                                             (:name a)"\n"
                                                                                             [:chunk {:style :italic} (:description a)] "\n"
                                                                                             [:chunk.link (:url a)] [:spacer 0]]) )]))]
                                                                [:paragraph
                                                                 [:chunk.chunk (str (t :badge/Criteria ul)": ")] [:anchor {:target (:criteria_url %) :style{:family :times-roman :color [66 100 162]}} (:criteria_url %)]]
                                                                [:spacer 0]
                                                                (process-markdown "Criteria" (:criteria_content %))]

                                                               (when-not (empty? $endorsements)
                                                                 [:paragraph.generic
                                                                  [:spacer 0]
                                                                  [:phrase {:size 12 :style :bold} (t :badge/BadgeEndorsedBy ul)]
                                                                  [:spacer 0]
                                                                  (into [:paragraph {:indent 0} ] (for [e $endorsements]
                                                                                                    [:paragraph
                                                                                                     (:issuer_name e) "\n"
                                                                                                     [:anchor {:target (:issuer_url e) :style{:family :times-roman :color [66 100 162]}} (or (:issuer_url e) "-")] "\n"
                                                                                                     [:chunk (if (number? (:issued_on e)) (date-from-unix-time (long (* 1000 (:issued_on e)))) (:issued_on e))] "\n"
                                                                                                     (process-markdown "Endorsements" (:content e))]))])

                                                               [:line {:dotted true}]
                                                               [:spacer 0]
                                                               [:heading.heading-name (t :badge/IssuerInfo ul)]
                                                               [:spacer 0]
                                                               [:paragraph.generic
                                                                [:chunk.chunk (str (t :badge/IssuerDescription ul)": ")] (:issuer_description %) ]
                                                               [:spacer 1]
                                                               [:paragraph.generic
                                                                [:chunk.chunk (str (t :badge/IssuerWebsite ul)": ")]
                                                                [:anchor {:target (:issuer_content_url %) :style{:family :times-roman :color [66 100 162]}}  (:issuer_content_url %)]]
                                                               [:paragraph.generic
                                                                [:chunk.chunk (str (t :badge/IssuerContact ul)": ")] (:issuer_contact %)]
                                                               (when-not (= "-" (:creator_description %))
                                                                 [:paragraph.generic
                                                                  [:chunk.chunk (str (t :badge/CreatorDescription ul)": ")] (:creator_description %)])
                                                               (when-not (= "-" (:creator_url %))
                                                                 [:paragraph.generic
                                                                  [:chunk.chunk (str (t :badge/CreatorWebsite ul)": ")] [:anchor {:target (:creator_url %) :style{:family :times-roman :color [66 100 162]}} (:creator_url %)] "\n"])
                                                               (when-not (= "-" (:creator_email %))
                                                                 [:paragraph.generic
                                                                  [:chunk.chunk (str (t :badge/CreatorContact ul)": ")] (:creator_email %)])
                                                               (let [issuer-endorsement (replace-nils (select-issuer-endorsements {:id (:issuer_content_id %)} (u/get-db ctx)))]
                                                                 (when-not (empty? issuer-endorsement)
                                                                   [:paragraph.generic
                                                                    [:spacer]
                                                                    [:phrase {:size 12 :style :bold} (t :badge/IssuerEndorsedBy ul)]
                                                                    [:spacer 0]
                                                                    (into [:paragraph {:indent 0} ]
                                                                          (for [e issuer-endorsement]
                                                                            [:paragraph {:indent 0}
                                                                             (:issuer_name e) "\n"
                                                                             [:anchor {:target (:issuer_url e) :style{:family :times-roman :color [66 100 162]}} (:issuer_url e)] "\n"
                                                                             [:chunk.link (if (number? (:issued_on e)) (date-from-unix-time (long (* 1000 (:issued_on e)))) (:issued_on e))] "\n"
                                                                             (process-markdown "Issuer endorsements" (:content e))]))]))

                                                               [:pdf-table {:align :right :width-percent 100 :cell-border false}
                                                                nil
                                                                [[:pdf-cell [:paragraph {:align :right} [:chunk [:image  {:width 85 :height 85 :base64 true} $qr_code]]"\n"
                                                                             [:phrase [:chunk.link {:style :italic} (str site-url "/badge/info/" $id)]]]]]]
                                                               [:pagebreak]])

                               content (if (= lang "all") (map template $content) (map template (filter #(= (:default_language_code %) (:language_code %)) $content)))]

                           (reduce into [] content)))]
    (fn [output-stream]
      (pdf/pdf (into [pdf-settings] (badge-template badges)) output-stream)))
  )
