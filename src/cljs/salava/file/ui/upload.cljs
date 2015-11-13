(ns salava.file.ui.upload
  (:require [reagent.core :refer [atom]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [clojure.walk :refer [keywordize-keys]]
            [ajax.core :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.helper :refer [navigate-to]]
            [salava.core.i18n :refer [t]]))

(defn upload-modal [{:keys [status message reason]}]
  [:div
   [:div.modal-header
    [:button {:type "button"
              :class "close"
              :data-dismiss "modal"
              :aria-label "OK"}
     [:span {:aria-hidden "true"
             :dangerouslySetInnerHTML {:__html "&times;"}}]]
    [:h4.modal-title message]]
   [:div.modal-body
    [:div {:class (str "alert " (if (= status "error")
                                  "alert-warning"
                                  "alert-success"))}
     reason]]
   [:div.modal-footer
    [:button {:type "button"
              :class "btn btn-primary"
              :data-dismiss "modal"}
     "OK"]]])

(defn send-file []
  (let [file (-> (.querySelector js/document "input")
                 .-files
                 (.item 0))
        form-data (doto
                    (js/FormData.)
                    (.append "file" file (.-name file)))]
    (ajax/POST
      "/obpv1/file/upload/1"
      {:body    form-data
       :handler (fn [data]
                  (let [data-kws (keywordize-keys data)]
                    (m/modal! (upload-modal data-kws)
                              (if (= (:status data-kws) "success")
                                {:hide #(navigate-to "/page/files")}))))})))

(defn content []
  [:div {:class "file-upload"}
   [m/modal-window]
   [:h2.uppercase-header (t :file/Upload)]
   [:form {:id "form"}
    [:input {:type "file"
             :name "file"
             :on-change #(send-file)
             :accept "image/*"}]]])

(defn handler [site-navi]
  (fn []
    (layout/default site-navi (content))))
