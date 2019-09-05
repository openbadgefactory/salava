(ns salava.extra.hpass.block)

(defn favicon [ctx]
  {:icon "/img/extra/hpass/favicon.ico"
   :png "/img/extra/hpass/favicon.png" })

(defn pluginjs [ctx]
 ["/js/extra/hpass/gtm.js"])
