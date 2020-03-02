(ns salava.badgeIssuer.ui.helper
  (:require
   [cljs-uuid-utils.core :refer [make-random-uuid uuid-string]]
   [clojure.string :refer [blank? starts-with?]]
   [reagent.core :refer [atom cursor create-class]]
   [reagent-modals.modals :as m]
   [reagent.session :as session]
   [salava.badge.ui.evidence :refer [evidence-icon toggle-input-mode input resource-input init-resources init-evidence-form]]
   [salava.badgeIssuer.schemas :as schemas]
   [salava.badgeIssuer.ui.util :refer [save-selfie-badge validate-inputs]]
   [salava.core.ui.ajax-utils :as ajax]
   [salava.core.ui.helper :refer [navigate-to path-for hyperlink url?]]
   [salava.core.ui.input :refer [editor]]
   [salava.core.i18n :refer [t]]
   [salava.core.ui.modal :as mo]
   [salava.user.ui.helper :refer [profile-picture]]))


(defn progress-wizard [state]
  (let [step (cursor state [:step])]
    [:div.container
     [:div.stepwizard
      [:div.stepwizard-row.setup-panel
       [:div.stepwizard-step.col-xs-4
        [:a#step1.btn.btn-success.btn-circle
         {:type "button"
          :class (if (= 0 @step) "active" "")
          :on-click #(reset! step 0)}
         1]
        [:p {:class (if (= 0 @step) "active" "")} [:small (t :badgeIssuer/Image)]]]

       [:div.stepwizard-step.col-xs-4
        [:a#step2.btn.btn-success.btn-circle
         {:type "button"
          :class (if (= 1 @step) "active" "")
          :on-click #(reset! step 1)}
         2]

        [:p {:class (if (= 1 @step) "active" "")} [:small (t :badgeIssuer/Content)]]]

       [:div.stepwizard-step.col-xs-4
        [:a#step3.btn.btn-success.btn-circle
         {:type "button"
          :class (if (= 2 @step) "active" "")
          :on-click #(reset! step 2)}
         3]

        [:p {:class (if (= 2 @step) "active" "") } [:small (t :badgeIssuer/Settings)]]]]]]))

(defn init-selfie-badge [id state]
  (ajax/GET
   (path-for (str "/obpv1/selfie/create/" id))
   {;:param {:id (:id @state)}
    :handler (fn [data]
               (swap! state assoc :badge data
                      :generating-image false))}))

(defn md->html [md]
  (if @editor (.previewRender (.-options @editor) md) md))


(defn bottom-buttons [state]
  (create-class
   {:reagent-render
    (fn []
      [:div.bottom-navigation.panel-footer
       ;[:hr.border]
       [:div;.row
        [:div
         (when (and @(cursor state [:in-modal])(:error-message @state) (not (blank? (:error-message @state))))
            [:div
              {:class "alert alert-danger" :role "alert"}  (:error-message @state)])
         [:div.btn-toolbar
           [:div.btn-group
           ;;preview
            (when (not @(cursor state [:in-modal]))
              [:a.btn.btn-success.btn-bulky
               {:href "#"
                :on-click #(do
                             (.preventDefault %)
                             (mo/open-modal [:selfie :preview] {:badge (assoc (:badge @state) :criteria (md->html (get-in @state [:badge :criteria] "")) #_(if @editor
                                                                                                                                                                                                                                                                    (.previewRender (.-options @editor) (get-in @state [:badge :criteria] ""))
                                                                                                                                                                                                                                                                    (get-in @state [:badge :criteria] "")))}))}
               [:span  (t :badgeIssuer/Preview)]])
            ;;Saveandexit
            [:a.btn.btn-primary.btn-bulky
             {:href "#"
              :on-click #(save-selfie-badge state (if @(cursor state [:in-modal])
                                                    (fn [] (do
                                                             #_(init-selfie-badge @(cursor state [:badge :id]) state)
                                                             (reset! (cursor state [:tab]) nil)
                                                             (reset! (cursor state [:tab-no]) 1)))

                                                    (fn [] (if (pos? @(cursor state [:badge :issue_to_self]))
                                                             (navigate-to "/badge")
                                                             (navigate-to "/badge/selfie")))))}

             (t :badgeIssuer/Saveandexit)]
            ;;Saveandissue
             ;(when (not @(cursor state [:in-modal]))
            [:a.btn.btn-primary.btn-bulky
             {:href "#"
              :on-click #(save-selfie-badge state (if @(cursor state [:in-modal])
                                                    (fn []
                                                      (swap! state assoc :tab nil :tab-no 2))
                                                    (fn []
                                                      (do
                                                       (navigate-to "/badge/selfie")
                                                       (mo/open-modal [:selfie :view] {:badge (:badge @state) :tab 2})))))}
             (t :badgeIssuer/Saveandissue)]


           ;;cancel
            [:div.btn-group
             [:a.btn.btn-danger.btn-bulky
               {:href "#"
                :on-click #(do
                             (.preventDefault %)
                             (if @(cursor state [:in-modal])
                               (do
                                 #_(init-selfie-badge @(cursor state [:badge :id]) state)
                                 (reset! (cursor state [:tab]) nil)
                                 (reset! (cursor state [:tab-no]) 1))
                               (navigate-to "/badge/selfie")))}
               [:span (t :core/Cancel)]]]]]]]])}))

