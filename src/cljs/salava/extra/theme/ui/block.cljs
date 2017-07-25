(ns salava.extra.theme.ui.block
  (:require [reagent.session :as session]
            [salava.core.helper :refer [dump]]
            [salava.extra.theme.ui.footer :refer [footer-element]]
            [salava.core.i18n :refer [t]]))


(defn ^:export footer []
  (footer-element)
  )
