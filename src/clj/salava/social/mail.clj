(ns salava.social.mail
  (:require [salava.social.db :as so]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.core.util :refer [get-full-path]]))


(defn filter-last-checked [events]
  (filter #(nil? (:last_checked %)) events))

(defn html-mail-body-item [text]
  [:tr
   [:td
    {:style
     "text-align: left;font-family: Arial,sans-serif;font-size: 16px;line-height: 150%;color: #333333;padding-top:10px",
     :valign "top",
     :align  "left"}
    text]])

(defn html-mail-body-li [text]
  [:li
   text])

(defn badge-message [item lng]
  (let [new-messages (get-in item [:message :new_messages] ) ]
    (html-mail-body-li (str "\"" (:name item) "\" " ""  (t :social/Emailnewmessage1 lng) " " new-messages " "
                              (if (= 1 new-messages )
                                (t :social/Emailnewcomment lng)
                                (t :social/Emailnewcomments lng)) "."))))



(defn message-events [events lng]
  (let [message-helper (fn [item]
                         (when (and (get-in item [:message :new_messages] )
                                    (< 0 (get-in item [:message :new_messages] ))
                                    (= "message" (:verb item)))
                           (badge-message item lng)))
        message-events (map message-helper events)]
    (if-not (empty? message-events)
      (map message-helper events))))


(defn follow-message [item lng]
  (let [badge-body (if (:badge_count item)
                     (str " " (:badge_count item)" " (if (= 1 (:badge_count item))
                                                       (t :social/Emailbadge lng)
                                                       (t :social/Emailbadges lng))))
        page-body (if (:page_count item)
                    (str  " " (:page_count item) " " (if (= 1 (:page_count item))
                                                       (t :social/Emailpage lng)
                                                       (t :social/Emailpages lng))))]
    (html-mail-body-li
     (str  (:first_name item) " " (:last_name item) " "
          (t :social/Emailhaspublish lng) 
          badge-body
          (if (and badge-body page-body)
            (str " " (t :social/Emailand lng)))
          page-body
         
          ".")))
  
  )

(defn follow-events
  "create message events example:
  ([:li Test user has published 1 badge and 2 pages])"
  [events lng]
  (let [follow-events (filter #(= "publish" (:verb %)) events)
        helper (fn [current item]
                 (let [key [(:subject item)]
                       typecount (keyword (str (:type item) "_count" ))]
                   (-> current
                       (assoc-in [key :first_name] (:first_name item))
                       (assoc-in [key :last_name] (:last_name item))
                       (assoc-in  [key typecount]  (inc (get-in current [key typecount] 0))))))
        
        reduced-events (vals (reduce helper {} (reverse follow-events)))]
    (if-not (empty? follow-events)
      (map (fn [item] (follow-message item lng)) reduced-events))))


(defn admin-events
  "create message events example:
  ([:li Open notifications in the platform 1.])"
  [events lng]
  (let [admin-events (filter #(= "ticket" (:verb %)) events)]
    (if (pos? (count admin-events))
      (str (t :social/Emailadmintickets lng) " " (count admin-events) "." ))
    ))

(defn events [ctx user lng]
  (let [events (or (filter-last-checked (so/get-all-events ctx (:id user))) nil)
        message-events (message-events events lng)
        follow-events (follow-events events lng)
        admin-events (admin-events events lng)]
    (if (or admin-events follow-events message-events)
      (html-mail-body-item [:ul
                            (if-not (empty? admin-events)
                              admin-events)                            
                            (if-not (empty? follow-events)
                              follow-events)
                            (if-not (empty? message-events)
                                message-events)]))))





(defn email-new-messages-block [ctx user lng]
  (let [admin? (= "admin" (:role user))
        events (events ctx user lng)
        social-url (str (get-full-path ctx) "/social")]
    (if (not (empty? events)) 
      [:table
       {:width "100%", :border "0", :cellspacing "0", :cellpadding "0"}
       (html-mail-body-item  [:strong (str (t :user/Emailnotificationtext2 lng) ":")] )
       events
       (html-mail-body-item [:div (str (t :user/Emailnotificationtext3 lng) " ") [:a {:href social-url  :target "_blank"} (t :badge/Gohere lng)] "."])])))




(defmulti get-fragment #(last %&))

(defmethod get-fragment "email-notifications" [ctx user lng type]
  (email-new-messages-block ctx user lng))

(defmethod get-fragment "email-notifications-footer" [ctx user lng type]
  [:table
   {:style       "max-width: 640px;margin-left:auto;margin-right: auto;",
    :align       "center",
    :cellspacing "0",
    :cellpadding "0",
    :width       "100%",
    :border      "0"}
   [:tr [:td {:style "font-size: 1px;line-height: 15px;"} " "]]
   [:tr
    [:td
     [:table
      {:style       "max-width: 640px;margin-left:auto;margin-right: auto;",
       :align       "center",
       :cellspacing "0",
       :cellpadding "0",
       :width       "100%",
       :border      "0"}
      [:tr
       [:td.emailPoweredBy
        {:style  "padding-top: 13px; padding-bottom: 40px; ",
         :valign "top",
         :align  "left"}
        [:div {:style " font-family: Arial,sans-serif; color: #686868; font-size: 14px;"} (str (t :user/Emailnotificationunsubscribetext  lng) " ") [:a {:href (str (get-full-path ctx) "/user/edit")  :target "_blank"} (t :badge/Gohere lng)] "."]
        ]]]]]])
