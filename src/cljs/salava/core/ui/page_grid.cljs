(ns salava.core.ui.page-grid
  (:require [salava.core.ui.helper :refer [path-for]]
            [salava.core.time :refer [date-from-unix-time]]
            [salava.core.i18n :refer [t]]
            [salava.user.ui.helper :refer [profile-picture]]
            [salava.core.ui.modal :as mo]
            [salava.admin.ui.admintool :refer [admintool-gallery-page]]))

(defn page-grid-element [element-data opts]
  (let [{:keys [state init-data type]} opts
        {:keys [id name user_id first_name last_name profile_picture visibility mtime badges]} element-data]
    [:div.col-xs-12.col-sm-6.col-md-4 {:key id}
     (case type
       "basic"      [:div.media.grid-container
                     [:a {:href (path-for (str "/profile/page/view/" id )) :style {:text-decoration "none"}}
                      [:div.media-content

                       [:div.media-body
                        [:div;.flip-modal
                         [:div.media-heading
                          [:p.heading-link name]]
                         [:div.visibility-icon
                          (case visibility
                            "private" [:i {:class "fa fa-lock" :title (t :page/Private)}]
                            "password" [:i {:class "fa fa-lock" :title (t :page/Passwordprotected)}]
                            "internal" [:i {:class "fa fa-group" :title (t :page/Forregistered)}]
                            "public" [:i {:class "fa fa-globe" :title (t :page/Public)}]
                            nil)]
                         [:div.media-description
                          [:div.page-create-date.no-flip
                           (date-from-unix-time (* 1000 mtime) "minutes")]
                          (reduce (fn [r badge] (conj r [:img {:title (:name badge)
                                                               :alt (:name badge)
                                                               :src (str "/" (:image_file badge))}] ) )[:div.page-badges] badges)]
                         ]]]]
                     [:div.media-bottom.flip-modal
                      [:a {:class "bottom-link"
                           :title (t :page/Edit)
                           :href  (path-for (str "/profile/page/edit/" id))}
                       [:i {:class "fa fa-pencil"}]]
                      [:a {:class "bottom-link pull-right"
                           :title (t :page/Settings)
                           :href  (path-for (str "/profile/page/settings/" id))}
                       [:i {:class "fa fa-cog"}]]]]

       "profile" [:div.media.grid-container
                  [:a {:href "#" :on-click #(mo/open-modal [:page :view] {:page-id id}) :style {:text-decoration "none"}}
                   [:div.media-content
                    [:div.media-body
                     [:div.media-heading
                      [:p.heading-link name]]
                     [:div.media-content
                      [:div.page-owner
                       [:p (str first_name " " last_name)]]
                      [:div.page-create-date.no-flip
                       (date-from-unix-time (* 1000 mtime) "minutes")]
                      (reduce (fn [r badge] (conj r [:img {:title (:name badge)
                                                           :alt (:name badge)
                                                           :src (str "/" (:image_file badge))}] ) )[:div.page-badges] (take 4 badges))
                      ]]
                    [:div {:class "media-right"}
                     [:img {:src (profile-picture profile_picture)
                            :alt (str first_name " " last_name)}]]]]]
       "gallery" [:div.media.grid-container
                  [:a {:href "#" :on-click #(mo/open-modal [:page :view] {:page-id id}) :style {:text-decoration "none"}}
                   [:div.media-content
                    [:div.media-body
                     [:div.media-heading
                      [:p.heading-link name]]
                     [:div.media-content
                      [:div.page-owner
                       [:p (str first_name " " last_name)]]
                      [:div.page-create-date.no-flip
                       (date-from-unix-time (* 1000 mtime) "minutes")]
                      (reduce (fn [r badge] (conj r [:img {:title (:name badge)
                                                           :alt (:name badge)
                                                           :src (str "/" (:image_file badge))}] ) )[:div.page-badges] (take 4 badges))
                      ]]

                    [:div {:class "media-right"}
                     [:img {:src (profile-picture profile_picture)
                            :alt (str first_name " " last_name)}]]]]
                  (admintool-gallery-page id "page" state init-data user_id)])]))
