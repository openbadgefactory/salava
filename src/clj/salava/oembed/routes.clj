(ns salava.oembed.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [salava.core.layout :as layout]
            [clojure.string :as string]
            [salava.oembed.core :as oembed]))

(defn route-def [ctx]
  (routes
    (context "/obpv1/oembed" []
             :tags ["oembed"]

             (GET "/" []
                  :query-params [url :- String
                                 {maxheight :- s/Int 270}
                                 {maxwidth  :- s/Int 200}
                                 {format :- String "json"}
                                 {referrer :- String ""}]
                  (if (or (string/blank? format) (= format "json"))
                    (if-let [out (oembed/badge ctx url maxwidth maxheight referrer)]
                      (ok out)
                      (not-found "404 Not Found"))
                    (not-implemented "501 Not Implemented"))))))
