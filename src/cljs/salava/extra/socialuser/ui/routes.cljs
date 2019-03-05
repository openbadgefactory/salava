(ns salava.extra.socialuser.ui.routes
   (:require [salava.core.ui.layout :as layout]
             [salava.core.i18n :as i18n :refer [t]]
             [salava.social.ui.connections :as c]
             [salava.extra.socialuser.ui.block]
             [salava.social.ui.stream :as s]
             [salava.core.ui.helper :refer [base-path]]
             [salava.extra.socialuser.ui.connections :as connections]))

(defn ^:export routes [context]
  {(str (base-path context) "/connections") [["/connections" connections/handler]]})

(defn ^:export navi [context]
  {(str (base-path context) "/connections/connections") {:weight 63 :title (t :connections/Users) :site-navi true :breadcrumb (t :social/Connections " / " :connections/Users)}})
