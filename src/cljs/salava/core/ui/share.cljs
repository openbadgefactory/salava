(ns salava.core.ui.share
  (:require [reagent.core :refer [create-class]]
            [reagent.session :as session]
            [salava.core.helper :refer [dump]]
            [salava.core.ui.helper :refer [private?]]
            [salava.core.i18n :refer [t]]))

(defn google-plus [url]
  (create-class
    {:reagent-render (fn [url]
                       [:a {:href (str "https://plus.google.com/share?url=" url) :target "_blank" :data-action "share" :data-href url :data-annotation "none"}
                       [:i {:class "fa fa-google-plus-square"}]
                       ])
     :should-component-update (fn [] false)}))

(defn share-buttons-element [url title public? is-badge? link-or-embed-atom image-file]
  (let [site-name (session/get-in [:share :site-name])
        hashtag (session/get-in [:share :hashtag])]
    [:div {:id "share"}
     [:div {:id "share-buttons" :class (if-not public? " share-disabled")}
      [:div.share-button
       [:a {:class  "twitter"
            :href   (str "https://twitter.com/intent/tweet?size=medium&count=none&text="
                         (js/encodeURIComponent (str site-name ": " title))
                         "&url=" (js/encodeURIComponent url) "&hashtags=" hashtag)
            :target "_blank"}
        [:i {:class "fa fa-twitter-square"}]]
       ]
                                        ;[:div.share-button
                                        ;[:iframe {:id "tweet-button"
                                        ;         :allowTransparency true
                                        ;        :frameBorder 0
                                        ;       :scrolling "no"
                                        ;      :style {:width "55px"
                                        ;             :height "20px"}
                                        ;    :src (str "https://platform.twitter.com/widgets/tweet_button.html?size=medium&count=none&text="
                                        ;             (js/encodeURIComponent (str "Open Badge Passport: " title))
                                        ;            "&url=" (js/encodeURIComponent url) "&hashtags=OpenBadgePassport")}]]
                                        ;[:div.share-button
                                        ; [:script {:type "IN/Share" :data-url url}]]
      [:div.share-button
       (if false ;is-badge?  remove bade certification
         [:a {:href   (str "https://www.linkedin.com/profile/add?_ed=0_JhwrBa9BO0xNXajaEZH4q5ax3e9v34rhyYLtaPv6h1UAvW5fJAD--ayg_G2AIDAQaSgvthvZk7wTBMS3S-m0L6A6mLjErM6PJiwMkk6nYZylU7__75hCVwJdOTZCAkdv&pfCertificationName=" title "&pfCertificationUrl=" url "&trk=onsite_html" )
              :target "_blank"}
          [:i {:title "LinkedIn Add to Profile" :class "inprofile fa fa-linkedin-square"}]]
         [:div.share-button
          [:a {:href (str "https://www.linkedin.com/shareArticle?mini=true&url=" url "&title=" title "&summary=" (js/encodeURIComponent (str site-name ": " title)) "&source=" hashtag) :target "_blank"}
           [:i {:title "LinkedIn Share" :class "fa fa-linkedin-square"}]]]
         )]
      [:div.share-button
       [google-plus url]]
      [:div.share-button
       [:a {:href (str "https://www.facebook.com/sharer/sharer.php?u=" url) :target "_blank"} [:i {:class "fa fa-facebook-square"}]]]
      [:div.share-button
       (if is-badge?
         [:a {:href            (str "//www.pinterest.com/pin/create/button/?url=" url "&description=" title)
              :data-pin-do     "buttonPin"
              :data-pin-custom "true"
              :data-pin-media  image-file}
          [:i {:class "fa fa-pinterest-square"}]])]
      [:div.share-link
       [:a {:href "#" :on-click #(do (.preventDefault %) (reset! link-or-embed-atom (if (= "link" @link-or-embed-atom) nil "link")))} (t :core/Link)]]
      [:div.share-link
       [:a {:href "#" :on-click #(do (.preventDefault %) (reset! link-or-embed-atom (if (= "embed" @link-or-embed-atom) nil "embed")))} (t :core/Embedcode)]]]
     (if (and public? (= "link" @link-or-embed-atom))
       [:div.linkinput [:input {:class "form-control" :read-only true :type "text" :value url}]])
     (if (and public? (= "embed" @link-or-embed-atom))
       (if is-badge?
         [:div.form-horizontal
          [:div.form-group
           [:label.col-xs-3 (t :core/Imageonly) ":"]
           [:div.col-xs-9 [:input {:class "form-control" :read-only true :type "text" :value (str "<iframe  frameborder=\"0\"  scrolling=\"no\" src=\""url"/embed\" width=\"200\" height=\"270\"></iframe>")}]]]
          ]
         [:div.linkinput [:input {:class "form-control" :read-only true :type "text" :value (str "<iframe width=\"90%\" height=\"560\" src=\""url"/embed\" frameborder=\"0\"></iframe>")}]]))]))


(defn share-buttons [url title public? is-badge? link-or-embed-atom image-file]
  (if (private?)
    [:div ]
    (create-class {:reagent-render      (fn [url title public? is-badge?]
                                          (share-buttons-element url title public? is-badge? link-or-embed-atom image-file))
                   :component-did-mount (fn []
                                          (do
                                            (.getScript (js* "$") "//assets.pinterest.com/js/pinit.js")
                                            (.getScript (js* "$") "//platform.twitter.com/widgets.js")
                                            (js* "delete IN")
                                        ;(.getScript (js* "$") "//platform.linkedin.com/in.js")
                                            (.getScript (js* "$") "https://apis.google.com/js/platform.js")))})))

