(ns salava.page.ui.helper
  (:require [reagent.core :refer [create-class atom cursor]]
            [reagent-modals.modals :as m]
            [markdown.core :refer [md->html]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.helper :refer [navigate-to path-for]]
            [salava.badge.ui.helper :as bh]
            [salava.badge.ui.modal :as bm]
            [salava.core.time :refer [date-from-unix-time]]
            [salava.file.icons :refer [file-icon]]
            [salava.core.helper :refer [dump]]
            [salava.admin.ui.reporttool :refer [reporttool1]]
            ))

(defn delete-page [id]
  (ajax/DELETE
    (path-for (str "/obpv1/page/" id))
    {:handler (fn [] (do
                       (m/close-modal!)
                       (navigate-to "/profile/page")))}))

(defn delete-page-modal [page-id]
  [:div
   [:div.modal-header
    [:button {:type "button"
              :class "close"
              :data-dismiss "modal"
              :aria-label "OK"}
     [:span {:aria-hidden "true"
             :dangerouslySetInnerHTML {:__html "&times;"}}]]]
   [:div.modal-body
    [:div {:class (str "alert alert-warning")}
     (t :page/Deleteconfirm)]]
   [:div.modal-footer
    [:button {:type "button"
              :class "btn btn-primary"
              :data-dismiss "modal"}
     (t :page/Cancel)]
    [:button {:type "button"
              :class "btn btn-warning"
              :on-click #(delete-page page-id)}
     (t :page/Delete)]]])




(defn badge-block [{:keys [format image_file name description issuer_image issued_on issuer_contact criteria_url criteria_markdown issuer_content_id issuer_content_name issuer_content_url issuer_email issuer_description criteria_content creator_content_id creator_name creator_url creator_email creator_image creator_description show_evidence evidence_url]}]
  [:div {:class "row badge-block badge-info flip"}
   [:div {:class "col-md-4 badge-image"}
    [:img {:src (str "/" image_file)}]]
   [:div {:class "col-md-8"}
    [:div.row
     [:div.col-md-12
      [:h3.badge-name name]]]
    #_[:div.row
       [:div
        (bh/issuer-image issuer_image)]]
    [:div.row
     [:div.col-md-12
      (bh/issued-on issued_on)]]
    [:div.row
     [:div.col-md-12
      #_(bh/issuer-label-image-link issuer_content_name issuer_content_url issuer_description issuer_contact issuer_image)

      #_(bh/creator-label-image-link creator_name creator_url creator_description creator_email creator_image)
      (bm/issuer-modal-link issuer_content_id issuer_content_name)
      (bm/creator-modal-link creator_content_id creator_name)
      ]]

    [:div.row
     [:div {:class "col-md-12 description"} description]]
    [:div.row
     [:div.col-md-12
      [:h3.criteria (t :badge/Criteria)]]]
    [:div.row
     [:div.col-md-12
      [:a {:href criteria_url :target "_blank"} (t :badge/Opencriteriapage)]]]
    (if (= format "long")
      [:div
       [:div {:class "row criteria-html"}
        [:div.col-md-12
         {:dangerouslySetInnerHTML {:__html criteria_content}}]]
       [:div.row
        [:div {:class                   "col-md-12"
               :dangerouslySetInnerHTML {:__html (md->html criteria_markdown)}}]]])
    (if (and (pos? show_evidence) evidence_url)
      [:div.row
       [:div.col-md-12
        [:h2.uppercase-header (t :badge/Evidence)]
        [:div [:a {:target "_blank" :href evidence_url} (t :badge/Openevidencepage) "..."]]]])]])

