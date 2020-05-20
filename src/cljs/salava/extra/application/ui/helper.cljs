(ns salava.extra.application.ui.helper
  (:require
   [reagent.core :refer [atom cursor create-class dom-node]]
   [reagent.session :as session]
   [salava.core.i18n :refer [t]]
   [clojure.string :refer [join blank? upper-case capitalize lower-case split]]
   [dommy.core :as dommy :refer-macros [sel sel1]]
   [salava.core.ui.ajax-utils :as ajax]
   [salava.core.ui.helper :refer [path-for base-url private?]]
   [clojure.string :refer [trim split replace]]
   [salava.core.ui.share :as s]))

(defn subs-hashtag [text]
  (if (re-find #"^#" text) (subs text 1) text))

(defn- process-tags [tags]
  (as-> (split tags #",") $
        (map #(subs-hashtag %) $)
        (join "," $)))

(defn query-params [base]
  {:country (get base :country "")
   :tags (process-tags (get base :tags ""))
   :name (get base :name "")
   :issuer (get base :issuer "")
   :order (get base :order "mtime")
   :id (get base :id nil)
   :followed (get base :followed false)})

(defn application-plugin? []
  (some #(= "extra/application" %) (session/get :plugins)))

(defn url-builder [params state]
  (let [params (remove (fn [[k v]] (blank? v)) params)
        query (join (cons (str (name (key (first params))) "=" (val (first params)))
                          (map #(str (if (not (coll? (val %)))
                                       (str "&" (name (key %)) "=" (val %))
                                       (if (empty? (val %)) "" (if (empty? (rest (val %)))
                                                                 (str "&" (name (key %)) "[0]=" (first (val %)))
                                                                 (join (cons (str "&" (name (key %)) "[0]=" (first (val %)))
                                                                             (map (fn [e] (str "&" (name (key %)) "[" (.indexOf (val %) e) "]=" e)) (rest (val %)))))))))
                               (rest params))))]
    #_(.replaceState js/history "" "Badge Gallery" (str "?" query))
    (swap! state assoc :query-param (str (.-href js/window.location) "?" query))))

(defn str-cat [a-seq]
  (if (empty? a-seq)
    ""
    (let [a-seq (if (string? a-seq) (->> (split a-seq #",") (map #(if (re-find #"^#" %) % (str "#" %)))) a-seq)
          str-space (fn [str1 str2]
                      (str str1 " " str2))]
      (reduce str-space a-seq))))

(defn fetch-badge-adverts [state]
  (let [{:keys [user-id country-selected name issuer order tags show-followed-only]} @state
        params (->  (query-params (:params @state)) (assoc :followed show-followed-only))
        initial-params? (= (-> params (dissoc :followed)) (-> (:initial-query @state) (dissoc :followed)))]
    (swap! state assoc :show-link-or-embed-code nil)
    (if  (and initial-params? (false? show-followed-only) (blank? issuer))
      (swap! state assoc :show-featured true)
      (swap! state assoc :show-featured false))
    (ajax/GET
     (path-for (str "/obpv1/application/"))
     {:params  params
      :handler (fn [data]
                 (swap! state assoc :applications (:applications data))
                 (url-builder params state))})))

(defn taghandler
  "set tag with autocomplete value and accomplish searchs"
  [state value]
  (let [tags (cursor state [:params :tags])
        autocomplete-items (cursor state [:autocomplete :tags :items])]
    (reset! tags (apply str (interpose "," (vals (select-keys @autocomplete-items value)))))
    (fetch-badge-adverts state)))

(defn- match? [tag item]
  (when (and tag item)
    (let [item (-> (split item #"#") last)]
      (some #(= tag (str %)) [item (str "#" item) (lower-case item) (capitalize item) (upper-case item)]))))

(defn get-items-key [state tag]
  (let [autocomplete-items (cursor state [:autocomplete :tags :items])]
    (key (first (filter #(match? tag (val %)) @autocomplete-items)))))
    ;(key (first (filter #(= (str "#" tag) (or (val %))) @autocomplete-items)))))

(defn set-to-autocomplete [state tag]
  (let [key (get-items-key state tag)]
    (when key
      (swap! state assoc :value #{key})
      (reset! (cursor state [:autocomplete :tags :value]) #{key})
      (taghandler state #{key}))))

(defn add-to-followed
  "set advert to connections"
  [badge-advert-id data-atom state]
  (ajax/POST
   (path-for (str "/obpv1/application/create_connection_badge_application/" badge-advert-id))
   {:handler (fn [data]
               (when (= "success" (:status data))
                 (swap! data-atom assoc :followed 1) ;set current data-atom to followed true
                 (fetch-badge-adverts state)))}))

(defn remove-from-followed
  "remove advert from connections"
  ([badge-advert-id state]
   (remove-from-followed badge-advert-id nil state))
  ([badge-advert-id data-atom state]
   (ajax/DELETE
    (path-for (str "/obpv1/application/delete_connection_badge_application/" badge-advert-id))
    {:handler (fn [data]
                (when (= "success" (:status data))
                  ;set current data-atom to followed false
                  (when data-atom (swap! data-atom assoc :followed 0))
                  (fetch-badge-adverts state)))})))

;; Refactor code: move share functionality to core
(defn clipboard-button [target status]
  (let [clipboard-atom (atom nil)]
    (create-class
     {:display-name "clipboard-button"
      :component-did-mount
      #(let [clipboard (new js/Clipboard (dom-node %))]
         (reset! clipboard-atom clipboard))
      :component-will-unmount
      #(when-not (nil? @clipboard-atom)
         (.destroy @clipboard-atom)
         (reset! clipboard-atom nil))
      :reagent-render
      (fn []
        [:button {:class "btn btn-primary input-btn"
                  :id "copybutton"
                  :type "button"
                  :data-clipboard-target target
                  :on-click #(.preventDefault %)
                  :style {:height "34px"}}

         [:i {:class "fa fa-clipboard" :aria-hidden "true"}] (str " " (t :core/Copy))])})))

(defn input-button [name id text]
  (let [status (atom "")]
    (fn []
      [:div {:class "" :key id}
       [:fieldset
        [:legend ""]
        (when-not (clojure.string/blank? name) [:label {:class " sub-heading"} name])
        [:div.input-group
         [:input {:class       "form-control"
                  :id          id
                  :name        "email-text"
                  :type        "text"
                  :read-only true
                  :value       text
                  :aria-label  "copy text"}]
         [:span {:class "input-group-btn"}
          [clipboard-button (str "#" id) status]]]]])))

(defn share-buttons-applications [link-url embed-url link-or-embed-atom]
  (let [site-name (session/get-in [:share :site-name])
        hashtag   (session/get-in [:share :hashtag])]
    (if (private?)
      [:div]
      (create-class {:reagent-render  (fn [link-url embed-url link-or-embed-atom]

                                        [:div {:id "share"}
                                         [:div#share-buttons
                                          [:div.share-button
                                           [:a {:class  "twitter"
                                                :href   (str "https://twitter.com/intent/tweet?size=medium&count=none&text="
                                                             (js/encodeURIComponent (str site-name ": "))
                                                             "&url=" (js/encodeURIComponent link-url) "&hashtags=" hashtag)
                                                :target "_blank"
                                                :aria-label "Twitter"}
                                            [:i {:class "fa fa-twitter-square"}]]]
                                          [:div.share-button
                                           [:a {:href (str "https://www.linkedin.com/shareArticle?mini=true&url=" link-url "&summary=" (js/encodeURIComponent (str site-name ": ")) "&source=" hashtag) :target "_blank" :aria-label "LinkedIn"}
                                            [:i {:title "LinkedIn Share" :class "fa fa-linkedin-square"}]]]
                                          [:div.share-link
                                           [:a {:href "#" :on-click #(do (.preventDefault %) (reset! link-or-embed-atom (if (= "link" @link-or-embed-atom) nil "link")))} (str (t :badge/Share) " " (clojure.string/lower-case (t :core/Link)))]]
                                          [:div.share-link
                                           [:a {:href "#" :on-click #(do (.preventDefault %) (reset! link-or-embed-atom (if (= "embed" @link-or-embed-atom) nil "embed")))} (t :core/Embedcode)]]]
                                         (if (= "link" @link-or-embed-atom)
                                           [:div.copy-boxes
                                            [input-button nil "url" link-url]])
                                         (if (= "embed" @link-or-embed-atom)
                                           [:div.copy-boxes
                                            [input-button nil "embed" (str "<iframe width=\"90%\" height=\"560\" src=\"" embed-url "\" frameborder=\"0\"></iframe>")]])])
                     :component-did-mount (fn []
                                            (do
                                              (.getScript (js* "$") "//assets.pinterest.com/js/pinit.js")
                                              (.getScript (js* "$") "//platform.twitter.com/widgets.js")
                                              (js* "delete IN")
                                         ;(.getScript (js* "$") "//platform.linkedin.com/in.js")
                                              (.getScript (js* "$") "https://apis.google.com/js/platform.js")))}))))

(defn share [link-or-embed-atom url]
  (let [embed-url (replace (.-href js/window.location) #"/badge/application" (str "/badge/application/" (session/get-in [:user :id]) "/embed"))]
    [:div.form-group {:style {:margin-bottom "unset"}} [:div.col-sm-8 [share-buttons-applications url embed-url link-or-embed-atom]]]))
