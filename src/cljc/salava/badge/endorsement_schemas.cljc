(ns salava.badge.endorsement-schemas
   #?(:clj (:require [schema.core :as s]
                     [schema.coerce :as c]
                     [compojure.api.sweet :refer [describe]])
      :cljs (:require [schema.core :as s :include-macros true])))

#?(:cljs (defn describe [v _] v))

(def content (describe (s/constrained s/Str #(and (>= (count %) 5)
                                                  (not (clojure.string/blank? %)))) "Content in markdown format"))
(def _name (s/constrained s/Str #(and (>= (count %) 1)
                                      (<= (count %) 255))))

(s/defschema endorsement {:id s/Int
                          :user_badge_id s/Int
                          :content s/Str
                          :status (s/enum "pending" "accepted" "declined")
                          :mtime s/Int})

(s/defschema request (-> endorsement
                         (assoc
                          :status (s/enum "pending" "endorsed" "declined")
                          (s/optional-key  :type) (s/enum "sent_request" "request")
                          (s/optional-key :ctime) s/Int)))

(s/defschema received-user-endorsement-p  (-> endorsement
                                              (assoc :issuer_id (s/maybe s/Int))))

(s/defschema received-user-endorsement (-> received-user-endorsement-p
                                           (assoc
                                             :issuer_name _name
                                             :issuer_url (s/maybe s/Str)
                                             :profile_picture (describe (s/maybe s/Str) "Issuer's image")
                                             :description (s/maybe s/Str)
                                             :name (s/maybe s/Str)
                                             :image_file (s/maybe s/Str))))

(s/defschema given-user-endorsement-p  (-> endorsement
                                           (assoc
                                             :endorsee_id (describe (s/maybe s/Int) "ID of endorsement receiver"))))

(s/defschema given-user-endorsement  (-> given-user-endorsement-p
                                         (assoc
                                           :profile_picture (s/maybe s/Str)
                                           :first_name _name
                                           :last_name _name
                                           :name (describe s/Str "Badge name")
                                           :image_file (describe (s/maybe s/Str) "Badge image")
                                           :description (describe (s/maybe s/Str) "Badge description"))))

(s/defschema sent-request-p (-> request
                                (assoc :requestee_id (describe (s/maybe s/Int) "ID of request receiver"))))

(s/defschema sent-request  (-> sent-request-p
                               (assoc
                                :profile_picture (s/maybe s/Str)
                                :name (s/maybe s/Str)
                                :image_file (s/maybe s/Str)
                                :issuer_name _name)))

(s/defschema received-request-p (-> request
                                      (assoc :requester_id (s/maybe s/Int))))

(s/defschema received-request (-> received-request-p
                                  (assoc
                                    :description (s/maybe s/Str)
                                    :issuer_name s/Str
                                    :issued_on s/Int
                                    :issuer_content_id (s/maybe s/Str)
                                    :first_name _name
                                    :last_name _name
                                    :profile_picture (s/maybe s/Str)
                                    :image_file (s/maybe s/Str)
                                    :name s/Str)))

(s/defschema pending-requests [(s/maybe received-request)])

(s/defschema pending-sent-requests [(s/maybe (-> request
                                               (dissoc :type)
                                               (assoc :user_id (s/maybe s/Int)
                                                      :profile_picture (s/maybe s/Str)
                                                      :first_name _name
                                                      :last_name _name)))])



(s/defschema all-endorsements-p {:given [(s/maybe given-user-endorsement-p)]
                                 :received [(s/maybe received-user-endorsement-p)]
                                 :sent-requests [(s/maybe sent-request-p)]
                                 :requests [(s/maybe received-request-p)]})



(s/defschema all-endorsements {:given [(s/maybe given-user-endorsement)]
                               :received [(s/maybe received-user-endorsement)]
                               :sent-requests [(s/maybe sent-request)]
                               :requests [(s/maybe received-request)]
                               :all-endorsements [(s/maybe s/Any)]})


(s/defschema user-badge-endorsements-p {:endorsements [(s/maybe received-user-endorsement-p)]})

(s/defschema user-badge-endorsement {:endorsements [(s/maybe (-> received-user-endorsement
                                                                 (assoc :profile_visibility (s/enum "internal" "public"))
                                                                 (dissoc :name :image_file :description)))]})

(s/defschema pending-user-endorsements {:endorsements [(-> received-user-endorsement (assoc :ctime s/Int) (dissoc :status :mtime))]})
