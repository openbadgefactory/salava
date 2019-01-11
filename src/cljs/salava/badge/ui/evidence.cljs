(ns salava.badge.ui.evidence
  (:require [salava.core.ui.modal :as mo]
            [reagent.core :refer [cursor]]
            [salava.core.i18n :refer [t]]
            [reagent.session :as session]
            [salava.core.ui.helper :refer [plugin-fun path-for]]
            [salava.core.ui.ajax-utils :as ajax]))

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

(defn toggle-url-input [state]
  (let [show-url-input-atom (cursor state [:enable_url_input])]
    (if @show-url-input-atom (swap! state assoc :enable_url_input false) (swap! state assoc :enable_url_input true))
    ))

(defn user-pages [pages-atom]
  (ajax/GET
    (path-for "/obpv1/page" true)
    {:handler (fn [data]
                (reset! pages-atom data)
                )}))

(defn pages-tab [state]
  (let [pages-atom (atom {})]
    ()
    )

  )


(defn evidence-form [data state init-data]
  (let [settings-tab (first (plugin-fun (session/get :plugins) "settings" "settings_tab_content"))
        evidence-name-atom (cursor state [:evidence :name])
        evidence-description-atom (cursor state [:evidence :description])
        evidence-audience-atom (cursor state [:evidence :audience])
        evidence-genre-atom (cursor state [:evidence :genre])
        evidence-url-atom (cursor state [:evidence :url])
        url-input-enabled? (cursor state [:enable_url_input])
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

       [:div.form-group
        [:label.col-xs-12 "Resource"]
        [:div {:style {:margin "10px"}}
         [:a {:style {:margin "10px"} :href "#"  :on-click #(toggle-url-input state)} [:i.fa.fa-link] (t :admin/Url)]
         [:a {:href "#" } [:i.fa.fa-file] (t :page/File)]]

        (if @url-input-enabled?
          [:div.col-md-9
           [input {:name "evidence-url" :atom evidence-url-atom :type "url"}]])
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

