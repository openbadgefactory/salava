(ns salava.gallery.ui.routes
   (:require [salava.core.ui.layout :as layout]
             [salava.core.i18n :as i18n :refer [t]]
             [salava.core.ui.helper :refer [base-path path-for]]
             [salava.gallery.ui.badges :as b]
             ;[salava.gallery.ui.badge-view :as bv]
             [salava.gallery.ui.pages :as p]
             [salava.gallery.ui.profiles :as u]
             [salava.gallery.ui.modal :as modal]
             [salava.gallery.ui.block :as block]))

(defn placeholder [content]
  (fn [site-navi params]
    #(layout/default site-navi content)))

(defn ^:export routes [context]
  {(str (base-path context) "/gallery") [["" b/handler]
                                         ["/badges" b/handler]
                                         [["/badges/" :user-id] b/handler]
                                         [["/badges/" :user-id "/" :badge_id] b/handler]
                                         ["/pages" p/handler]
                                         [["/pages/" :user-id] p/handler]
                                         ["/profiles" u/handler]]})
                                         ;[["/badgeview/" :badge-content-id] bv/handler]

(defn about []
  {:badges {:heading (t :gallery/Gallery " / " :badge/Badges)
            :content [:div
                      [:p.page-tip (t :gallery/Aboutexplorebadges)]
                      [:p (t :gallery/Asharedbadgeis)]
                      [:p (t :badge/AnOpenBadgeIs)]]}

   :pages {:heading (t :gallery/Gallery " / " :page/Pages)
           :content [:div
                     [:p.page-tip (t :gallery/Aboutexplorepages)]
                     [:p (t :gallery/Asharedpageis)]
                     [:p (t :page/Whatispage)]]}

   :profiles {:heading (t :gallery/Gallery " / " :gallery/Profiles)
              :content [:div
                        [:p.page-tip (t :gallery/Aboutprofiles)]]}})


#_(defn ^:export navi [context]
   {(str (base-path context) "/gallery")                {:weight 40 :title (t :gallery/Gallery) :top-navi true :breadcrumb (t :gallery/Gallery " / " :gallery/Sharedbadges)}
    (str (base-path context) "/gallery/badges")         {:weight 41 :title (t :gallery/Sharedbadges) :site-navi true :breadcrumb (t :gallery/Gallery " / " :gallery/Sharedbadges)}
    (str (base-path context) "/gallery/pages")          {:weight 42 :title (t :gallery/Sharedpages) :site-navi true :breadcrumb (t :gallery/Gallery " / " :gallery/Sharedpages)}
    (str (base-path context) "/gallery/profiles")       {:weight 43 :title (t :gallery/Sharedprofiles) :site-navi true :breadcrumb (t :gallery/Gallery " / " :gallery/Sharedprofiles)}
    (str (base-path context) "/gallery/badges/\\d+")    {:breadcrumb (t :gallery/Gallery " / " :gallery/Sharedbadges)}
    (str (base-path context) "/gallery/pages/\\d+")     {:breadcrumb (t :gallery/Gallery " / " :gallery/Sharedpages)}
    (str (base-path context) "/gallery/badgeview/\\w+") {:breadcrumb (t :gallery/Gallery " / " :gallery/Viewbadge)}})

(defn ^:export navi [context]
  {(str (base-path context) "/gallery")                {:weight 40 :title (t :gallery/Gallery) :top-navi true :breadcrumb (t :gallery/Gallery " / " :badge/Badges) :about (:badges (about))}
   (str (base-path context) "/gallery/badges")         {:weight 41 :title (t :badge/Badges) :site-navi true :breadcrumb (t :gallery/Gallery " / " :badge/Badges) :about (:badges (about))}
   (str (base-path context) "/gallery/pages")          {:weight 42 :title (t :page/Pages) :site-navi true :breadcrumb (t :gallery/Gallery " / " :page/Pages) :about (:pages (about))}
   (str (base-path context) "/gallery/profiles")       {:weight 43 :title (t :gallery/Profiles) :site-navi true :breadcrumb (t :gallery/Gallery " / " :gallery/Profiles) :about (:profiles (about))}
   (str (base-path context) "/gallery/badges/\\d+")    {:breadcrumb (t :gallery/Gallery " / " :badge/Badges)}
   (str (base-path context) "/gallery/pages/\\d+")     {:breadcrumb (t :gallery/Gallery " / " :page/Pages)}})
   ;(str (base-path context) "/gallery/badgeview/\\w+") {:breadcrumb (t :gallery/Gallery " / " :gallery/Viewbadge)}


(defn ^:export quicklinks []
  [{:title [:p (t :social/Iwanttofindbadges)]
    :url (str (path-for "/gallery/badges"))
    :weight 6}

   {:title  [:p (t :social/Iwanttofindotherusers)]
    :url (str (path-for "/gallery/profiles"))
    :weight 7}])
