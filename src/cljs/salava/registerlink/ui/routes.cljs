(ns salava.registerlink.ui.routes
  (:require [salava.core.ui.layout :as layout]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.registerlink.ui.register-link :as rl]
            [salava.registerlink.ui.register-token :as rt]
            [salava.core.ui.helper :refer [base-path]]))

(defn ^:export routes [context]
  {(str (base-path context) "/admin") [["/register-link" rl/handler]]
   (str (base-path context) "/user") [[["/register/token/" :token ] rt/handler]]})


(defn register-link-navi [context]
  {(str (base-path context) "/admin/register-link") {:weight 54 :title (t :admin/register-link) :site-navi true :breadcrumb (t :admin/Admin " / "  :admin/register-link)}
   })

(defn ^:export navi [context]
  (if (= "admin" (get-in context [:user :role]))
    (register-link-navi context)
    {}))