(defn html-block [{:keys [content]}]
  [:div.html-block
   (if (re-find #"iframe" (str content))
     [:div.embed-responsive.embed-responsive-16by9
      {:dangerouslySetInnerHTML {:__html (md->html content)}}]
     [:div
      {:dangerouslySetInnerHTML {:__html (md->html content)}}]
     )])


(defn file-block [{:keys [files]}]
  [:div.file-block
   [:div.row
    [:div.col-md-12
     (if (every? #(re-find #"image/" (str (:mime_type %))) files)
       (into [:div.file-block-images]
             (for [file files]
               [:div.file-block-image
                [:img {:src (str "/" (:path file))}]]))
       [:div.file-block-attachments
        [:label.files-label
         (t :page/Attachments) ": "]
        (into [:div]
              (for [file files]
                [:span.attachment
                 [:i {:class (str "page-file-icon fa " (file-icon (:mime_type file)))}]
                 [:a.file-link {:href (str "/" (:path file))
                                :target "_blank"}
                  (:name file)]]))])]]])

(defn heading-block [{:keys [size content]}]
  [:div.heading-block
   (case size
     "h1" [:h1 content]
     "h2" [:h2 content]
     nil)])

(defn tag-block [{:keys [tag badges format sort]}]
  [:div.tag-block
   [:div
    [:label (t :page/Tag ":")] (str " " tag)]
   (let [sorted-badges (case sort
                         "name" (sort-by :name < badges)
                         "modified" (sort-by :mtime > badges)
                         badges)]
     (for [badge sorted-badges]
       (if (= format "short")
         [:a.small-badge-image {:href (path-for (str "/badge/info/" (:id badge)))
                                :key  (:id badge)}
          [:img {:src (str "/" (:image_file badge))
                 :title (:name badge)}]]
         (badge-block (assoc badge :format "long")))))])

(defn view-page [page]
  (let [{:keys [id name description mtime user_id first_name last_name blocks theme border padding visibility qr_code]} page]
    [:div {:id    (str "theme-" (or theme 0))
           :class "page-content"}
     (if id
       [:div.panel
        [:div.panel-left
         [:div.panel-right
          [:div.panel-content
           (if (and qr_code (= visibility "public"))
             [:div.row
              [:div {:class "col-xs-12 text-center"}
               [:img#print-qr-code {:src (str "data:image/png;base64," qr_code)}]]])
           (if mtime
             [:div.row
              [:div {:class "col-md-12 page-mtime"}
               (date-from-unix-time (* 1000 mtime))]])
           [:div.row
            [:div {:class "col-md-12 page-title"}
             [:h1 name]]]
           [:div.row
            [:div {:class "col-md-12 page-author"}
             [:a {:href (path-for (str "/user/profile/" user_id))} (str first_name " " last_name)]]]
           [:div.row
            [:div {:class "col-md-12 page-summary"}
             description]]
           (into [:div.page-blocks]
                 (for [block blocks]
                   [:div {:class "block-wrapper"
                          :style {:border-top-width (:width border)
                                  :border-top-style (:style border)
                                  :border-top-color (:color border)
                                  :padding-top (str padding "px")
                                  :margin-top (str padding "px")}}
                    (case (:type block)
                      "badge" (badge-block block)
                      "html" (html-block block)
                      "file" (file-block block)
                      "heading" (heading-block block)
                      "tag" (tag-block block)
                      nil)]))]]]])]))

(defn render-page-modal [page]
  [:div {:id "badge-content"}
   [:div.modal-body
    [:div.row
     [:div.col-md-12
      [:button {:type         "button"
                :class        "close"
                :data-dismiss "modal"
                :aria-label   "OK"}
       [:span {:aria-hidden             "true"
               :dangerouslySetInnerHTML {:__html "&times;"}}]]]]]
   [view-page page]
   [:div {:class "modal-footer page-content"}
    (reporttool1 (:id page) (:name page) "page")
    ]
   ])

(defn view-page-modal [page]
  (create-class {:reagent-render (fn [] (render-page-modal page))
                 :component-will-unmount (fn [] (m/close-modal!))}))

(defn edit-page-header [header]
  [:div.row
   [:div.col-sm-12
    [:h1 header]]])

(defn block-specific-values [{:keys [type content badge tag format sort files title badges]}]
  (case type
    "heading" {:type "heading" :size "h1" :content content}
    "sub-heading" {:type "heading" :size "h2":content content}
    "badge" {:format (or format "short") :badge_id (:id badge 0)}
    "html" {:content content}
    "file" {:files (map :id files)}
    "tag" {:tag tag :format (or format "short") :sort (or sort "name")}
    "showcase" {:format (or format "short") :title title :badges (map :id badges)}

    nil))

(defn prepare-blocks-to-save [blocks]
  (for [block blocks]
    (-> block
        (select-keys [:id :type])
        (merge (block-specific-values block)))))

(defn save-page [{:keys [id name description blocks]} next-url]
  (ajax/POST
    (path-for (str "/obpv1/page/save_content/" id))
    {:params {:name name
              :description description
              :blocks (prepare-blocks-to-save blocks)}
     :handler (fn [] (when next-url (navigate-to next-url)))}))

(defn save-theme [state next-url]
  (let [{:keys [id theme border padding]} (:page @state)]
    (ajax/POST
      (path-for (str "/obpv1/page/save_theme/" id))
      {:params {:theme theme
                :border (:id border)
                :padding padding}
       :handler (fn [data]
                  (swap! state assoc :alert {:message (t (keyword (:message data))) :status (:status data)})
                  (js/setTimeout (fn [] (swap! state assoc :alert nil)) 3000))
       :finally (fn [] (when next-url (navigate-to next-url)))})))

(defn save-settings [state next-url]
  (let [{:keys [id tags visibility password]} (:page @state)]
    (reset! (cursor state [:message]) nil)
    (ajax/POST
      (path-for (str "/obpv1/page/save_settings/" id))
      {:params {:tags tags
                :visibility visibility
                :password password}
       :handler (fn [data]
                  (swap! state assoc :alert {:message (t (keyword (:message data))) :status (:status data)})

                  (js/setTimeout (fn [] (swap! state assoc :alert nil)) 5000)
                  (if (and (= "error" (:status data)) (= (:message data) "page/Evidenceerror"))
                    (swap! state assoc ;:message (keyword (:message data))
                           :page {:id id
                                  :tags tags
                                  :password password
                                  :visibility "public"})))
       :finally (when next-url (navigate-to next-url))})))


(defn button-logic
  "Use map lookup to manage button actions. editing what a button does happens here"
  [page-id state]
  (let [urls {:content {:previous nil
                        :current (str "/profile/page/edit/" page-id)
                        :next (str "/profile/page/edit_theme/" page-id)
                        :save-function (fn [next-url] (save-page (:page @state) next-url))}
              :theme {:previous (str "/profile/page/edit/" page-id)
                      :current (str "/profile/page/edit_theme/" page-id)
                      :next (str "/profile/page/settings/" page-id)
                      :save-function (fn [next-url] (save-theme state next-url))}
              :settings {:previous (str "/profile/page/edit_theme/" page-id)
                         :current (str "/profile/page/settings/" page-id)
                         :next (str "/profile/page/preview/" page-id)
                         :save-function (fn [next-url] (save-settings state next-url))
                         }
              :preview {:previous (str "/profile/page/settings/" page-id)
                        :current (str "/profile/page/preview/" page-id)
                        :next nil}}]

    {:content {:save! (get-in urls [:content :save-function])
               :save-and-next! (fn [] (save-page (:page @state) (get-in urls [:content :next])))
               :url (get-in urls [:content :current])
               :go! (fn [] (navigate-to (get-in urls [:content :current])))
               :editable? true
               :previous false
               :next true}
     :theme {:save! (get-in urls [:theme :save-function])
             :save-and-next! (fn [] (save-theme state (get-in urls [:theme :next])))
             :save-and-previous! (fn [] (save-theme state (get-in urls [:theme :previous])))
             :go! (fn [] (navigate-to (get-in urls [:theme :current])))
             :editable? true
             :url (get-in urls [:theme :current])
             :previous true
             :next true
             }
     :settings {:save! (get-in urls [:settings :save-function])
                :save-and-next! (fn [] (save-settings state (get-in urls [:settings :next])))
                :save-and-previous! (fn [] (save-settings state (get-in urls [:settings :previous])))
                :go! (fn [] (navigate-to (get-in urls [:settings :current])))
                :editable? true
                :url (get-in urls [:settings :current])
                :previous true
                :next true
                }
     :preview {:save! #()
               :go! (fn [] (navigate-to (get-in urls [:preview :current])))
               :editable? false
               :url (get-in urls [:preview :current])
               :previous true
               :next false}}))

(defn edit-page-buttons [id target state]
  (let [logic (button-logic id state)
        editable? (get-in logic [target :editable?])]
    [:div {:class "row flip"
           :id "buttons"}
     [:div.col-xs-8.wizard
      [:a {:class (if (= target :content) "current")
           :href "#"
           :on-click #(do
                        (.preventDefault %)
                        (if editable?
                          (as-> (get-in logic [target :save!]) f (f (get-in logic [:content :url])))
                          (as-> (get-in logic [:content :go!])  f (f))))}
       [:span {:class (str "badge" (if (= target :content) " badge-inverse" ))} "1."]
       (t :page/Content)]
      [:a {:class (if (= target :theme) "current")
           :href "#"
           :on-click #(do (.preventDefault %)
                        (if editable?
                          (as-> (get-in logic [target :save!]) f (f (get-in logic [:theme :url])))
                          (as-> (get-in logic [:theme :go!]) f (f))))}
       [:span {:class (str "badge" (if (= target :theme) " badge-inverse" ))} "2."]
       (t :page/Theme)]
      [:a {:class (if (= target :settings) "current")
           :href "#"
           :on-click #(do (.preventDefault %)
                        (if editable?
                          (as-> (get-in logic [target :save!]) f (f (get-in logic [:settings :url])))
                          (as-> (get-in logic [:settings :go!]) f (f))))}
       [:span {:class (str "badge" (if (= target :settings) " badge-inverse" ))} "3."]
       (t :page/Settings)]
      [:a {:class (if (= target :preview) "current")
           :href "#"
           :on-click #(do (.preventDefault %)
                        (if editable?
                          (as-> (get-in logic [target :save!]) f (f (get-in logic [:preview :url])))
                          (as-> (get-in logic [:preview :go!]) f (f)))
                        )}
       [:span {:class (str "badge" (if (= target :preview) " badge-inverse" ))} "4."]
       (t  :page/Preview)]]
     [:div {:class "col-xs-4"
            :id "buttons-right"}
      [:a {:class "btn btn-primary view-btn"
           :on-click #(do (.preventDefault %) (navigate-to (str "/profile/page/view/" id)))
           :href "#"}
       (t :page/View)]]
     [m/modal-window]]))

(defn manage-page-buttons [current id state]
  (let [id @id
        logic (button-logic id state)
        previous? (get-in logic [current :previous])
        next?  (get-in logic [current :next])]
    (create-class {:reagent-render   (fn []

                                       [:div
                                        [:div.row {:id "page-edit"
                                                   :style {:margin-top "10px" :margin-bottom "10px"}}
                                         [:div.col-md-12
                                          (when previous? [:div {:id "step-button-previous"}
                                                           [:a {:href "#" :on-click #(do
                                                                                       (.preventDefault %)
                                                                                       (as-> (get-in logic [current :save-and-previous!]) f (f))
                                                                                       )}  (t :core/Previous)]])
                                          [:button {:class    "btn btn-primary"
                                                    :on-click #(do
                                                                 (.preventDefault %)
                                                                 (dump (:page @state))
                                                                 (as-> (get-in logic [current :save!]) f (f)))}
                                           (t :page/Save)]
                                          [:button.btn.btn-warning {:on-click #(do
                                                                                 (.preventDefault %)
                                                                                 (navigate-to  "/profile/page"))}
                                           (t :core/Cancel)]

                                          [:button.btn.btn-danger {:on-click #(do
                                                                                (.preventDefault %)
                                                                                (delete-page (get-in @state [:page :id])))}
                                           (t :core/Delete)]
                                          (when next?  [:div.pull-right {:id "step-button"}
                                                        [:a {:href "#" :on-click #(do
                                                                                    (.preventDefault %)
                                                                                    (as-> (get-in logic [current :save-and-next!]) f (f)))}
                                                         (t :core/Next)]])]]

                                        (when (:alert @state)
                                          [:div.row
                                           [:div.col-md-12
                                            [:div {:class (str "alert " (case (get-in @state [:alert :status])
                                                                          "success" "alert-success"
                                                                          "error" "alert-warning"))
                                                   :style {:display "block" :margin-bottom "20px"}}
                                             (get-in @state [:alert :message] nil)]
                                            ]])
                                        ])})))
