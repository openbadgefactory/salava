(ns salava.page.schemas
  (:require [schema.core :as s
             :include-macros true] ;; cljs only

            [salava.badge.schemas :refer [Badge Evidence]]
            [salava.file.schemas :refer [File]]))

(def page
  {:id             s/Int
   :name           s/Str
   :description    (s/maybe s/Str)
   :tags           (s/maybe [s/Str])
   :password       (s/maybe s/Str)
   :visible_before (s/maybe s/Int)
   :visible_after  (s/maybe s/Int)
   :theme          (s/maybe s/Int)
   :ctime          s/Int
   :mtime          s/Int
   :padding        (s/maybe s/Int)
   :border         (s/maybe s/Int)
   :visibility     (s/enum "private" "password" "internal" "public")})

(s/defschema PageFile (-> File
                          (dissoc :ctime :mtime :tags)
                          (assoc :file_order s/Int)))

(s/defschema Page (assoc page :badges (s/maybe [(select-keys Badge [:name :image_file])])))

(s/defschema PageSettings (assoc page :user_id s/Int
                                      :first_name s/Str
                                      :last_name s/Str))

(s/defschema HeadingBlock {:type    (s/eq "heading")
                           :size    (s/enum "h1" "h2")
                           :content (s/maybe s/Str)})

(s/defschema BadgeBlock {:type     (s/eq "badge")
                         :format   (s/enum "short" "long")
                         :badge_id (s/maybe s/Int)})

(s/defschema HtmlBlock {:type     (s/eq "html")
                        :content (s/maybe s/Str)})

(s/defschema FileBlock {:type     (s/eq "file")
                        :files (s/maybe [PageFile])})

(s/defschema TagBlock {:type        (s/eq "tag")
                       :tag         (s/maybe (s/constrained s/Str #(and (>= (count %) 1)
                                                                        (<= (count %) 255))))
                       :format      (s/enum "short" "long")
                       :sort        (s/enum "name" "modified")})

(s/defschema ShowcaseBlock {:type (s/eq "showcase")
                            :title  (s/maybe s/Str)
                            :badges [(s/maybe s/Int)]
                            :format      (s/enum "short" "long")})


(s/defschema ViewPage (assoc page :user_id s/Int
                                  :first_name s/Str
                                  :last_name s/Str
                                  :border {:id s/Int :style s/Str :width s/Int :color s/Str}
                                  :tags (s/maybe s/Str)
                                  :owner? s/Bool
                                  :qr_code (s/maybe s/Str)
                                  :blocks [(s/conditional #(= (:type %) "heading") (assoc HeadingBlock :id s/Int
                                                                                                       :block_order s/Int)
                                                          #(= (:type %) "badge") (merge
                                                                                   (assoc BadgeBlock :id s/Int
                                                                                                     :show_evidence (s/maybe s/Int)
                                                                                                     :block_order s/Int)

                                                                                   (select-keys Badge [:name :criteria_content :criteria_url :description
                                                                                                       :image_file :issued_on :issuer_email :issuer_content_id :issuer_content_name
                                                                                                       :issuer_content_url :issuer_image :creator_email :creator_name
                                                                                                       :creator_url :creator_image :evidence_url :show_evidence]))

                                                          #(= (:type %) "html") (assoc HtmlBlock :id s/Int
                                                                                                 :block_order
                                                                                                 s/Int)
                                                          #(= (:type %) "file") (assoc FileBlock :id s/Int :block_order s/Int)
                                                          #(= (:type %) "tag") (assoc TagBlock :id s/Int
                                                                                               :block_order s/Int
                                                                                               :badges [(-> Badge
                                                                                                            (select-keys [:id :name :criteria_content :criteria_url :description
                                                                                                                          :image_file :issued_on :expires_on :visibility :mtime :status :badge_id])
                                                                                                            (assoc :tag (s/maybe s/Str)))])
                                                          #(= (:type %) "showcase") (assoc ShowcaseBlock :id s/Int
                                                                                                         :block_order s/Int
                                                                                                         :badges [ (-> Badge
                                                                                                                      (select-keys [:id :name :image_file :criteria_content :criteria_url :description :creator_name :creator_url
                                                                                                                                    :issuer_content_name :issuer_content_id :issuer_content_url])
                                                                                                                      (assoc :creator_content_id (s/maybe s/Str))
                                                                                                                      (assoc :evidences [(-> Evidence
                                                                                                                                             (select-keys [:url :id :narrative :name])
                                                                                                                                             (assoc :ctime (s/maybe s/Int) :description (s/maybe s/Str) :mtime (s/maybe s/Int)
                                                                                                                                               (s/optional-key :properties) {(s/optional-key :hidden) (s/maybe s/Bool)
                                                                                                                                                                             (s/optional-key :resource_id) (s/maybe s/Int)
                                                                                                                                                                              (s/optional-key :resource_type) (s/maybe s/Str)
                                                                                                                                                                              (s/optional-key :mime_type) (s/maybe s/Str)}))]))])
                                                          #(= (:type %) "profile") {:id s/Int :block_order s/Int :type (s/eq "profile")})]))

