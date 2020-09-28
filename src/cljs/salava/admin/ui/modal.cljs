(ns salava.admin.ui.modal
  (:require
   [reagent.core :refer [atom cursor]]
   [reagent.session :as session]
   [salava.core.ui.grid :as g]
   [salava.core.ui.helper :refer [plugin-fun]]
   [salava.core.ui.modal :as mo]
   [salava.gallery.ui.badges :as gallery]
   [salava.core.i18n :refer [t]]
   [salava.admin.ui.report :as report]))
   ;[salava.extra.spaces.ui.report :as report]))

#_(defn- add-or-remove [x coll]
     (if (some #(= x %) @coll)
       (reset! coll (->> @coll (remove #(= x %)) vec))
       (reset! coll (conj @coll x))))


#_(defn badge-grid-element [badge state]
   (let [{:keys [recipients image_file name id selfie_id badge_id gallery_id issuer_content_name description]} badge
         badge-filters (cursor state [:filters :badges])]
    [:div {:class "media grid-container" :style {:position "relative"}}
     [:input.pull-right
      {:id (str "checkbox-"gallery_id)
       :type "checkbox"
       :name (str "input-checkbox-"gallery_id)
       ;:value (some #(= gallery_id (:gallery_id %))  @badge-filters)
       :checked (if (some #(= gallery_id (:gallery_id %))  @badge-filters) true false)
       :on-change #(add-or-remove badge badge-filters)}]
     [:a {:href "#"
          :on-click #(do
                       (.preventDefault %)
                       (mo/open-modal [:gallery :badges] {:badge-id badge_id :gallery-id gallery_id} {:hidden (fn [] (reset! (cursor state [:select-all]) false))}))

          :title name}
      [:div.media-content
       (if image_file
         [:div.media-left
          [:img {:src (str "/" image_file)
                 :alt (str (t :badge/Badge) " " name)}]])
       [:div.media-body
        [:div.media-heading
         [:p.heading-link name]]
        [:div.media-issuer
         [:p (when-not (clojure.string/blank? selfie_id)
              [:i.fa.fa-user.fa-fw.fa-lg {:title (str (t :badgeIssuer/Createdandissued) " " (session/get :site-name))
                                          :aria-label (str (t :badgeIssuer/Createdandissued) " " (session/get :site-name))}])
          issuer_content_name]]]]]]))

#_(defn gallery-grid-form [state]
    (let [show-advanced-search (cursor state [:advanced-search])]
      [:div {:id "grid-filter"
             :class "form-horizontal"}
       [:div
        #_[gallery/country-selector state]
        [:div
         [:a {:on-click #(reset! show-advanced-search (not @show-advanced-search))
              :href "#"}
          (if @show-advanced-search
            (t :gallery/Hideadvancedsearch)
            (t :gallery/Showadvancedsearch))]]
        (when @show-advanced-search
          [:div
           [gallery/autocomplete state]
           [gallery/text-field :badge-name (t :gallery/Badgename) (t :gallery/Searchbybadgename) state]
           [gallery/text-field :recipient-name (t :gallery/Recipient) (t :gallery/Searchbyrecipient) state]
           [gallery/text-field :issuer-name (t :gallery/Issuer) (t :gallery/Searchbyissuer) state]])]
       [g/grid-radio-buttons (str (t :core/Order) ":") "order" (gallery/order-radio-values) [:params :order] state gallery/fetch-badges]
       (into [:div]
        (for [f (plugin-fun (session/get :plugins) "block" "gallery_checkbox")]
         (when (ifn? f) [f state (fn [] (gallery/fetch-badges state))])))]))

#_(defn load-more [state]
    (if (pos? @(cursor state [:results :user_count]))
       [:div {:style {:margin "10px 0"}}
        (if @(cursor state [:fetching-more])
          [:span [:i.fa.fa-cog.fa-spin.fa-lg.fa-fw] (str (t :core/Loading) "...")]
          [:span [:a {:href     "#"
                      :id    "loadmore"
                      :on-click #(do
                                   (reset! (cursor state [:fetching-more]) true)
                                   (fetch-more state)
                                   ;(init-data state)
                                   (.preventDefault %))}

                   (str (t :social/Loadmore) " (" @(cursor state [:results :user_count])) " " (t :admin/rowleft) ")"]])]))

#_(defn gallery-grid [state]
    (let [badges (:badges @state)]
      [:div#badges (into [:div {:class "row wrap-grid"
                                :id    "grid"}]
                         (for [element-data badges]
                           (badge-grid-element element-data state))) ;"pickable" gallery/fetch-badges)))
       (gallery/load-more state)]))

#_(defn badges-modal [state]
   (fn []
    [:div#badge-gallery
     [:div.col-md-12
      [gallery-grid-form state]
      [:div {:style {:background-color "ghostwhite" :margin "10px auto" :padding "8px"}}
         ;[:p [:b (t :admin/selectallbadgesinstruction)]]
         [:label
           [:input
            {:style {:margin "0 5px"}
             :type "checkbox"
             :default-checked @(cursor state [:select-all-badges])
             :on-change #(do
                           (reset! (cursor state [:filters :badges]) [])
                           (reset! (cursor state [:select-all-badges]) (not @(cursor state [:select-all-badges])))
                           (when @(cursor state [:select-all-badges])
                             (reset! (cursor state [:filters :badges])  (:badges @state))))}]
             ;:disabled (pos? @(cursor state [:badge_count]))}]
           [:b (t :admin/Selectallvisiblebadges)]]]

      (if (:ajax-message @state)
        [:div.ajax-message
         [:i {:class "fa fa-cog fa-spin fa-2x "}]
         [:span (:ajax-message @state)]]
        [gallery-grid state])

      [:div.well.well-sm.text-center
       [:button.btn.btn-primary.btn-bulky
        {:aria-label (t :core/Continue) :data-dismiss "modal"}
        (t :core/Continue)]]]]))

(def ^:export modalroutes
  {:report {:badges report/badges-modal}})
