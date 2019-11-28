(ns salava.connections.schemas
  #?(:clj (:require [schema.core :as s]
                    [compojure.api.sweet :refer [describe]])
     :cljs (:require [schema.core :as s :include-macros true])))

(s/defschema badge {:id s/Str
                    :name s/Str
                    :image_file (s/maybe s/Str)
                    :description (s/maybe s/Str)})

(s/defschema badge-connections {:badges [badge]})

(s/defschema issuer {:id s/Str
                     :name s/Str
                     :image_file (s/maybe s/Str)})

(s/defschema issuer-connections {:issuers [issuer]})
