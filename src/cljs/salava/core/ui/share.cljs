(ns salava.core.ui.share
  (:require [reagent.core :refer [create-class]]
            [salava.core.i18n :refer [t]]))

(defn share-buttons-element [url title public? is-badge? link-or-embed-atom]
  [:div {:id "share"}
   [:div {:id "share-buttons" :class (if-not public? " share-disabled")}
    [:div.share-button
     [:iframe {:id "tweet-button"
               :allowTransparency true
               :frameBorder 0
               :scrolling "no"
               :style {:width "55px"
                       :height "20px"}
               :src (str "https://platform.twitter.com/widgets/tweet_button.html?size=medium&count=none&text="
                         (js/encodeURIComponent (str "Open Badge Passport: " title))
                         "&url=" (js/encodeURIComponent url) "&hashtags=OpenBadgePassport")}]]
    [:div.share-button
     [:script {:type "IN/Share" :data-url url}]]
    [:div.share-button
     [:div.g-plus {:data-action "share" :data-href url :data-annotation "none" :data-height "20px"}]]
    [:div.share-button
     [:a {:href (str "https://www.facebook.com/sharer/sharer.php?u=" url) :target "_blank"} [:img.fb-share {:src "/img/fb_share.png"}]]]
    [:div.share-button
     (if is-badge?
       [:a {:href (str "//www.pinterest.com/pin/create/button/?url=" url "&description=" title)
            :data-pin-do "buttonPin"
            :data-pin-config "red"}
        [:img {:src "//assets.pinterest.com/images/pidgets/pinit_fg_en_rect_red_20.png"}]])]
    [:div.share-link
     [:a {:href "" :on-click #(reset! link-or-embed-atom (if (= "link" @link-or-embed-atom) nil "link"))} (t :core/Link)]]
    [:div.share-link
     [:a {:href "" :on-click #(reset! link-or-embed-atom (if (= "embed" @link-or-embed-atom) nil "embed"))} (t :core/Embedcode)]]]
   (if (and public? (= "link" @link-or-embed-atom))
     [:div [:input {:class "form-control" :disabled true :type "text" :value url}]])
   (if (and public? (= "embed" @link-or-embed-atom))
     (if is-badge?
       [:div.form-horizontal
        [:div.form-group
         [:label.col-xs-3 (t :core/Alldetails) ":"]
         [:div.col-xs-9 [:input {:class "form-control" :disabled true :type "text" :value (str "<iframe width=\"90%\" height=\"560\" src=\""url"/embed\" frameborder=\"0\"></iframe>")}]]]
        [:div.form-group
         [:label.col-xs-3 (t :core/Imageonly) ":"]
         [:div.col-xs-9 [:input {:class "form-control" :disabled true :type "text" :value (str "<iframe width=\"90%\" height=\"320\" src=\""url"/embed/pic\" frameborder=\"0\"></iframe>")}]]]]
       [:div [:input {:class "form-control" :disabled true :type "text" :value (str "<iframe width=\"90%\" height=\"560\" src=\""url"\" frameborder=\"0\"></iframe>")}]]))])

(defn share-buttons [url title public? is-badge? link-or-embed-atom]
  (create-class {:reagent-render      (fn [url title public? is-badge?]
                                        (share-buttons-element url title public? is-badge? link-or-embed-atom))
                 :component-did-mount (fn []
                                        (do
                                          (.getScript (js* "$") "//assets.pinterest.com/js/pinit.js")
                                          (js* "delete IN")
                                          (.getScript (js* "$") "//platform.linkedin.com/in.js")
                                          (.getScript (js* "$") "https://apis.google.com/js/platform.js")))}))

