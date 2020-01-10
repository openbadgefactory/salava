(ns salava.social.schemas
  #? (:clj (:require [schema.core :as s]
                     [compojure.api.sweet :refer [describe]])
           :cljs (:require [schema.core :as s :include-macros true])))

#? (:cljs (defn describe [v _] v))

(s/defschema message {:id s/Int
                      :badge_id s/Str
                      :message (s/constrained s/Str #(and (>= (count %) 1)
                                                          (<= (count %) 255)))
                      :ctime s/Int
                      :user_id s/Int
                      :first_name (s/constrained s/Str #(and (>= (count %) 1)
                                                             (<= (count %) 255)))
                      :last_name (s/constrained s/Str #(and (>= (count %) 1)
                                                            (<= (count %) 255)))
                      :profile_picture (s/maybe s/Str)})

(s/defschema social-messages {:messages [(s/maybe message)]
                              :messages_left s/Int})

(s/defschema message-count {:new-messages (describe s/Int "Messages not yet viewed")
                            :all-messages s/Int})

(s/defschema new-message (-> message (select-keys [:user_id :message])))

(s/defschema pending-badge {:id s/Int
                            :description (s/maybe s/Str)
                            :tags [(s/maybe s/Str)]
                            :name s/Str
                            :image_file (s/maybe s/Str)
                            :expires_on (s/maybe s/Int)
                            :badge_id s/Str
                            :issued_on s/Int
                            :assertion_url (s/maybe s/Str)
                            :visibility s/Str
                            :mtime s/Int
                            :png_image_file (s/maybe s/Str)})

(s/defschema pending-badges {:pending-badges [(s/maybe pending-badge)]})
