(ns salava.badge.ui.sw
  (:require
            [salava.core.helper :refer [dump]]
            [cemerick.url :as url]))

(def app-cache-name "badge-cache")
(def files-to-cache [

;;                      "/assets/bootstrap/css/bootstrap.min.css"
;;                      "/assets/bootstrap/css/bootstrap-theme.min.css"
;;                      "/assets/font-awesome/css/font-awesome.min.css"
;;                      "/css/rateit/rateit.css"
;;                      "/assets/jquery/jquery.min.js"
;;                      "/assets/bootstrap/js/bootstrap.min.js"
;;                      "https://fonts.googleapis.com/css?family=Halant:300,400,600,700|Dosis:300,400,600,700,800|Gochi+Hand|Coming+Soon|Oswald:400,300,700|Dancing+Script:400,700|Archivo+Black|Archivo+Narrow|Open+Sans:700,300,600,800,400|Open+Sans+Condensed:300,700|Cinzel:400,700&subset=latin,latin-ext"
;;                      "http://assets.pinterest.com/js/pinit.js"
;;                       "/js/rateit/jquery.rateit.min.js?_=1521014736499"
;;                       "/js/salava.js"
                      ])

(defn install-service-worker [e]
  #_(let [scope (.scope js/ServiceWorkerRegistration)]
    (dump scope))
  (js/console.log "[Service Worker] Installing")
  (-> js/caches
      (.open app-cache-name)
      (.then (fn [cache]
               (js/console.log "[Service Worker] caching Shell")
               (.addAll cache files-to-cache))))
      #_(.catch (fn [err]
                (js/console.log "[Service Worker] Some files not cached" err)))

      (.then (fn []
                (js/console.log "[Service Worker] Successfully Installed!")))
  )

(.addEventListener js/self "install" #(.waitUntil % (install-service-worker %)))
#_(.addEventListener js/self "fetch" #(.respondWith % (fetch-cached %)))
#_(.addEventListener js/self "activate" #(.waitUntil % (purge-old-caches %)))
