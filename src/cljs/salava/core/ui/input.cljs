(ns salava.core.ui.input
  (:require
   [cljsjs.simplemde]
   [dommy.core :as dommy :refer-macros [sel1 sel]]
   [reagent.core :refer [atom create-class]]
   [reagent.session :as session]
   [salava.core.i18n :refer [t]]))


(defn text-field [opts]
  (let [{:keys [name atom placeholder password? error-message-atom aria-label]} opts]
    [:input
     {:class       "form-control"
      :id          (str "input-" name)
      :name        name
      :type        (if password? "password" "text")
      :placeholder placeholder
      :on-change   #(do
                      (reset! atom (.-target.value %))
                      (when error-message-atom (reset! error-message-atom (:message ""))))
      :value       @atom
      :aria-label (or aria-label (str "input " name))}]))

(defn file-input [opts]
  (let [{:keys [id upload-fn aria-label style]} opts]
    [:input
     {:type "file"
      :name "file"
      :on-change #(upload-fn)
      :accept "image/png, image/svg+xml"
      :aria-label (or aria-label (t :badge/Browse))
      :style style
      :id id}]))

(def simplemde-toolbar
  #js ["bold"
       "italic"
       "heading-3"
       "quote"
       "unordered-list"
       "ordered-list"
       "link"
       "horizontal-rule"
       "preview"
       "guide"])

(def editor (atom nil))

(defn init-editor [element-id value]
  (reset! editor (js/SimpleMDE. (clj->js {:element (.getElementById js/document element-id)
                                          :toolbar simplemde-toolbar
                                          :status false
                                          :spellChecker false
                                          :forceSync true})))
  (-> (sel1 [".CodeMirror" :textarea])
      (dommy/set-attr! :aria-label "CodeMirror textarea"))
  (.value @editor @value)
  (js/setTimeout (fn [] (.value @editor @value)) 200)
  (.codemirror.on @editor "change" (fn [] (reset! value (.value @editor)))))

(defn markdown-editor
 ([value]
  (create-class
   {:component-did-mount
    (fn []
     (init-editor (str "editor" (-> (session/get :user) :id)) value))
    :reagent-render
    (fn []
      [:div.form-group {:style {:display "block"}}
       [:textarea
        {:class "form-control"
         :id (str "editor" (-> (session/get :user) :id))
         :defaultValue @value
         :on-change #(reset! value (.-target.value %))}]])}))
 ([value element-id]
  (create-class
   {:component-did-mount
    (fn []
     (init-editor element-id value))
    :reagent-render
    (fn []
      [:div.form-group {:style {:display "block"}}
       [:textarea
        {:class "form-control"
         :id element-id
         :defaultValue @value
         :on-change #(reset! value (.-target.value %))}]])})))
