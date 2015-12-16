(ns salava.file.schemas
  (:require [schema.core :as s
             :include-macros true ;; cljs only
             ]))

(s/defschema File {:id s/Int
                   :name s/Str
                   :path s/Str
                   :mime_type s/Str
                   :size s/Int
                   :ctime s/Int
                   :mtime s/Int
                   :tags (s/maybe [s/Str])})


(s/defschema Upload {:status                (s/enum "success" "error")
                     :message               (s/maybe s/Str)
                     :reason                (s/maybe s/Str)
                     (s/optional-key :data) File})