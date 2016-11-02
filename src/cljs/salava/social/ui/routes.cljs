(ns salava.social.ui.routes
   (:require [salava.core.ui.layout :as layout]
             [salava.core.i18n :as i18n :refer [t]]
             [salava.social.ui.connections :as c]
             [salava.social.ui.stream :as s]
             [salava.core.ui.helper :refer [base-path]]))

(defn ^:export routes [context]
  {(str (base-path context) "/social") [["" s/handler]
                                        ["/stream" s/handler]
                                        ["/connections" c/handler]]})

(defn ^:export navi [context]
  {(str (base-path context) "/social")                 {:weight 1 :title (t :social/Social) :top-navi true :breadcrumb (t :social/Social " / " :social/Stream)}
   (str (base-path context) "/social/stream")         {:weight 11 :title (t :social/Stream) :site-navi true :breadcrumb (t :social/Social " / " :social/Stream)}
   (str (base-path context) "/social/connections")         {:weight 12 :title (t :social/Connections) :site-navi true :breadcrumb (t :social/Social " / " :social/Connections)}})
