(ns salava.gallery.ui.routes
   (:require [salava.core.ui.layout :as layout]
             [salava.core.i18n :as i18n :refer [t]]
             [salava.core.ui.helper :refer [base-path]]
             [salava.gallery.ui.badges :as b]
             [salava.gallery.ui.badge-view :as bv]
             [salava.gallery.ui.pages :as p]
             [salava.gallery.ui.profiles :as u]
             [salava.gallery.ui.modal :as modal]))

(defn placeholder [content]
  (fn [site-navi params]
    #(layout/default site-navi content)))

(defn ^:export routes [context]
  {(str (base-path context) "/gallery") [["" b/handler]
                                         ["/badges" b/handler]
                                         [["/badges/" :user-id] b/handler]
                                         [["/badges/" :user-id "/" :badge_content_id] b/handler]
                                         ["/pages" p/handler]
                                         [["/pages/" :user-id] p/handler]
                                         ["/profiles" u/handler]
                                         [["/badgeview/" :badge-content-id] bv/handler]]})

(defn ^:export navi [context]
  {(str (base-path context) "/gallery")                {:weight 40 :title (t :gallery/Gallery) :top-navi true :breadcrumb (t :gallery/Gallery " / " :gallery/Sharedbadges)}
   (str (base-path context) "/gallery/badges")         {:weight 41 :title (t :gallery/Sharedbadges) :site-navi true :breadcrumb (t :gallery/Gallery " / " :gallery/Sharedbadges)}
   (str (base-path context) "/gallery/pages")          {:weight 42 :title (t :gallery/Sharedpages) :site-navi true :breadcrumb (t :gallery/Gallery " / " :gallery/Sharedpages)}
   (str (base-path context) "/gallery/profiles")       {:weight 43 :title (t :gallery/Sharedprofiles) :site-navi true :breadcrumb (t :gallery/Gallery " / " :gallery/Sharedprofiles)}
   (str (base-path context) "/gallery/badges/\\d+")    {:breadcrumb (t :gallery/Gallery " / " :gallery/Sharedbadges)}
   (str (base-path context) "/gallery/pages/\\d+")     {:breadcrumb (t :gallery/Gallery " / " :gallery/Sharedpages)}
   (str (base-path context) "/gallery/badgeview/\\w+") {:breadcrumb (t :gallery/Gallery " / " :gallery/Viewbadge)}})

