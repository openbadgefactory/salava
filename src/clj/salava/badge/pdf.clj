(ns salava.badge.pdf
  (:require [yesql.core :refer [defqueries]]
            [salava.core.time :refer [unix-time date-from-unix-time]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump private?]]
            [salava.badge.main :refer [user-badges-to-export fetch-badge badge-url badge-evidences]]
            [salava.core.util :as u :refer [get-db plugin-fun get-plugins md->html str->qr-base64]]
            [clj-pdf.core :as pdf]
            [clj-pdf-markdown.core :refer [markdown->clj-pdf]]
            [clojure.string :refer [ends-with? blank?]]
            [salava.user.db :as ud]
            [ring.util.io :as io]
            [clojure.tools.logging :as log]
            [salava.badge.endorsement :refer [user-badge-endorsements]]
            [salava.badge.pdf-helper :as pdfh]))

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
                           :endorsements (->> [(vec (select-badge-endorsements {:id (:badge_id %1)} (u/get-db ctx)))
                                               (user-badge-endorsements ctx (:id %1))]
                                              flatten) #_(->> (vec (select-badge-endorsements {:id (:badge_id %1)} (u/get-db ctx))))
                           :evidences (remove (fn [e] (= true (get-in e [:properties :hidden]))) (badge-evidences ctx (:id %1) user-id))
                           :content %2)) badge-with-content temp)]
    (replace-nils badges)))


#_(defn process-pdf-page [stylesheet template badge ul]
    (let [file (java.io.File/createTempFile "temp" ".pdf")
          pdf (pdf/pdf (into [stylesheet] (template badge)) (.getAbsolutePath file))]
      (if (blank? (slurp file))
        (pdf/pdf [{} [:paragraph (t :core/Errorpage ul)]] (.getAbsolutePath file))
        file)))



(defn process-markdown-helper [markdown id context]
  (let [file (java.io.File/createTempFile "markdown" ".pdf")]
    (try
      (pdf/pdf [{} (pdfh/markdown->clj-pdf {:wrap {:global-wrapper :paragraph}} markdown)] (.getAbsolutePath file))

      true
      (catch Exception e
        (log/error (str "Markdown Error in Badge id:  "id " in " context))
        false)
      (finally (.delete file)))))

(defn process-markdown [markdown id context]
  (if (== 1 (count markdown))
    markdown
    (if-let [p (process-markdown-helper markdown id context)]
      (pdfh/markdown->clj-pdf {:image {:x 10 :y 10};} ;:spacer {:extra-starting-value 1 :allow-extra-line-breaks? true :single-value 2}
                               :wrap {:global-wrapper :paragraph}} markdown)
      "")))



