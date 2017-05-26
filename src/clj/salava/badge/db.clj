(ns salava.badge.db
  (:require [clojure.tools.logging :as log]
            [yesql.core :refer [defqueries]]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [clojure.data.json :as json]
            [schema.core :as s]
            [salava.core.helper :refer [dump]]
            [salava.badge.schemas :as schemas] ;cljc
            [salava.core.util :as u]))

(defqueries "sql/badge/main.sql")




(def badge-content-fi
  {:language-code "fi"
   :language-name  "Finnish"
   :name           "Open Badge Passport - Jäsen"
   :description    "Merkki on myönnetty henkilölle, joka on ottanut Open Badge Passport -palvelun käyttöön."
   :tags           ["jäsen" "osaamismerkit"]
   :criteria       "Tämä merkki on myönnetty henkilölle, joka on ottanut Open Badge Passport -palvelun käyttöön.

Open Badge Passport on Discendum Oy:n kehittämä ilmainen palvelu, johon käyttäjä voi tallentaa ansaitsemansa Open Badges -osaamismerkit. Palvelussa käyttäjä voi jakaa merkkejä sekä palvelun sisällä muille käyttäjille että palvelun ulkopuolelle sosiaalisen median eri palveluihin. Lisäksi palvelusta löytyy työkalu, jolla käyttäjä voi luoda merkeistä ja muista sisällöistä miniportfoliosivuja, joita voi julkaista muille käyttäjille ja internetiin.

**Hyödylliset linkit**

[Open Badge Passport ](http://openbadgepassport.com)

[Lisätietoa Open Badge Passport -palvelusta](https://openbadgepassport.com/fi/about) 

[Luo oma Open Badge Passport -tunnus](https://openbadgepassport.com/fi/user/register)

[Lisätietoja Open Badges -konseptista](http://openbadges.org/)
"
   :default false
   })

(def badge-content-en
  {:language-code "en"
   :language-name  "English"
   :name           "Open Badge Passport - Member"
   :description    "his Open Badge has been issued to a person, who has created an account to Open Badge Passport / who has taken Open Badge Passport into usage."
   :tags           ["member" "openbadges"]
   :criteria       "This Open Badge has been issued to a person, who has created an account to Open Badge Passport / who has taken Open Badge Passport into usage. 

Open Badge Passport is a free service developed by Discendum, where the user can save / store the Open Badges they’ve earned. The user can share their Open Badges to other users inside the service as well as outside the service to different social media services. Open Badge Passport has also a tool with which the user can create miniportfolio pages consisting of their Open Badges and other content and publish those pages to other users as well as to the internet. 

**Helpful links**

[More information about Open Badge Passport ](https://openbadgepassport.com/en/about)

[More information about the Open Badges -concept](http://openbadges.org)

[Create your own Open Badge Passport account](https://openbadgepassport.com/en/user/register)
"
   :default true
   })



(def badge
  {:id                  19
   :content             [badge-content-en badge-content-fi]
   :image_file          "file/e/4/3/c/e43c48360a8a0a6b75caf225d2fab021b3812dc5032b158fa834ae658a3d9b04.png"
   :issued_on           1493942400
   :expires_on          nil
   :revoked             0
   :visibility          "public"
   :status              "accepted"
   :mtime               1495430902
   :badge_content_id    "d38193c6a77672357edbc147caa4c2ed3e3e6ffe486c7667ad83c39d4aa5146f"
   :issuer_url          "http://localhost:5000/v1/client/?key=OK9XPAaLWVa1&event=OPH43Aa4RGa3&v=1.1"
   :badge_url           "http://localhost:5000/v1/badge/_/OPH42Fa4RGa1.json?v=1.1&event=OPH43Aa4RGa3"
   :obf_url             "http://localhost:5000"
   :issued_by_obf       false
   :verified_by_obf     false
   :issuer_verified     false
   :issuer_content_name "Discendum Oy"
   :issuer_content_url  "http://www.discendum.com"
   })

badge

(defn badge-events-reduce [events]
  (let [helper (fn [current item]
                  (let [key [(:verb item) (:object item)]]
                    (-> current
                        (assoc  key item)
                        ;(assoc-in  [key :count] (inc (get-in current [key :count ] 0)))
                        )))
        reduced-events (vals (reduce helper {} (reverse events)))]
    (filter #(false? (:hidden %)) reduced-events)))





(defn admin-events-reduce [events]
  (let [helper (fn [current item]
                  (let [key [(:verb item)]]
                    (-> current
                        (assoc  key item)
                        (assoc-in  [key :count] (inc (get-in current [key :count ] 0)))
                        )))
        reduced-events (vals (reduce helper {} (reverse events)))]
    (filter #(false? (:hidden %)) reduced-events)))





(defn badge-message-map
  "returns newest message and count new messages"
  [messages]
  (let [message-helper (fn [current item]
                         (let [key  (:badge_content_id item)
                               new-messages-count (get-in current [key :new_messages] 0)]
                           (-> current
                               (assoc key item)
                               (assoc-in [key :new_messages] (if (> (:ctime item) (:last_viewed item))
                                                              (inc new-messages-count)
                                                              new-messages-count)))))]
    (reduce message-helper {} (reverse messages))))


(defn filter-badge-message-events [events]
  (filter #(= "message" (:verb %)) events))

(defn filter-own-events [events user_id]
  (filter #(and (= user_id (:subject %)) (= "follow" (:verb %))) events) )

(defn get-user-badge-events
  "get users badge  message and follow events"
  [ctx user_id]
  (let [events (select-user-events {:user_id user_id} (u/get-db ctx)) ;get all events where type = badge
        reduced-events (badge-events-reduce events) ;bundle events together with object and verb
        badge-content-ids (map #(:object %) reduced-events)
        messages (if (not (empty? badge-content-ids)) (select-messages-with-badge-content-id {:badge_content_ids badge-content-ids :user_id user_id} (u/get-db ctx)) ())
        messages-map (badge-message-map messages)
        message-events (map (fn [event] (assoc event :message (get messages-map (:object event)))) (filter-badge-message-events reduced-events)) ;add messages for nessage event
        follow-events (filter-own-events reduced-events user_id)
        badge-events (into follow-events message-events)]
    badge-events))

(defn get-badge-events [ctx user_id]
  (let [badge-events (get-user-badge-events ctx user_id)
        sorted (take 25 (sort-by :ctime #(> %1 %2) (vec badge-events)))]
    sorted))
;Owners

(defn str->number? [str]
  (try
    (if (number? str)
        true
        (let [n (read-string str)]
          (number? n)))
    (catch Exception e
      false)))

(defn get-owners [ctx object]
  (if (str->number? object) ;if object is badge-id set owner be badges owner
    (select-badge-owner-as-owner {:id object} (u/get-db ctx))
    (select-users-from-connections-badge {:badge_content_id object} (u/get-db ctx))))

(defn- content-id [data]
  (u/map-sha256 (assoc data :id "")))

(defn- save-image [ctx item]
  (if (string/blank? (:image_file item))
    item
    (assoc item :image_file (u/file-from-url ctx (:image_file item)))))


(defn save-criteria-content! [ctx input]
  (s/validate schemas/CriteriaContent input)
  (let [id (content-id input)]
    (insert-criteria-content! (assoc input :id id) (u/get-db ctx))
    id))

(defn save-issuer-content! [ctx input]
  (s/validate schemas/IssuerContent input)
  (let [id (content-id input)]
    (insert-issuer-content! (assoc input :id id) (u/get-db ctx))
    id))

(defn save-creator-content! [ctx input]
  (when input
    (s/validate schemas/CreatorContent input)
    (let [id (content-id input)]
      (insert-creator-content! (assoc input :id id) (u/get-db ctx))
      id)))

(defn save-badge-content! [ctx input]
  (s/validate schemas/BadgeContent input)
  (let [id (content-id input)]
    (jdbc/with-db-transaction  [t-con (:connection (u/get-db ctx))]
      (insert-badge-content! (assoc input :id id) {:connection t-con})
      (doseq [tag (:tags input)]
        (insert-badge-content-tag! {:badge_content_id id :tag tag} {:connection t-con}))
      (doseq [a (:alignment input)]
        (insert-badge-content-alignment! (assoc a :badge_content_id id) {:connection t-con})))
    id))

;;

(defn save-badge! [ctx badge]
  (try
    (let [badge_content_id    (->> (:badge_content badge) (save-image ctx) (save-badge-content! ctx))
          issuer_content_id   (->> (:issuer_content badge) (save-image ctx) (save-issuer-content! ctx))
          criteria_content_id (save-criteria-content! ctx (:criteria_content badge))
          creator_content_id  (->> (:creator_content badge) (save-image ctx) (save-creator-content! ctx))]
      (-> badge
          (dissoc :badge_content :issuer_content :criteria_content :creator_content)
          (assoc    :badge_content_id badge_content_id
                   :issuer_content_id issuer_content_id
                 :criteria_content_id criteria_content_id
                  :creator_content_id creator_content_id)
          (insert-badge<! (u/get-db ctx))))
    (catch Exception ex
      (log/error "save-badge!: failed to save badge data")
      (log/error (.toString ex)))))
