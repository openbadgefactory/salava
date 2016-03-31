(ns salava.file.ui.browser
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.file.icons :refer [file-icon]]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]
            [salava.core.time :refer [date-from-unix-time]]))

(defn select-file [file-path callback-id]
  (when js/window.opener
    (js/window.opener.CKEDITOR.tools.callFunction callback-id (str "/" file-path))
    (js/window.close)))

(defn content [state]
  (let [{:keys [files callback]} @state]
    [:div.container-fluid {:id "file-browser"}
     [:h3 (t :file/Choosefile)]
     (into [:div.row]
           (for [file (sort-by :name files)
                 :let [{:keys [path name mime_type]} file
                       image? (re-find #"^image/" (str mime_type))]]
             [:div {:class "col-sm-4 col-md-3 col-lg-2"}
              [:div.thumbnail {:on-click #(do (.preventDefault %) (select-file path callback))}
               [:div.thumbnail-img
                (if image?
                  [:img {:src (str "/" path)}]
                  [:i {:class (str "fa " (file-icon mime_type))}])]
               [:div.thumbnail-name
                [:a {:href "#"}
                 name]]]]))]))

(defn init-data [state]
  (ajax/GET
    "/obpv1/file"
    {:handler (fn [data]
                (swap! state assoc :files data))}))

(defn handler [navi params]
  (let [state (atom {:files []
                     :callback (:callback params)})]
    (init-data state)
    (fn []
      (content state))))