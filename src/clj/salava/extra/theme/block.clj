(ns salava.extra.theme.block
  #_(:require [salava.core.i18n :refer [t]]
            [salava.core.util :refer [get-site-url]]))


(defn favicon [ctx]
  (let [favicon (get-in ctx [:config :extra/theme :favicon] nil)]
    (if favicon
          favicon
          {:icon "/img/favicon.icon"
           :png  "/img/favicon.png"})))
