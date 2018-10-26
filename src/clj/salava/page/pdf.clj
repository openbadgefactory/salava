(ns salava.page.pdf
  (:require [yesql.core :refer [defqueries]]
            [clojure.string :refer [upper-case ends-with? blank?]]
            [salava.core.time :refer [unix-time date-from-unix-time]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump private?]]
            [salava.core.util :as u :refer [get-db plugin-fun get-plugins md->html]]
            [salava.page.main :refer [page-with-blocks]]
            [clj-pdf.core :as pdf]
            [clj-pdf-markdown.core :refer [markdown->clj-pdf]]
            [clojure.zip :as zip]
            [salava.user.db :as ud]
            [net.cgrand.enlive-html :as enlive]
            [salava.badge.pdf :refer [process-markdown replace-nils pdf-generator-helper]]
            ))

(defqueries "sql/page/main.sql")

(def tag-map
  {
    :h1 [:paragraph {:leading 20 :style :bold :size 14}]
    :h2 [:paragraph {:leading 20 :style :bold :size 12}]
    :h3 [:paragraph {:leading 20 :style :bold :size 10}]
    :hr [:line]
    :br [:spacer]
    :img [:image]
    :pre [:paragraph {:size 10 :family :times-roman}]
    :p [:paragraph]
    :b [:chunk {:style :bold}]
    :em [:chunk {:style :italic}]
    :del [:chunk {:style :strikethru}]
    :ul [:list {:numbered false}]
    :ol [:list {:numbered true}]
    :li [:chunk]
    :a [:anchor]
    :sup [:chunk {:super true}]
    :strong [:chunk {:style :bold}]
    :blockquote [:paragraph {:style :italic :indent 5}]
    :table [:pdf-table {:border true} nil ]
    :tbody []
    :tr [:pdf-cell]
    :td [:chunk]

    })

(defn set-attrs [content {:keys [href src title]}]
  (cond
    href            (conj content {:target href})
    (and title src) [:paragraph {:align :center} (conj content src) title]
    src             (into content [{:align :center} src])
    :else           content))

(defn transform-node [{:keys [tag attrs content]}]
  (-> (or (tag tag-map) [:paragraph])
      (set-attrs attrs)
      (into content)))


(defn strip-html-tags [s]
  (->> s
       java.io.StringReader.
       enlive/html-resource
       first
       ;;     zip/xml-zip
       ;;     (iterate zip/next)
       ;;     (take-while (complement zip/end?))
       ;;     (filter (complement zip/branch?))
       ;;     (map zip/node)
       ;;     (apply str)
       ))