(defn generatePDF [ctx user-id input lang]
  (let [data-dir (get-in ctx [:config :core :data-dir])
        site-url (get-in ctx [:config :core :site-url])
        badges (pdf-generator-helper ctx user-id input)
        user-data (ud/user-information ctx user-id)
        ul (if (blank? (:language user-data)) "en" (:language user-data))
        font-path  (first (mapcat #(get-in ctx [:config % :font] []) (get-plugins ctx)))
        font  {:ttf-name (str site-url font-path) :encoding :unicode}
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

        pdf-settings  (if (empty? font-path) {:stylesheet stylesheet  :bottom-margin 0 :footer {:page-numbers true :align :right}} {:font font :stylesheet stylesheet  :bottom-margin 0 :footer {:page-numbers false :align :right} :register-system-fonts? true})
        badge-template (pdf/template
                         (let [template #(cons [:paragraph]  [ (if (and (not (= "-" (:image_file %)))(ends-with? (:image_file %) "png"))
                                                                 [:image {:width 85 :height 85 :align :center} (str data-dir "/" (:image_file %))]
                                                                 [:image {:width 85 :height 85 :align :center :base64 true} $qr_code])

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
                                                                                             [:chunk.link (:url a)] [:spacer 0]]))]))]
                                                                [:paragraph
                                                                 [:chunk.chunk (str (t :badge/Criteria ul)": ")] [:anchor {:target (:criteria_url %)} [:chunk.link (t :badge/Opencriteriapage ul)]]"\n"

                                                                 [:spacer 0]
                                                                 (process-markdown (:criteria_content %) $id "Criteria")
                                                                 [:spacer 1] "\n"]]




                                                               (when-not (empty? $evidences)
                                                                 [:paragraph.generic
                                                                  [:spacer 0]
                                                                  [:phrase {:size 12 :style :bold} (if (= 1 (count $evidences)) (t :badge/Evidence ul) (t :badge/Evidences ul))]
                                                                  [:spacer]
                                                                  (reduce (fn [r evidence]
                                                                            (conj r [:phrase
                                                                                     (when (and (not (blank? (:name evidence))) (not= "-" (:name evidence))) [:phrase [:chunk (:name evidence)]"\n"])
                                                                                     (when (and (not (blank? (:narrative evidence))) (not= "-" (:narrative evidence)))  [:phrase [:chunk (:narrative evidence)] "\n"])
                                                                                     (when (and (not (blank? (:description evidence))) (not= "-" (:description evidence))) [:phrase [:chunk (:description evidence)]"\n"])
                                                                                     [:anchor {:target (:url evidence)} [:chunk.link (str (t :badge/Openevidencepage ul) "...")]]
                                                                                     [:spacer 2]]))
                                                                          [:list {:numbered true :indent 0}] $evidences)])

                                                               (when (seq $endorsements)
                                                                 [:paragraph.generic
                                                                  ;[:spacer 0]
                                                                  [:phrase {:size 12 :style :bold} (t :badge/BadgeEndorsedBy ul)] "\n"
                                                                  [:spacer 0]
                                                                  (into [:paragraph {:indent 0} ] (for [e $endorsements]
                                                                                                    [:paragraph
                                                                                                     (if (or (= "-" (:issuer_name e)) (blank? (:issuer_name e))) (str (:first_name e) " " (:last_name e))(:issuer_name e)) "\n"
                                                                                                     [:anchor {:target (:issuer_url e) :style{:family :times-roman :color [66 100 162]}} (or (:issuer_url e) "-")] "\n"
                                                                                                     [:chunk (if (number? (or (:issued_on e) (:mtime e))) (date-from-unix-time (long (* 1000 (or (:issued_on e) (:mtime e))))) (or (:issued_on e) (:mtime e)))] "\n"
                                                                                                     (process-markdown (:content e) $id "Endorsements")]))])

                                                               [:line {:dotted true}]
                                                               [:spacer 0]
                                                               [:heading.heading-name (t :badge/IssuerInfo ul)]
                                                               [:spacer 0]
                                                               [:paragraph.generic
                                                                [:chunk.chunk (str (t :badge/IssuerDescription ul)": ")] (:issuer_description %)]
                                                               [:spacer 0]
                                                               [:paragraph.generic
                                                                [:chunk.chunk (str (t :badge/IssuerWebsite ul)": ")]
                                                                [:anchor {:target (:issuer_content_url %) :style {:family :times-roman :color [66 100 162]}}  (:issuer_content_url %)]]
                                                               [:paragraph.generic
                                                                [:chunk.chunk (str (t :badge/IssuerContact ul)": ")] (:issuer_contact %)]

                                                               (when-not (= "-" (and (:creator_description %) (:creator_url %) (:creator_email %)))
                                                                 [:paragraph.generic
                                                                  [:spacer 0]
                                                                  [:paragraph
                                                                   [:chunk.chunk (str (t :badge/CreatorDescription ul)": ")] (:creator_description %) "\n"]
                                                                  [:paragraph
                                                                   [:chunk.chunk (str (t :badge/CreatorWebsite ul)": ")] [:anchor {:target (:creator_url %) :style{:family :times-roman :color [66 100 162]}} (:creator_url %)]]
                                                                  [:paragraph
                                                                   [:chunk.chunk (str (t :badge/CreatorContact ul)": ")] (:creator_email %)]])
                                                               (let [issuer-endorsement (replace-nils (select-issuer-endorsements {:id (:issuer_content_id %)} (u/get-db ctx)))]
                                                                 (when-not (empty? issuer-endorsement)
                                                                   [:paragraph.generic
                                                                    [:spacer]
                                                                    [:phrase {:size 12 :style :bold} (t :badge/IssuerEndorsedBy ul)]
                                                                    [:spacer 0]
                                                                    (into [:paragraph {:indent 0}]
                                                                          (for [e issuer-endorsement]
                                                                            [:paragraph {:indent 0}
                                                                             (:issuer_name e) "\n"
                                                                             [:anchor {:target (:issuer_url e) :style{:family :times-roman :color [66 100 162]}} (:issuer_url e)] "\n"
                                                                             [:chunk (if (number? (:issued_on e)) (date-from-unix-time (long (* 1000 (:issued_on e)))) (:issued_on e))] "\n"
                                                                             (process-markdown (:content e) $id "Issuer Endorsements")]))]))

                                                               [:pdf-table {:align :right :width-percent 100 :cell-border false}
                                                                nil
                                                                [[:pdf-cell [:paragraph {:align :right} [:chunk [:image  {:width 60 :height 60 :base64 true} $qr_code]]"\n"
                                                                             [:phrase [:chunk.link {:style :italic} (str site-url "/app/badge/info/" $id)]]]]]]
                                                               [:pagebreak]])

                               content (if (= lang "all") (map template $content) (map template (filter #(= (:default_language_code %) (:language_code %)) $content)))]

                           (reduce into [] content)))]
    (fn [out]
      (try
        (pdf/pdf (into [pdf-settings] (badge-template badges)) out)
        (catch Exception e
          (log/error "PDF not generated")
          (log/error (.getMessage e))
          (pdf/pdf [{} [:paragraph (t :core/Errorpage ul)]] out))))))
