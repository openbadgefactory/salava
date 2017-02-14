(ns salava.extra.application.schemas
  (:require [schema.core :as s
             :include-macros true]  ;; cljs only
            ))

(s/defschema Applications  {:iframe (s/maybe s/Str)
                            :language (s/maybe s/Str)})

(s/defschema BadgeAdvert {:id s/Int
                          :remote_url s/Str
                          :remote_id s/Str
                          :remote_issuer_id s/Str
                          :info s/Str
                          :application_url s/Str
                          :issuer_content_id s/Str
                          :badge_content_id s/Str
                          :criteria_content_id s/Str
                          :kind (s/enum "application" "advert")
                          :country s/Str
                          :not_before s/Int
                          :not_after s/Int
                          :ctime s/Int
                          :mtime s/Int
                          :deleted s/Bool})

(s/defschema BadgeAdvertPublish (-> BadgeAdvert
                                    (dissoc :id :issuer_content_id :badge_content_id
                                            :criteria_content_id :ctime :mtime :deleted)
                                    (assoc :badge s/Str :client s/Str)))

(s/defschema BadgeAdvertUnpublish {:remote_url s/Str
                                   :remote_id s/Str
                                   :remote_issuer_id s/Str})



(s/defschema BadgeAdvertModal (-> BadgeAdvert
                                  (dissoc :issuer_content_id :badge_content_id
                                                       :criteria_content_id :ctime :deleted :remote_url :remote_id :remote_issuer_id)
                                    (assoc :followed s/Int
                                           :image_file s/Str
                                           :issuer_content_url s/Str
                                           :issuer_content_name s/Str
                                           :name s/Str
                                           :tags (s/maybe s/Str))))

(s/defschema BadgeAdverts {:applications [(-> BadgeAdvert
                                               (dissoc :issuer_content_id :badge_content_id
                                                       :criteria_content_id :ctime :deleted :remote_url :remote_id :remote_issuer_id)
                                               (assoc :followed s/Int
                                                      :image_file s/Str
                                                      :issuer_content_url s/Str
                                                      :issuer_content_name s/Str
                                                      :name s/Str
                                                      :tags (s/maybe s/Str)
                                                      :not_after (s/maybe s/Int)
                                                      :not_before (s/maybe s/Int)))]
                           :countries [[(s/maybe s/Str)]]
                           :user-country (s/maybe s/Str)})


