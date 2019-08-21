(ns salava.translator.ui.helper
  (:require [salava.core.i18n :refer [t t+]]
            [reagent.session :as session]))

(defn translate [lang key]
  (if (session/get :user)
    (t key)
    (t+ lang key)))
