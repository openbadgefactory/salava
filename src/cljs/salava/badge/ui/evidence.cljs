(ns salava.badge.ui.evidence
  (:require [salava.core.ui.modal :as mo]
            [reagent.core :refer [cursor atom]]
            [salava.core.i18n :refer [t]]
            [reagent.session :as session]
            [salava.core.ui.helper :refer [plugin-fun path-for hyperlink base-url]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.time :refer [date-from-unix-time]]
            [clojure.string :refer [blank? includes?]]
            [salava.file.icons :refer [file-icon]]))

(defn input [input-data textarea?]
  (let [{:keys [name atom placeholder type error-message-atom rows cols]} input-data]
    [(if textarea? :textarea :input)
     {:class       "form-control"
      :id          (str "input-" name)
      :name        name
      :type         type
      :placeholder placeholder
      :on-change   #(do
                      (reset! atom (.-target.value %))
                      (if error-message-atom(reset! error-message-atom (:message "")))
                      )
      :value       @atom
      :rows rows
      :cols cols}]))

#_(defn input [input-data textarea?]
   (let [{:keys [name atom placeholder type error-message-atom rows cols]} input-data
         opt-map (merge input-data {:class "form-control"
                                    :id (str "input-" name)
                                    :value @atom
                                    :on-change #(do
                                                  (reset! atom (.-target.value %))
                                                  (if error-message-atom(reset! error-message-atom (:message ""))))})]
     [(if textarea? :textarea :input) opt-map]))


(defn toggle-input-mode [key state url?]
  (let [key-atom (cursor state [key])]
    (do
      (if url? (swap! state assoc :resource_input false) (swap! state assoc :url_input false))
      (if @key-atom (swap! state assoc key false) (swap! state assoc key true) ))))

(defn init-user-pages [resource-atom]
  (ajax/GET
    (path-for "/obpv1/page" true)
    {:handler (fn [data]
                (swap! resource-atom assoc :pages data)
                )}))

(defn init-user-files [resource-atom]
  (ajax/GET
    (path-for "/obpv1/file" true)
    {:handler (fn [data]
                (swap! resource-atom assoc :files data)
                )}))

(defn init-resources [resource-atom]
  (do
    (init-user-pages resource-atom)
    (init-user-files resource-atom)))

(defn toggle-selected [resource coll]
  (if (contains? @coll resource)
    (reset! coll (disj @coll resource))
    (swap! coll conj resource)))

