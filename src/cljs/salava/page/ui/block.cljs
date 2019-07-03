(ns salava.page.ui.block
 (:require [salava.core.ui.ajax-utils :as ajax]
           [reagent.core :refer [atom cursor create-class]]
           [salava.core.ui.helper :refer [path-for]]
           [salava.page.ui.helper :refer [view-page]]))
(defn init-data [page-id state]
    (ajax/GET
      (path-for (str "/obpv1/page/view/" page-id) true)
      {:handler (fn [data]
                  (reset! state (assoc data
                                  :page-id page-id
                                  :show-link-or-embed-code nil
                                  :permission "success"
                                  :badge-small-view false)))}
      (fn [] (swap! state assoc :permission "error"))))


(defn ^:export page [page-id]
 (let [state (atom {})]
  (fn []
     (create-class {:reagent-render (fn [] [view-page (:page @state)])
                    :component-will-mount (fn [] (init-data page-id state))}))))