#_(defn bottom-navigation [step state]
      (create-class
       {:reagent-render
        (fn [step]
          [:div.bottom-navigation
           [:hr.border]
           [:div.row
            [:div.col-md-12
                 ;;previous
             (when (pos? @step) [:a {:href "#"
                                     :on-click #(do
                                                  (.preventDefault %)
                                                  (reset! step (dec @step)))}
                                 [:div {:id "step-button-previous"}
                                  (t :core/Previous)]])
                 ;;preview
             (when (and  (not @(cursor state [:in-modal])) (< @step 2))
               [:a.btn.btn-success.btn-bulky
                {:href "#"
                 :on-click #(do
                              (.preventDefault %)
                              (mo/open-modal [:selfie :preview] {:badge (assoc (:badge @state) :criteria (if @editor
                                                                                                          (.previewRender (.-options @editor) (get-in @state [:badge :criteria] ""))
                                                                                                          (get-in @state [:badge :criteria] "")))}))}
                [:span  (t :badgeIssuer/Preview)]])

                    ;;Saveandexit
             (when (>= @step 1)
               [:a.btn.btn-primary.btn-bulky
                {:href "#"
                 :on-click #(save-selfie-badge state (if @(cursor state [:in-modal])
                                                       (fn [] (do
                                                                #_(init-selfie-badge @(cursor state [:badge :id]) state)
                                                                (reset! (cursor state [:tab]) nil)
                                                                (reset! (cursor state [:tab-no]) 1)))

                                                       (fn [] (if (pos? @(cursor state [:badge :issue_to_self]))
                                                                (navigate-to "/badge")
                                                                (navigate-to "/badge/selfie")))))}

                (t :badgeIssuer/Saveandexit)])

                  ;;cancel
             (when (not= @step 2)
               [:a.btn.btn-danger.btn-bulky
                {:href "#"
                 :on-click #(do
                              (.preventDefault %)
                              (if @(cursor state [:in-modal])
                                (do
                                  #_(init-selfie-badge @(cursor state [:badge :id]) state)
                                  (reset! (cursor state [:tab]) nil)
                                  (reset! (cursor state [:tab-no]) 1))
                                (navigate-to "/badge/selfie")))}
                [:span (t :core/Cancel)]])

                   ;;save
             #_(when (= 2 @step)
                 [:a.btn.btn-primary
                  {:href "#"
                   :on-click #(save-selfie-badge state nil)}
                  (t :badgeIssuer/Save)])

                    ;;next
             (when (< @step 2)
               [:a {:href "#"
                    :on-click #(do
                                 (.preventDefault %)
                                 (reset! step (inc @step))
                                 #_(if (pos? @step)
                                     (save-selfie-badge state (fn [] (reset! step (inc @step))))
                                     (reset! step (inc @step))))}
                [:div.pull-right {:id "step-button"}
                 (t :core/Next)]])]]])
        :component-did-mount (fn [] (reset! step 0))}))

