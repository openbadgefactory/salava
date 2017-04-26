(ns salava.core.ui.share
  (:require [reagent.core :refer [atom create-class dom-node]]
            [reagent.session :as session]
            [salava.core.helper :refer [dump]]
            [salava.core.ui.helper :refer [private?]]
            [salava.core.time :refer [date-from-unix-time unix-time]]
            [reagent-modals.modals :as m :refer [close-modal!]]
            [salava.core.i18n :refer [t]]
            ))


(defn clipboard-button [label target status]
  (let [clipboard-atom (atom nil)]
    (create-class
     {:display-name "clipboard-button"
      :component-did-mount
      #(let [clipboard (new js/Clipboard (dom-node %))]
         (reset! clipboard-atom clipboard)
         )
      :component-will-unmount
      #(when-not (nil? @clipboard-atom)
         (.destroy @clipboard-atom)
         (reset! clipboard-atom nil)
         )
      :reagent-render
      (fn []
        [:button {:class "btn btn-primary"
                  :type "button"
                  :data-clipboard-target target
                  :on-click (fn [] (do
                                     (reset! status "alert-success")
                                     (js/setTimeout #(reset! status "") 2000)))}
         label])})))







(defn input-button11 [name id text]
  (let [status (atom "")]
    (fn []
      [:div {:class (str "input-box " @status)}
       [:div.name name]
       [:div
        [:p {:id id} text]
        [clipboard-button "Copy" (str "#" id) status]]]))
  )

(defn input-button [name id text]
  (let [status (atom "")]
    (fn []
      [:div {:class (str "media input-box " @status)}
       [:div.media-body
        [:h4.media-heading name]
        [:div{:id id :class "text"} text]]
       [:div.media-right
        [:div.media-object
         [clipboard-button "Copy" (str "#" id) status]]
        ]]))
   )

(defn input-date [datefrom dateto]
  [:div {:class (str "media input-box")}
   [:div.media-body
    [:h4.media-heading "Time period"]
        [:div (date-from-unix-time (* 1000 datefrom)) "-"  (if dateto (date-from-unix-time (* 1000 dateto))
                                                               "present") ]]
       ])


(defn open-linkedin-popup []
  (.open js/window "https://www.linkedin.com/profile/add?startTask=CERTIFICATION_NAME",
               "_blank",
               "toolbar=yes,scrollbars=yes,resizable=yes"))

(defn add-to-profile-image []
  (let [user-lng (session/get-in [:user :language])
        btn-lngs '("de" "en" "es" "fr" "sv")
        use-lng (if (some #(= % user-lng) btn-lngs) user-lng "en")]
    [:img {:src (str "/img/linkedin-addprofile/" use-lng ".png") :alt "LinkedIn Add to Profile button"}]) )


(defn certificate-badge-helper [{:keys [name authory licence url datefrom dateto] :as certification}]
  [:div.certification
   [:div.row
    [:div.guide-text.col-xs-12
     [:h3 (t :core/Sharelinkedinaddcredit) ]
     [:p (str (t :core/Sharelinkedincopytip) ".")]
     [:div [:a {:href "#" :on-click #(open-linkedin-popup)}
            (add-to-profile-image)]
      [:a {:href "https://www.linkedin.com/profile/add?startTask=CERTIFICATION_NAME" :target "_blank"} (str " " (t :core/Sharelinkedinclickhere) ".")]]]]
     [:div.row
      [:div.col-md-6.copy-boxes         
       [input-button "certification name" "name" name]
       [input-button "Certification authority" "authory" authory]
       [input-button "License number" "licence" licence]
       [input-date datefrom dateto]
       [input-button "Certification url" "url" url]]
      
      [:div.col-md-6.image-view
       [:img.img-responsive.img-thumbnail {:src "/img/certification.png" :alt "linkedin certification img"}]
       ]]]
  )


(defn start-view [url title certification view]
  (let [site-name (session/get-in [:share :site-name])
        hashtag (session/get-in [:share :hashtag])]
    [:div.col-xs-12.certification
     [:div.guide-text
      [:h3  (t :core/Shareonlinkedin)]
      [:div (str (t :core/Sharelinkedintip) "."
                 (t :core/Sharelinkedinprofile) ":") ]]
     [:div.col-xs-12
      [:a {:class "btn btn-oauth btn-linkedin text-center"
           :href "#"
           :on-click (fn []
                       (do
                         (reset! view :addtoprofile)
                         (-> (js* "$('#reagent-modal .modal-dialog')")
                             (.addClass "modal-lg" )
                             (.removeClass "modal-sm"))
                         #_(js/setTimeout #(open-linkedin-popup) 500)
                         )
                       )}
        [:i {:class "fa fa-linkedin"}]
       (t :core/Addtoprofile)]]
     [:div (str (t :core/Sharelinkedinupdate) ":") ]
     [:div.col-xs-12
      [:a {:class "btn btn-oauth btn-linkedin" :href (str "https://www.linkedin.com/shareArticle?mini=true&url=" url "&title=" title "&summary=" (js/encodeURIComponent (str site-name ": " title)) "&source=" hashtag) :rel "nofollow" :target "_blank"}
       [:i {:class "fa fa-linkedin"}]
       (t :badge/Share)]
      ]]))


(defn modal-content [url title certification]
  (let [view (atom :start)]
    (fn []
      
      (case @view
        :start [start-view url title certification view]
        :addtoprofile [certificate-badge-helper certification]
        :default))))

(defn content-modal-render [url title certification]
  [:div 
   [:div.modal-body
    [:div.row
     [:div.col-md-12
      [:button {:type         "button"
                :class        "close"
                :data-dismiss "modal"
                :aria-label   "OK"}
       [:span {:aria-hidden             "true"
               :dangerouslySetInnerHTML {:__html "&times;"}}]]]]
    [modal-content url title certification]]
   [:div.modal-footer
    #_[:button {:type         "button"
              :class        "btn btn-primary"
              :data-dismiss "modal"}
     (t :core/Close)]]])

(defn linkedin-modal [url title certification]
  (create-class {:reagent-render (fn [url title public?] (content-modal-render url title certification))
                 :component-will-unmount (fn [] (close-modal!) )}))



(defn google-plus [url]
  (create-class
    {:reagent-render (fn [url]
                       [:a {:href (str "https://plus.google.com/share?url=" url) :target "_blank" :data-action "share" :data-href url :data-annotation "none"}
                       [:i {:class "fa fa-google-plus-square"}]
                       ])
     :should-component-update (fn [] false)}))




(defn share-buttons-element [url title public? is-badge? link-or-embed-atom image-file certification]
  (let [site-name (session/get-in [:share :site-name])
        hashtag (session/get-in [:share :hashtag])
        ]
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
       (if is-badge?
         [:a {:href "#" :on-click #(m/modal! [linkedin-modal url title certification] {:size :sm} )}
          [:i {:title "LinkedIn Share" :class "fa fa-linkedin-square"}]]
         #_[:a {:href   (str "https://www.linkedin.com/profile/add?_ed=0_JhwrBa9BO0xNXajaEZH4q5ax3e9v34rhyYLtaPv6h1UAvW5fJAD--ayg_G2AIDAQaSgvthvZk7wTBMS3S-m0L6A6mLjErM6PJiwMkk6nYZylU7__75hCVwJdOTZCAkdv&pfCertificationName=" title "&pfCertificationUrl=" url "&trk=onsite_html" )
              :target "_blank"}
          [:i {:title "LinkedIn Add to Profile" :class "inprofile fa fa-linkedin-square"}]]
         [:div.share-button
          [:a {:href (str "https://www.linkedin.com/shareArticle?mini=true&url=" url "&title=" title "&summary=" (js/encodeURIComponent (str site-name ": " title)) "&source=" hashtag) :target "_blank"}
           [:i {:title "LinkedIn Share" :class "fa fa-linkedin-square"}]]]
         )
       ]
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
         [:div.linkinput [:input {:class "form-control" :read-only true :type "text" :value (str "<iframe width=\"90%\" height=\"560\" src=\""url"/embed\" frameborder=\"0\"></iframe>")}]]))]
    ))



