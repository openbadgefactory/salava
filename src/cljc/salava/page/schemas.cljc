(ns salava.page.schemas
  (:require [schema.core :as s
             :include-macros true ;; cljs only
             ]
            [salava.badge.schemas :refer [Badge]]))

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

(def edit-page-content-block
  {:type                      (s/enum "heading" "sub-heading" "badge" "html" "file" "tag")
   :block_order               s/Int
   :id                        s/Int
   (s/optional-key :content)  (s/maybe s/Str)
   (s/optional-key :size)     (s/enum "h1" "h2")
   (s/optional-key :badge_id) (s/maybe s/Int)
   (s/optional-key :format)   (s/enum "short" "long")
   (s/optional-key :tag)      (s/maybe s/Str)
   (s/optional-key :sort)     (s/enum "name" "modified")
   (s/optional-key :files)    [{:id s/Int
                                :name s/Str
                                :path s/Str
                                :size s/Int
                                :mime_type s/Str
                                :file_order s/Int}]
   (s/optional-key :badge)    (select-keys Badge [:id :name :image_file])})

(s/defschema Page (assoc page :badges (s/maybe [(select-keys Badge [:name :image_file])])))

(s/defschema ViewPage (assoc page :user_id s/Int
                                  :first_name s/Str
                                  :last_name s/Str
                                  :border {:id s/Int :style s/Str :width s/Int :color s/Str}
                                  :blocks [(s/conditional #(= (:type %) "heading") {:type    (s/eq "heading")
                                                                                    :id s/Int
                                                                                    :block_order s/Int
                                                                                    :size    (s/enum "h1" "h2")
                                                                                    :content (s/maybe s/Str)}
                                                          #(= (:type %) "badge") (merge
                                                                                   {:type     (s/eq "badge")
                                                                                    :id s/Int
                                                                                    :block_order s/Int
                                                                                    :format   (s/enum "short" "long")
                                                                                    :badge_id (s/maybe s/Int)}
                                                                                   (select-keys Badge [:name :criteria_markdown :criteria_url :description :image_file :issued_on :issuer_email :issuer_name :issuer_url]))
                                                          #(= (:type %) "html") {:type     (s/eq "html")
                                                                                 :id s/Int
                                                                                 :block_order s/Int
                                                                                 :content (s/maybe s/Str)}
                                                          #(= (:type %) "file") {:type     (s/eq "file")
                                                                                 :id s/Int
                                                                                 :block_order s/Int
                                                                                 :files (s/maybe [{:file_order s/Int
                                                                                                   :id s/Int
                                                                                                   :mime_type s/Str
                                                                                                   :name s/Str
                                                                                                   :path s/Str
                                                                                                   :size s/Int}])}
                                                          #(= (:type %) "tag") {:type        (s/eq "tag")
                                                                                :id          s/Int
                                                                                :block_order s/Int
                                                                                :tag         (s/maybe s/Str)
                                                                                :format      (s/enum "short" "long")
                                                                                :sort        (s/enum "name" "modified")
                                                                                :badges      [(select-keys Badge [:id :name :description :image_file :issued_on :expires_on :visibility :mtime :status :badge_content_id :tag])]})]))

(s/defschema EditPageContent {:page   {:id          s/Int
                                       :user_id     s/Int
                                       :name        s/Str
                                       :description (s/maybe s/Str)
                                       :blocks      [edit-page-content-block]}
                              :badges [{:id         s/Int
                                        :name       s/Str
                                        :image_file s/Str
                                        :tags       (s/maybe [s/Str])}]
                              :tags   (s/maybe [s/Str])
                              :files  [{:id        s/Int
                                        :name      s/Str
                                        :path      s/Str
                                        :mime_type s/Str
                                        :size      s/Int}]})

(s/defschema SavePageContent {:name           (s/both s/Str
                                                      (s/pred #(>= (count %) 1))
                                                      (s/pred #(<= (count %) 255)))
                              :description    (s/maybe s/Str)
                              :blocks         [(s/conditional #(= (:type %) "heading") {:type    (s/eq "heading")
                                                                                        (s/optional-key :id) s/Int
                                                                                        :size    (s/enum "h1" "h2")
                                                                                        :content (s/maybe s/Str)}
                                                              #(= (:type %) "badge") {:type     (s/eq "badge")
                                                                                      (s/optional-key :id) s/Int
                                                                                      :badge_id (s/maybe s/Int)
                                                                                      :format   (s/enum "short" "long")}
                                                              #(= (:type %) "html") {:type     (s/eq "html")
                                                                                     (s/optional-key :id) s/Int
                                                                                     :content (s/maybe s/Str)}
                                                              #(= (:type %) "file") {:type     (s/eq "file")
                                                                                     (s/optional-key :id) s/Int
                                                                                     :files (s/maybe [Long])}
                                                              #(= (:type %) "tag") {:type     (s/eq "tag")
                                                                                    (s/optional-key :id) s/Int
                                                                                    :tag (s/maybe (s/both s/Str
                                                                                                           (s/pred #(>= (count %) 1))
                                                                                                           (s/pred #(<= (count %) 255))))
                                                                                    :format (s/enum "short" "long")
                                                                                    :sort (s/enum "name" "modified")})]})