(defn badge-image [badge]
  (let [{:keys [name image]} badge]
    [:div {:class "col-md-3"}
     [:div.badge-image
      [:img {:src (if (re-find #"^data:image" image)
                    image
                    (str "/" image))
             :alt name}]]]))

(defn badge-content [badge]
 (let [{:keys [name description tags criteria_html image criteria]} badge
       criteria (or criteria_html criteria)]
  [:div
   [:div.row
    [:div {:class "col-md-12"}
     [:h1.uppercase-header name]
     [:div.description description]]]

   (when-not (blank? criteria)
     [:div {:class "row criteria-html"}
      [:div.col-md-12
       [:h2.uppercase-header (t :badge/Criteria)]
       [:div {:dangerouslySetInnerHTML {:__html criteria #_(or criteria_html criteria)}}]]])

   [:div.row
    (if (not (empty? tags))
      (into [:div.col-md-12 {:style {:margin "10px 0"}}]
            (for [tag tags]
              [:span {:id "tag"
                      :style {:font-weight "bold" :padding "0 2px"}}
               (str "#" tag)])))]]))



(defn profile-link-inline [id first_name last_name picture]
  [:div [:a {:href "#"
             :on-click #(mo/open-modal [:profile :view] {:user-id id})}
         [:img.badge-img {:src (profile-picture picture) :alt ""}]
         (str first_name " " last_name)]])

(defn logo []
  [:div.col-md-8.col-md-offset-2 {:style {:margin "40px 0"}}
   [:div {:class "logo-image logo-image-url img-responsive"
          :title "OBP logo"
          :aria-label "OBP logo"}]])

(defn stamp []
  (let [site-name (session/get :site-name)]
   [:span.label.label-info
    (str (t :badgeIssuer/Createdandissued) " " site-name)]))


(defn evidence-list [ev-atom state]
  (let [evidence-name-atom (cursor ev-atom [:name])
        evidence-narrative-atom (cursor ev-atom [:narrative])
        evidence-container (cursor state [:all_evidence])]
    (fn []
      (when (seq @evidence-container)
        ^{:key @evidence-container}
        [:div {:style {:margin "20px 0"}}
         [:div.col-md-12
          [:h4 (t :badge/Evidence)]
          (reduce (fn [r evidence]
                    (let [{:keys [narrative description name id url mtime ctime properties]} evidence
                          ;id (-> (make-random-uuid) (uuid-string))
                          {:keys [resource_id resource_type mime_type hidden]} properties
                          added-by-user? (and (not (blank? description)) (starts-with? description "Added by badge recipient"))
                          desc (cond
                                 (not (blank? narrative)) narrative
                                 (not added-by-user?) description ;;todo use regex to match description
                                 :else nil)]
                      (conj r
                            (when (and (not (blank? url)) (url? url))
                              [:div.panel.panel-default.evidence-coded-panel
                               [:div.panel-heading {:id (str "heading" id)
                                                    :role "tab"}
                                [:div.panel-title
                                 #_[:span.label.evidence-draft (t :badgeIssuer/Evidencedraft)]
                                 [:div.url.row.flip [:div.col-md-1 [evidence-icon {:type resource_type :mime_type mime_type}]]
                                  [:div.col-md.11.break (case resource_type
                                                          "file" (hyperlink url)
                                                          "page" (if (session/get :user)
                                                                   [:a {:href "#"
                                                                        :on-click #(do
                                                                                     (.preventDefault %)
                                                                                     (mo/open-modal [:page :view] {:page-id resource_id}))} url]
                                                                   (hyperlink url))
                                                          (hyperlink url))]]
                                 (when-not (blank? name) [:div.inline [:span._label (t :badge/Name) ": "] name])
                                 (when-not (blank? desc) [:div [:span._label (t :admin/Description) ": "]   desc])]

                                [:div
                                 [:div [:button {:type "button"
                                                 :aria-label "OK"
                                                 :class "close panel-edit"
                                                 :on-click #(do (.preventDefault %)
                                                                (init-evidence-form evidence state true))
                                                 :role "button"
                                                 :data-toggle "collapse"
                                                 :data-target (str "#collapse" id)
                                                 :data-parent "#accordion"
                                                 :href (str "#collapse" id)
                                                 :aria-expanded "true"}
                                           [:i.fa.fa-edit.edit-evidence]]];;edit-button

                                 [:div
                                  [:button.close
                                   {:type "button"
                                    :aria-label "OK"
                                    :data-toggle "collapse"
                                    :on-click #(do (.preventDefault %)
                                                   (reset! evidence-container (remove (fn [e] (= (:id e) (:id evidence))) @evidence-container)))
                                    :aria-expanded "false"
                                    :aria-controls (str "collapse" id)}
                                   [:i.fa.fa-trash.trash]]]]] ;;delete-button


                               [:div.panel-collapse {:id (str "collapse" id)
                                                     :class "collapse"
                                                     :role "tabpanel"
                                                     :aria-labelledby (str "heading" id)}
                                [:div.panel-body.evidence-panel-body {:style {:padding-top "10px"}}
                                 [:div.form-group
                                  [:label.col-md-3 {:for "input-name"} (t :badge/Name)]
                                  [:div.col-md-9
                                   [input {:id (str "name#" id) :name "name" :atom evidence-name-atom :type "text"} nil]]]
                                 [:div.form-group
                                  [:label.col-md-3 {:for "input-narrative"} (t :page/Description)]
                                  [:div.col-md-9
                                   [input {:id (str "description#" id) :name "narrative" :rows 5 :cols 40 :atom evidence-narrative-atom} true]]]
                                ;[:hr]
                                 [:div
                                  [:button {:type         "button"
                                            :class        "btn btn-primary"
                                            :on-click     #(do (.preventDefault %)
                                                               (reset! evidence-container (remove (fn [e] (= (:id e) (:id evidence))) @evidence-container))
                                                               (reset! evidence-container (conj @evidence-container (assoc (:evidence @state) :id (-> (make-random-uuid) (uuid-string))
                                                                                                                                              :properties properties))))


                                            :data-toggle "collapse"
                                            :data-target (str "#collapse" id)}
                                   (t :badge/Save)]]]]]))))

                  [:div {:id "accordion" :class "panel-group evidence-list" :role "tablist" :aria-multiselectable "true"}] @evidence-container)]]))))

(defn evidence-form [global-state]
  (let [state (cursor global-state [:evidence])
        evidence-name-atom (cursor state [:evidence :name])
        evidence-narrative-atom (cursor state [:evidence :narrative])
        evidence-url-atom (cursor state [:evidence :url])
        message (cursor state [:evidence :message])
        input-mode (cursor state [:input_mode])
        {:keys [image name]} (-> @global-state :badge (select-keys [:name :image]))
        evidence-container (cursor global-state [:all_evidence])]
    [:div {:id "badge-settings" :class "row flip"}
     [:div {:class "col-md-3 badge-image modal-left"}
      [:img {:src (str "/" image) :alt name}]]

     [:div {:class "col-md-9 settings-content settings-tab"}
      (if @message [:div @message])
      [:div
       [:div.row {:style {:margin-bottom "10px"}} [:span._label.col-md-9.sub-heading (t :badge/Addnewevidence)]]
       [:div.evidence-info (t :badge/Evidenceinfo)]

       [:div.row.form-horizontal
        [:div
         [:div.col-md-12.btn-toolbar {:style {:margin "10px 0"}}
          [:div.btn-group {:role "group"}
           [:a.btn.btn-primary.btn-bulky {:class (if (= :url_input @input-mode) "active-resource" "")
                                          :href "#"
                                          :type "button"
                                          :aria-label "url input"
                                          :on-click #(do
                                                       (.preventDefault %)
                                                       (toggle-input-mode :url_input state))}
            [:i.fa.fa-link] (t :admin/Url)]
           [:a.btn.btn-primary.btn-bulky {:class (if (= :page_input @input-mode) "active-resource" "")
                                          :href "#"
                                          :on-click #(do
                                                       (.preventDefault %)
                                                       (toggle-input-mode :page_input state))}

            [:i.fa.fa-file-text] (t :page/Pages)]
           [:button.btn.btn-primary.btn-bulky {:class (if (= :file_input @input-mode) "active-resource" "")
                                               :href "#" :on-click #(do
                                                                      (.preventDefault %)
                                                                      (toggle-input-mode :file_input state))}
            [:i.fa.fa-files-o] (t :page/Files)]

           [:a.btn.btn-danger.btn-bulky {:href "#" :on-click #(do
                                                                (.preventDefault %)
                                                                (swap! global-state assoc :tab nil ;[settings-tab-content (dissoc data :evidence) state init-data]
                                                                       :tab-no 2))}

            [:i.fa.fa-remove] (t :core/Cancel)]]]]

        ;;Preview
        (when @(cursor state [:show-preview])
          [:div.preview.evidence-list.col-md-9
           [:div.panel.panel-default
            [:div.panel-heading
             [:div.panel-title
              [:div.url  [evidence-icon @evidence-url-atom (cursor state [:evidence :properties :mime_type])]] (hyperlink @evidence-url-atom)]
             [:div [:button {:type "button"
                             :aria-label "OK"
                             :class "close panel-close"
                             :on-click #(do (.preventDefault %) (swap! state assoc :show-preview false
                                                                       :evidence nil
                                                                       :show-form false))}
                    [:i.fa.fa-trash.trash]]]]
            [:div.panel-body.evidence-panel-body
             (if (and (not @(cursor state [:show-form])) (every? #(blank? %) (vector @evidence-name-atom @evidence-narrative-atom)))
               [:div [:a {:href "#" :on-click #(do
                                                 (.preventDefault %)
                                                 (reset! (cursor state [:show-form]) true))} [:div [:i.fa.fa-plus] (t :badge/Addmoreinfoaboutevidence)]]])
             (when (or  @(cursor state [:show-form]) (not (every? #(blank? %) (vector @evidence-name-atom @evidence-narrative-atom))))

               [:div [:div.form-group
                      [:label.col-md-3 {:for "input-name"} (t :badge/Name)]
                      [:div.col-md-9
                       [input {:name "name" :atom evidence-name-atom :type "text"} nil]]]
                [:div.form-group
                 [:label.col-md-3 {:for "input-narrative"} (t :page/Description)]
                 [:div.col-md-9
                  [input {:name "narrative" :rows 5 :cols 40 :atom evidence-narrative-atom} true]]]])]
            [:div.add
             [:button.btn.btn-primary.btn-bulky
              {:type "button"
               :disabled (not (url? @evidence-url-atom))
               :on-click #(do

                            (.preventDefault %)
                            (when (= @input-mode :url_input)
                              (reset! (cursor state [:evidence :properties :resource_type]) "url"))
                            ;(when-not @(cursor state [:evidence :message])
                            (swap! global-state assoc
                                   :tab nil
                                   :tab-no 2
                                   :all_evidence (reset! evidence-container (conj @evidence-container (-> (:evidence @state) (assoc :id (-> (make-random-uuid) (uuid-string))
                                                                                                                                    :properties (get-in (:evidence @state) [:properties]))
                                                                                                                             (dissoc :message))))))}


              (t :core/Add)]]]])

        ;;url input
        (when (= @input-mode :url_input)
          [:div.preview.evidence-list.col-md-9
           [:div.panel.panel-default
            [:div.panel-heading
             [:div.panel-title
              [:div.url [:i.fa.fa-link]]
              [:label.sr-only {:for "input-evidence-url"} (t :badge/EnterevidenceURLstartingwith)]
              [input {:name "evidence-url" :atom evidence-url-atom :type "url" :placeholder (t :badge/EnterevidenceURLstartingwith)}]]]
            [:div.panel-body.evidence-panel-body
             [:div [:div.form-group
                    [:label.col-md-3 {:for "input-name"} (t :badge/Name)]
                    [:div.col-md-9
                     [input {:name "name" :atom evidence-name-atom :type "text"} nil]]]
              [:div.form-group
               [:label.col-md-3 {:for "input-narrative"} (t :page/Description)]
               [:div.col-md-9;[settings-tab-content data state init-data]

                [input {:name "narrative" :rows 5 :cols 40 :atom evidence-narrative-atom} true]]]]]
            [:div.add
             [:button.btn.btn-primary.btn-bulky
              {:type "button"
               :disabled (not (url? @evidence-url-atom))
               :on-click #(do
                            (.preventDefault %)
                            (reset! (cursor state [:evidence :properties :resource_type]) "url")
                            (when-not @(cursor state [:evidence :message])
                              (swap! global-state assoc
                                     :tab nil
                                     :tab-no 2
                                     :all_evidence (reset! evidence-container (conj @evidence-container (-> (:evidence @state) (assoc :id (-> (make-random-uuid) (uuid-string))
                                                                                                                                      :properties (get-in (:evidence @state) [:properties]))
                                                                                                                               (dissoc :message)))))))}

              (t :core/Add)]]]])
        ;;Resource grid
        (when-not @(cursor state [:show-preview]) [resource-input nil state nil])]]]]))


(defn add-evidence-block [state]
  (let [ev-atom (cursor state [:evidence])
        files-atom (cursor ev-atom [:files])
        pages-atom (cursor ev-atom [:pages])
        issue-tab (:tab @state)]
    [:div#badge-settings.row
     [:div
      [:div.col-md-12 {:class "new-evidence" :style {:margin-left "unset"}}
       [:i.fa.fa-plus.fa-fw {:style {:text-align "left"}}]
       [:a {:href "#"
            :on-click #(do (.preventDefault %)
                           (when (empty? @(cursor state [:files])) (init-resources :file_input files-atom))
                           (when (empty? @(cursor state [:pages])) (init-resources :page_input pages-atom))
                           (reset! ev-atom {:evidence nil
                                            :show-preview false
                                            :input_mode :url_input})

                           (swap! state assoc :tab [evidence-form state]
                                              :tab-no 2))}

        (t :badge/Addnewevidence)]
       [:span.text-muted  [:em (str " - " (t :badgeIssuer/Optional))]]]
      [evidence-list ev-atom state]
      (when (seq @(cursor state [:all_evidence]))
        [:div.col-md-12
         [:hr.border.dotted-border]])]]))
