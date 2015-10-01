(ns user
  (:require [reloaded.repl :refer [system init start stop go reset]]))

(defn set-opts [& {:keys [system opts]
                   :or {system 'salava.core.system/base-system
                        opts {}}}]
  (reloaded.repl/set-init!
    (fn []
      (require (symbol (namespace system)))
      ((resolve system)))))

(set-opts)