(defn share-buttons [url title public? is-badge? link-or-embed-atom image-file]
   (if (private?)
     [:div ]
     (create-class {:reagent-render      (fn [url title public? is-badge?]
                                           (share-buttons-element url title public? is-badge? link-or-embed-atom image-file nil))
                    :component-did-mount (fn []
                                           (do
                                             (.getScript (js* "$") "//assets.pinterest.com/js/pinit.js")
                                             (.getScript (js* "$") "//platform.twitter.com/widgets.js")
                                             (js* "delete IN")
                                        ;(.getScript (js* "$") "//platform.linkedin.com/in.js")
                                             (.getScript (js* "$") "https://apis.google.com/js/platform.js")))})))


(defn share-buttons-badge [url title public? is-badge? link-or-embed-atom image-file certification]
   (if (private?)
     [:div ]
     (create-class {:reagent-render      (fn [url title public? is-badge?]
                                           [share-buttons-element url title public? is-badge? link-or-embed-atom image-file certification])
                    :component-did-mount (fn []
                                           (do
                                             (.getScript (js* "$") "//assets.pinterest.com/js/pinit.js")
                                             (.getScript (js* "$") "//platform.twitter.com/widgets.js")
                                             (js* "delete IN")
                                        ;(.getScript (js* "$") "//platform.linkedin.com/in.js")
                                             (.getScript (js* "$") "https://apis.google.com/js/platform.js")))})))


