(ns salava.core.ui.notactivated
  (:require [reagent.session :as session]
            [clojure.string :as str]
            [salava.core.ui.helper :refer [path-for not-activated?]]
            [salava.core.i18n :refer [t]]
            [ajax.core :as ajax]))




(defn not-activated-banner []
  (if (not-activated?)
    [:div {:class (str "alert ""alert-warning")}
     (str (t :core/Notactivedpannerhelptext)  ". ")
     [:a {:href (path-for "/user/edit/email-addresses")}
      (str (t :core/Activatehere) ".")]]
    ))