(defn generate-pdf [ctx page-id user-id header?]
  (let [page (replace-nils (conj () (page-with-blocks ctx page-id)))
        data-dir (get-in ctx [:config :core :data-dir])
        site-url (get-in ctx [:config :core :site-url])
        plugins (get-plugins ctx)
        user-data (ud/user-information ctx user-id)
        ul (if (blank? (:language user-data)) "en" (:language user-data))
        font-path  (first (mapcat #(get-in ctx [:config % :font] []) plugins))
        header-path (first (mapcat #(get-in ctx [:config % :logo] []) plugins))
        font  {:ttf-name (str site-url font-path)}
        stylesheet  {:generic {:family :times-roman
                               :color [127 113 121]}
                      :link {:family :times-roman
                             :color [66 100 162]}
                     :bold {:style :bold}}
        header      (if (= "true" header?)
                      {:table
                       [:pdf-table {:border false}
                        [100]
                        [[:pdf-cell {:padding 5}[:image {:width 200 :height 48} (str site-url (if-not (blank? header-path) header-path "/img/logo.png"))] [:line] [:spacer 2]]
                         ]]} nil)

        pdf-settings  (if (empty? font-path) {:header header :stylesheet stylesheet  :bottom-margin 0 :footer {:page-numbers false :align :right}} {:font font :header header :stylesheet stylesheet  :bottom-margin 0 :footer {:page-numbers false :align :right}})
        page-template (pdf/template
                        (let [template #(cons [:paragraph][
                                                            (when (= "heading"  (:type %))
                                                              (case (:size %)
                                                                "h1" [:paragraph.generic {:align :left }
                                                                       [:spacer 0]
                                                                       [:heading  (:content %)]
                                                                       [:line {:dotted true}]
                                                                       ]
                                                                 "h2" [:paragraph.generic {:align :left}
                                                                       [:spacer 1]
                                                                       [:heading {:style {:size 12 :align :center}}  (:content %)]
                                                                       [:line {:dotted true}]] ))

                                                             (when (= "badge" (:type %))
                                                               [:table {:widths [1 3] :border false :keep-together? false }

                                                                [[:cell {:align :center}
                                                                  (if (= "-" (:image_file %))
                                                                    ""
                                                                    [:paragraph {:align :center :keep-together true}
                                                                     (if (ends-with? (:image_file %) "png")
                                                                       [:paragraph [:chunk [:image {:align :center :width 80 :height 80} (str data-dir "/" (:image_file %))]] [:spacer 1]"\n" ]
                                                                       "")])
                                                                  [:paragraph
                                                                   (let [badge (pdf-generator-helper ctx user-id (list (:badge_id %)))]
                                                                    (when-not (empty? badge)
                                                                      [:phrase {:align :center}
                                                                       [:chunk [:image {:align :center :width 75 :height 75  :base64 true} (:qr_code (first badge))]]"\n"
;;                                                                        [:spacer 0]
                                                                       [:chunk.generic (t :badge/Scantobadge ul)]]))]]

                                                                 [:cell
                                                                  [:heading.generic  (:name %)]
                                                                  [:spacer 0]
                                                                  [:paragraph.generic
                                                                   [:chunk.bold (str (t :badge/Issuedby ul) ": ")] [:chunk (:issuer_content_name %)] "\n"
                                                                   [:chunk.bold (str (t :badge/Issuedon ul)": ")] [:chunk (if (number? (:issued_on %)) (date-from-unix-time (long (* 1000 (:issued_on %))) "date") (:issued_on %))]
                                                                   [:spacer 0]
                                                                   (:description %)"\n"
                                                                   [:spacer 0]
                                                                   [:paragraph {:keep-together true}
                                                                    [:phrase.bold (str (t :badge/Criteria ul)": ")] [:spacer 0]
                                                                    [:anchor {:target (:criteria_url %) :style{:family :times-roman :color [66 100 162]}} (:criteria_url %)]
                                                                    [:spacer 0]
                                                                    (let [content (some->> (pdf-generator-helper ctx user-id (list (:badge_id %))) first :content first) ]
                                                                      (when-not (empty? content)
                                                                        (process-markdown (:criteria_content content) (:badge_id %) "Criteria")))]]]]

                                                                [[:cell {:colspan 2} [:line {:dotted true}]]]])

                                                             (when (and (= "html" (:type %)) (not (= "-" (:content %))))
                                                               [:paragraph.generic {:keep-together false :align :left}
                                                                [:chunk.bold {:size 11 :style :bold} (str (t :page/html ul) ": ")]"\n"
                                                                [:spacer 1]
                                                                (clojure.walk/postwalk
                                                                  (fn [n] (if (:tag n) (transform-node n) n))(strip-html-tags (:content %)))"\n"
                                                                [:spacer 4]
                                                                [:line {:dotted true}]
                                                                [:spacer 2]])

                                                             (when (= "file" (:type %))
                                                               [:paragraph.generic
                                                                [:chunk.bold {:size 11} (upper-case (t :file/Files ul))]"\n"
                                                                [:spacer 0]
                                                                (into [:paragraph] (for [file (:files %)]
                                                                                     [:paragraph
                                                                                      [:chunk.bold (str (t :badge/Name ul) ": ")] [:chunk (:name file)]"\n"
                                                                                      [:chunk.bold (str (t :badge/URL ul) ": ")][:anchor {:target (str site-url "/"(:path file)) :style{:family :times-roman :color [66 100 162]}} (str site-url "/"(:path file))]
                                                                                      ]))
                                                                [:line {:dotted true}]])

                                                             (when (= "tag" (:type %))
                                                               (if (= "long" (:format %))
                                                                 (into [:table {:border false :widths [1 3] :keep-together? false}]
                                                                       (conj  (into [[[:cell {:colspan 2}]]] (for [badge (:badges %)
                                                                                                                   :let [b (pdf-generator-helper ctx user-id (list (:id badge)))
                                                                                                                         content (-> b first :content first)]]
                                                                                                               (when-not (empty? b)
                                                                                                                 (conj [[:cell {:align :left} (if (and (not (= "-" (:image_file badge))) (ends-with? (:image_file badge) "png")) [:image {:align :center :width 75 :height 75}(str data-dir "/" (:image_file badge))] [:image {:align :center :width 75 :height 75 :base64 true} (:qr_code (first b))])]]
                                                                                                                       [:cell
                                                                                                                        [:paragraph.generic
                                                                                                                         [:heading (:name badge)]
                                                                                                                         [:chunk.bold (str (t :badge/Issuedby ul) ": ")] [:chunk (:issuer_content_name content)] "\n"
                                                                                                                           [:phrase
                                                                                                                            [:chunk.bold (str (t :badge/Issuedon ul)": ")] [:chunk (if (number? (:issued_on badge)) (date-from-unix-time (long (* 1000 (:issued_on badge))) "date") (:issued_on badge))] "\n"]
                                                                                                                         (:description badge)"\n"
                                                                                                                         [:spacer 0]
                                                                                                                         [:paragraph {:keep-together true}
                                                                                                                          [:phrase.bold (str (t :badge/Criteria ul)": ")] "\n"
                                                                                                                          [:anchor {:target (:criteria_url badge) :style{:family :times-roman :color [66 100 162]}} (:criteria_url badge)] "\n"
                                                                                                                          (process-markdown (:criteria_content content) (:id badge) "Criteria")
                                                                                                                          ]
                                                                                                                         ][:spacer 0]]))))
                                                                              [[:cell {:colspan 2}[:line {:dotted true}]]]))

                                                                 [:table {:no-split-cells? true :width 100 :border false}
                                                                  [[:cell
                                                                    [:table {:align :center :width-percent 100 :border false :num-cols 5}
                                                                     (into [[:cell {:colspan 5}]] (for [badge (:badges %)
                                                                                                        :let [b (pdf-generator-helper ctx user-id (list (:id badge)))
                                                                                                              content (-> b first :content first)]]
                                                                                                    (when-not (empty? b)
                                                                                                      [:cell {:padding-right 10}[:anchor {:target (str site-url "/app/badge/info/" (:id badge))}
                                                                                                                                 (if (ends-with? (:image_file badge) "png") [:chunk [:image {:align :center :width 60 :height 60}(str data-dir "/" (:image_file badge))]] [:chunk [:image {:align :center :width 60 :height 60 :base64 true} (:qr_code (first b))]])
                                                                                                                                 ]])))]]]
                                                                  [[:cell [:line {:dotted true}]]]]))
                                                             [:spacer 3]])

                               content (-> (mapv template $blocks)
                                           (conj   [[:pdf-table {:align :right :width-percent 100 :cell-border false}
                                                     nil
                                                     [[:pdf-cell [:paragraph [:chunk [:image {:width 85 :height 85 :base64 true} $qr_code]]"\n"
                                                                  [:phrase [:chunk.link {:style :italic} (str site-url "/app/page/view/" $id)]]]
                                                       ]]]]))
                               ]
                           (reduce into [[:paragraph.generic {:align :center}
                                          [:phrase {:size 30 :style :bold :align :center} $name]
                                          [:spacer 1]
                                          [:chunk {:style :italic :size 12} (str $first_name " " $last_name)]
                                          [:spacer 1]]
                                         [:paragraph.generic
                                          [:chunk {:style :italic} $description]
                                          [:spacer 1]]] content)
                           ))
         ]
    ;; TODO FIX HTML
    (fn [out]
      (pdf/pdf (into [pdf-settings [:spacer 4]]
                     (page-template page)) out))))
