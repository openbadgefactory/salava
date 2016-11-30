(ns salava.extra.kirkwood.ui.routes
  (:require [salava.extra.kirkwood.ui.footer :refer [footer]]
            [salava.core.ui.helper :refer [base-path]]
            [salava.core.i18n :as i18n :refer [t]]))

(defn ^:export routes [context]
  {})

(defn ^:export navi [context]
  {"passport/footer" {:footer footer}
   })