(defn resource-grid-element [element-data type coll]
  (let [{:keys [id name path visibility mtime badges ctime mime_type]} element-data
        page-url (str (session/get :site-url) (path-for (str "/page/view/" id)))
        checked? (if-not (blank? path)
                   (boolean (some #(= path %) @coll))
                   (boolean (some #(= page-url %) @coll) )) ]
    [:div.checkbox
     [:input {:id (str "input-" name)
              :type "checkbox"
              :on-change #(do (toggle-selected (.-target.value %) coll ))
              :value (case type
                       :page page-url
                       :file path)
              :checked checked?
              }]
     (case type
       :page [:a {:href "#" :on-click #(do
                                         (.preventDefault %)
                                         (mo/open-modal [:page :view] {:page-id id}))} name]

       :file [:div [:i {:style {:margin-right "10px"}
                        :class (str "file-icon-large fa " (file-icon mime_type))}]
              [:a {:href (str "/" path) :target "_blank"} name]])]))

(defn selected-resources-block [selected-resources-atom]
  (fn []
    [:div
     (reduce (fn [coll resource]
               (conj coll [:p (hyperlink resource)])
               ) [:div] @selected-resources-atom)
     [:br]]))

(defn toggle-visible-area [visible-area-atom key]
  (if (= key @visible-area-atom) (reset! visible-area-atom nil) (reset! visible-area-atom key)))


(defn resources-grid [state]
  (let [resource-atom (atom {:pages [] :files [] :visible_area nil})
        visible-area-atom (cursor state [:visible_area])
        selected-resources-atom (atom #{})]
    (init-resources resource-atom)
    (fn []
      (let [{:keys [pages files]} @resource-atom
            files (:files files)]
        [:div#badge-stats
                   (when-not (empty? @selected-resources-atom)
        [selected-resources-block selected-resources-atom])
         [:div.panel
          [:div.panel-heading
           [:h3 [:a {:href "#" :on-click #(do (.preventDefault %) (toggle-visible-area visible-area-atom :pages))}(str (t :page/Pages) " : " (count pages))]]]
          (if (= @visible-area-atom :pages)
            (reduce (fn [r p]
                      (conj r
                            [resource-grid-element p :page selected-resources-atom])) [:div.panel-body] pages))
          ]
         [:div.panel
          [:div.panel-heading
           [:h3 [:a {:href "#" :on-click #(do (.preventDefault %) (toggle-visible-area visible-area-atom :files))} (str (t :page/Files) " : " (count files))]]]
          (if (= @visible-area-atom :files)
            (reduce (fn [r p]
                      (conj r
                            [resource-grid-element p :file selected-resources-atom])) [:div.panel-body] files)
            )
          ]]
        )
      )
    ))


(defn evidence-form [data state init-data]
  (let [settings-tab (first (plugin-fun (session/get :plugins) "settings" "settings_tab_content"))
        evidence-name-atom (cursor state [:evidence :name])
        evidence-description-atom (cursor state [:evidence :description])
        evidence-audience-atom (cursor state [:evidence :audience])
        evidence-genre-atom (cursor state [:evidence :genre])
        evidence-url-atom (cursor state [:evidence :url])
        url-input-enabled? (cursor state [:url_input])
        resource-input-enabled? (cursor state [:resource_input])
        {:keys [image_file name]} data]
    [:div {:id "badge-settings" :class "row flip"}
     [:div {:class "col-md-3 badge-image modal-left"}
      [:img {:src (str "/" image_file) :alt name}]]
     [:div {:class "col-md-9 settings-content settings-tab"}
      [:form.form-horizontal
       [:div.form-group
        [:label.col-md-3 "Name"]
        [:div.col-md-9
         [input {:name "name" :atom evidence-name-atom :placeholder "input a name" :type "text"} nil]]]
       [:div.form-group
        [:label.col-md-3 "Audience"]
        [:div.col-md-9
         [input {:name "audience" :atom evidence-audience-atom :placeholder "" :type "text"} nil]]]
       [:div.form-group
        [:label.col-md-3 "Genre"]
        [:div.col-md-9
         [input {:name "genre" :atom evidence-genre-atom :placeholder "" :type "text"} nil]]]

       [:div.form-group
        [:label.col-xs-12 (t :page/Description)]
        [:div.col-xs-12
         [input {:name "Description" :rows 5 :cols 40 :atom evidence-description-atom } true]]]

       [:div.form-group.pages
        [:label.col-xs-12 "Resource"]
        [:div {:style {:margin "10px"}}
         [:a {:style {:margin "10px"} :href "#"  :on-click #(toggle-input-mode :url_input state true)} [:i.fa.fa-link] (t :admin/Url)]
         [:a {:href "#" :on-click #(do (toggle-input-mode :resource_input state nil)
                                     #_(swap! state assoc :tab [resource-tab data state init-data])
                                     ) } [:i.fa.fa-file] (t :page/File)]]

        (if @url-input-enabled?
          [:div.col-md-9
           [input {:name "evidence-url" :atom evidence-url-atom :type "url"}]])
        (if @resource-input-enabled?
          [:div.col-md-9
           [resources-grid state]
           ]
          )
        ]

       [:div
        [:button {:type         "button"
                  :class        "btn btn-primary"
                  :on-click     #()}
         (t :badge/Save)]
        [:button {:type "button"
                  :class "btn btn-primary"
                  :on-click #(do (.preventDefault %) (swap! state assoc :tab [settings-tab data state init-data] :tab-no 2))}
         (t :core/Cancel)]]
       ]]]))


(defn evidence-block [data state init-data]
  [:div
   [:div.form-group
    [:label {:class "col-md-12 sub-heading" :for "evidence"} (t :badge/Evidence)]

    [:div.col-md-9
     [:i.fa.fa-plus-square]
     [:a {:href "#" :on-click #(do (.preventDefault %)
                                 (swap! state assoc :tab [evidence-form data state init-data] :tab-no 2))} "Add new evidence"]]
    ]])

(defn evidence-modal [state]
  (fn []
    [:div
     [evidence-form state]
     ]
    )
  )