(s/defschema EditPageContent {:page   {:id          s/Int
                                       :user_id     s/Int
                                       :name        s/Str
                                       :description (s/maybe s/Str)
                                       :blocks      [(s/conditional #(= (:type %) "heading") (-> HeadingBlock
                                                                                                 (assoc :id s/Int :block_order s/Int)
                                                                                                 (dissoc :size))
                                                                    #(= (:type %) "sub-heading") (-> HeadingBlock
                                                                                                     (assoc :id s/Int :block_order s/Int :type (s/eq "sub-heading"))
                                                                                                     (dissoc :size))
                                                                    #(= (:type %) "badge") (-> BadgeBlock
                                                                                               (assoc :id s/Int
                                                                                                      :block_order s/Int
                                                                                                      :badge (select-keys Badge [:id :name :image_file :description]))
                                                                                               (dissoc :badge_id))
                                                                    #(= (:type %) "html") (assoc HtmlBlock :id s/Int
                                                                                                           :block_order
                                                                                                           s/Int)
                                                                    #(= (:type %) "file") (assoc FileBlock :id s/Int :block_order s/Int)
                                                                    #(= (:type %) "tag") (assoc TagBlock :id s/Int
                                                                                                         :block_order s/Int)
                                                                   #(= (:type %) "showcase") (assoc ShowcaseBlock :id s/Int :block_order s/Int :badges [(-> Badge
                                                                                                                                                          (select-keys [:id :name :image_file]))])
                                                                    #(= (:type %) "profile") {:id s/Int :block_order s/Int :type (s/eq "profile") :fields [(s/maybe s/Str)]})]}
                              :badges [{:id         s/Int
                                        :name       s/Str
                                        :image_file (s/maybe s/Str)
                                        :tags       (s/maybe [s/Str])
                                        :description (s/maybe s/Str)}]
                              :tags   (s/maybe [s/Str])
                              :files  [(dissoc PageFile :file_order)]
                              :profile-tab? s/Bool})

(s/defschema SavePageContent {:name        (s/constrained s/Str #(and (>= (count %) 1)
                                                                      (<= (count %) 255)))
                              :description (s/maybe s/Str)
                              :blocks      [(s/conditional #(= (:type %) "heading") (assoc HeadingBlock (s/optional-key :id) s/Int)
                                                           #(= (:type %) "badge") (assoc BadgeBlock (s/optional-key :id) s/Int)
                                                           #(= (:type %) "html") (assoc HtmlBlock (s/optional-key :id) s/Int)
                                                           #(= (:type %) "file") (assoc FileBlock (s/optional-key :id) s/Int
                                                                                                  :files (s/maybe [s/Int]))
                                                           #(= (:type %) "tag") (assoc TagBlock (s/optional-key :id) s/Int)
                                                           #(= (:type %) "showcase") (assoc ShowcaseBlock (s/optional-key :id) s/Int)
                                                           #(= (:type %) "profile") {:type (s/eq "profile") (s/optional-key :id) s/Int})]})
