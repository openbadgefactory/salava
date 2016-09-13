(ns salava.social.ui.badge-message
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [clojure.string :refer [trim]]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.user.ui.helper :refer [profile-picture]]
            [salava.core.ui.helper :refer [path-for current-path navigate-to input-valid?]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.core.time :refer [date-from-unix-time]]
            
            ))




(def dummy-messages
  [{:badge_content_id "3368c6f212f79433f2e0c912edb82ae36cf980fa1608fb22d2467769b980b9eb"
    :message "Kukaan muu saanut tätä merkkiä kahdesti?"
    :user {:name "Kaapo kukkonen"
           :image_url nil
           :user_id 3 }
    :ctime "1472646908"
    :mtime "1472646908"
    :id 1}
   {:badge_content_id "3368c6f212f79433f2e0c912edb82ae36cf980fa1608fb22d2467769b980b9eb"
    :message "JEE TESTI LOLOL 324"
    :user {:name "Leo vainio"
           :image_url "file/7/3/9/4/73945210848d01d315cb99beeeb5050efd0a90b425626a6713e0e60b2e8c3841.jpg"
           :user_id 1 }
    :ctime "1472646908"
    :mtime "1472646908"
    :id 2}
   {:badge_content_id "3368c6f212f79433f2e0c912edb82ae36cf980fa1608fb22d2467769b980b9eb"
    :message "sama juttu"
    :user {:name "Leo vainio"
           :image_url "file/7/3/9/4/73945210848d01d315cb99beeeb5050efd0a90b425626a6713e0e60b2e8c3841.jpg"
           :user_id 1 }
    :ctime "1472646908"
    :mtime "1472646908"
    :id 3}
   {:badge_content_id "3368c6f212f79433f2e0c912edb82ae36cf980fa1608fb22d2467769b980b9eb"
    :message "minäkin sain"
    :user {:name "Leo vainio"
           :image_url "file/7/3/9/4/73945210848d01d315cb99beeeb5050efd0a90b425626a6713e0e60b2e8c3841.jpg"
           :user_id 1 }
    :ctime "1472646908"
    :mtime "1472646908"
    :id 4}
   {:badge_content_id "3368c6f212f79433f2e0c912edb82ae36cf980fa1608fb22d2467769b980b9eb"
    :message "JEE TESTI LOLOL aa"
    :user {:name "Leo vainio"
           :image_url "file/7/3/9/4/73945210848d01d315cb99beeeb5050efd0a90b425626a6713e0e60b2e8c3841.jpg"
           :user_id 1 }
    :ctime "1472646908"
    :mtime "1472646908"
    :id 5}
   {:badge_content_id "3368c6f212f79433f2e0c912edb82ae36cf980fa1608fb22d2467769b980b9eb"
    :message "JEE TESTI LOLOL aa"
    :user {:name "Leo vainio"
           :image_url "file/7/3/9/4/73945210848d01d315cb99beeeb5050efd0a90b425626a6713e0e60b2e8c3841.jpg"
           :user_id 1 }
    :ctime "1472646908"
    :mtime "1472646908"
    :id 6}
   {:badge_content_id "3368c6f212f79433f2e0c912edb82ae36cf980fa1608fb22d2467769b980b9eb"
    :message "Olinpa hyvä kun sain tämän merkin."
    :user {:name "Leo vainio"
           :image_url "file/7/3/9/4/73945210848d01d315cb99beeeb5050efd0a90b425626a6713e0e60b2e8c3841.jpg"
           :user_id 1 }
    :ctime "1472646908"
    :mtime "1472646908"
    :id 7}
   {:badge_content_id "3368c6f212f79433f2e0c912edb82ae36cf980fa1608fb22d2467769b980b9eb"
    :message " piste"
    :user {:name "Leo vainio"
           :image_url "file/7/3/9/4/73945210848d01d315cb99beeeb5050efd0a90b425626a6713e0e60b2e8c3841.jpg"
           :user_id 1 }
    :ctime "1472646908"
    :mtime "1472646908"
    :id 8}
   {:badge_content_id "3368c6f212f79433f2e0c912edb82ae36cf980fa1608fb22d2467769b980b9eb"
    :message "Haluaisin avautua teille tästä merkistä: Pidän tätä merkkiä jotenkin ikonina ihmiskunnalle. Merkki symboloi ihmisten nälänhätää ja kaipausta kivikaudelle. Myös viikinkien sukupuuttoon kuoleminen on vaikuttanut vahvasti marjanpoimintaan."
    :user {:name "Leo vainio"
           :image_url "file/7/3/9/4/73945210848d01d315cb99beeeb5050efd0a90b425626a6713e0e60b2e8c3841.jpg"
           :user_id 1 }
    :ctime "1472646908"
    :mtime "1472646908"
    :id 9}
   {:badge_content_id "3368c6f212f79433f2e0c912edb82ae36cf980fa1608fb22d2467769b980b9eb"
    :message "Minun mielestä tämä merkki on ok"
    :user {:name "Leo vainio"
           :image_url "file/7/3/9/4/73945210848d01d315cb99beeeb5050efd0a90b425626a6713e0e60b2e8c3841.jpg"
           :user_id 1 }
    :ctime "1472646908"
    :mtime "1472646908"
    :id 10}
   {:badge_content_id "3368c6f212f79433f2e0c912edb82ae36cf980fa1608fb22d2467769b980b9eb"
    :message "Sain tämän merkin! Se oli yllättävän helppoa."
    :user {:name "Leo vainio"
           :image_url "file/7/3/9/4/73945210848d01d315cb99beeeb5050efd0a90b425626a6713e0e60b2e8c3841.jpg"
           :user_id 1 }
    :ctime "1472646908"
    :mtime "1472646908"
    :id 11}
   {:badge_content_id "3368c6f212f79433f2e0c912edb82ae36cf980fa1608fb22d2467769b980b9eb"
    :message "Pihalla on tähän vuoden aikaan aika paljon lehtiä vielä puissa."
    :user {:name "Leo vainio"
           :image_url "file/7/3/9/4/73945210848d01d315cb99beeeb5050efd0a90b425626a6713e0e60b2e8c3841.jpg"
           :user_id 1 }
    :ctime "1472646908"
    :mtime "1472646908"
    :id 12}
   {:badge_content_id "3368c6f212f79433f2e0c912edb82ae36cf980fa1608fb22d2467769b980b9eb"
    :message "Varmasti pitää paikkansa"
    :user {:name "Leo vainio"
           :image_url "file/7/3/9/4/73945210848d01d315cb99beeeb5050efd0a90b425626a6713e0e60b2e8c3841.jpg"
           :user_id 1 }
    :ctime "1472646908"
    :mtime "1472646908"
    :id 13}
   {:badge_content_id "3368c6f212f79433f2e0c912edb82ae36cf980fa1608fb22d2467769b980b9eb"
    :message "JEE TESTI LOLOL aa"
    :user {:name "Leo vainio"
           :image_url "file/7/3/9/4/73945210848d01d315cb99beeeb5050efd0a90b425626a6713e0e60b2e8c3841.jpg"
           :user_id 1 }
    :ctime "1472646908"
    :mtime "1472646908"
    :id 14}
   {:badge_content_id "3368c6f212f79433f2e0c912edb82ae36cf980fa1608fb22d2467769b980b9eb"
    :message "En kyllä yhtään ymmärrä miksi pitää aina olla jotain kommentoitavaa"
    :user {:name "Leo vainio"
           :image_url "file/7/3/9/4/73945210848d01d315cb99beeeb5050efd0a90b425626a6713e0e60b2e8c3841.jpg"
           :user_id 1 }
    :ctime "1472646908"
    :mtime "1472646908"
    :id 15}
   {:badge_content_id "3368c6f212f79433f2e0c912edb82ae36cf980fa1608fb22d2467769b980b9eb"
    :message "Minun mielestä tämä teksti tässä kentässä on siisti ja kaikkien pitiäisi nähdä aina tämä kenttä. NYt se kun on vain jossain piilossa"
    :user {:name "Leo vainio"
           :image_url "file/7/3/9/4/73945210848d01d315cb99beeeb5050efd0a90b425626a6713e0e60b2e8c3841.jpg"
           :user_id 1 }
    :ctime "1472646908"
    :mtime "1472646908"
    :id 16}
   {:badge_content_id "3368c6f212f79433f2e0c912edb82ae36cf980fa1608fb22d2467769b980b9eb"
    :message "JEE TESTI kenttä joopa jee"
    :user {:name "Leo vainio"
           :image_url "file/7/3/9/4/73945210848d01d315cb99beeeb5050efd0a90b425626a6713e0e60b2e8c3841.jpg"
           :user_id 1 }
    :ctime "1472646908"
    :mtime "1472646908"
    :id 17}])





(defn message-list-item [{:keys [message user ctime id]}]
  [:div {:class "media message-item" :key id}
   [:span {:class "pull-left"}
    [:img {:class "message-profile-img" :src (profile-picture (:image_url user))}]]
   [:div {:class "media-body"}
    [:h4 {:class "media-heading"} (str (:name user) " " (date-from-unix-time (* 1000 ctime) "minutes"))]
    [:span message]]
   ]
  )

(defn message-list [messages]
  [:div {:id "message-list"}
   (doall
    (for [item messages]
      (message-list-item item)))])

(defn message-textarea [state]
  [:div
    [:div {:class "form-group"}
     [:textarea {:class "form-control"}]]
    [:div {:class "form-group pull-right"}
     [:button {:class "btn btn-primary"} "Post new"]]])


(defn refresh-button []
  [:a {:href "#" :class "pull-right"} "Refresh"])

(defn content [state]
  (let [{:keys [messages]} @state]
    [:div
     [:h2 "Message board:"]
     (message-list messages)
     (refresh-button)
     (message-textarea state)]))

(defn init-data [state]
  (swap! state assoc :messages dummy-messages))

(defn badge-message-handler []
  (let [state (atom {:messages [] 
                     :user-id ""
                     :user-name ""
                     :text ""})]
    (init-data state)
    (fn []
      (content state)
      )))
