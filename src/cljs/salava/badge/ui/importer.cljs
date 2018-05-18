(ns salava.badge.ui.importer
  (:require [reagent.core :refer [atom]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.helper :refer [js-navigate-to accepted-terms? navigate-to path-for not-activated?]]
            [salava.core.ui.notactivated :refer [not-activated-banner]]
            [salava.core.helper :refer [dump]]
            [salava.core.ui.error :as err]
            [salava.core.i18n :refer [t translate-text]]))


(defn import-modal [{:keys [status message saved-count error-count]}]
  [:div
   [:div.modal-header
    [:button {:type "button"
              :class "close"
              :data-dismiss "modal"
              :aria-label "OK"
              }
     [:span {:aria-hidden "true"
             :dangerouslySetInnerHTML {:__html "&times;"}}]]]
   [:div.modal-body
    [:div {:class (str "alert " (if (= status "success")
                                  "alert-success"
                                  "alert-warning"))}
     (translate-text message)]]
   [:div.modal-footer
    [:button {:type "button"
              :class "btn btn-primary"
              :data-dismiss "modal"}
     "OK"]]])

(defn ajax-stop [state]
  (swap! state assoc :ajax-message nil))

(defn ok-badge-keys [badges]
  (->> badges
       (filter #(= (:status %) "ok"))
       (map #(:import-key %))))

(defn fetch-badges [state]
  (swap! state assoc :ajax-message (t :badge/Fetchingbadges))
  (ajax/GET
    (path-for "/obpv1/badge/import" true)
    {:finally (fn []
                (ajax-stop state))
     :handler (fn [{:keys [error badges]} data]
                (swap! state assoc :error error)
                (swap! state assoc :badges badges)
                (swap! state assoc :ok-badges (ok-badge-keys badges))
                (if (and (empty? badges) (not error))
                  (m/modal! (import-modal {:status "warning" :message (t :badge/Nobackpackfound)}))))}))

(defn import-badges [state]
  (swap! state assoc :ajax-message (t :badge/Savingbadges))
  (ajax/POST
    (path-for "/obpv1/badge/import_selected")
    {:params  {:keys (:badges-selected @state)}
     :finally (fn []
                (ajax-stop state))
     :handler (fn [data]
                (m/modal! (import-modal data)
                          {:hide #(navigate-to "/badge")}))}))

(defn remove-badge-selection [import-key state]
  (swap! state assoc :badges-selected
         (remove
           #(= % import-key)
           (:badges-selected @state))))

(defn add-badge-selection [import-key state]
  (swap! state assoc :badges-selected
         (conj (:badges-selected @state) import-key)))

(defn toggle-select-all [state]
  (swap! state update-in [:all-selected] not)
  (if (:all-selected @state)
    (swap! state assoc :badges-selected (:ok-badges @state))
    (swap! state assoc :badges-selected [])))

(defn import-grid-element [element-data state]
  (let [{:keys [previous-id image_file name import-key description status message issuer_content_name issuer_content_url error]} element-data
        checked? (some #(= import-key %) (:badges-selected @state))
        invalidtype (cond
                    (= "badge/Alreadyowned" message) "duplicate"
                    (= "badge/Badgeisexpired" message) "expired"
                    (= "badge/Savethisbadge" message) "ok"
                    :else "error")

        badge-link (if (= invalidtype "duplicate") (path-for (str "/badge/info/" previous-id)))]
     [:div {:class (str "media grid-container " invalidtype)}
      [:div.media-content
       (if (and image_file (re-find #"^(https?://|data:image/).+" image_file))
         [:div.media-left
          [:img {:src image_file
                 :alt name}]])
       [:div.media-body
        [:div.media-heading
          (if badge-link
            [:a.heading-link {:href badge-link} name]
            name)]
         [:div.media-issuer
          [:a {:href issuer_content_url
              :target "_blank"
              :title issuer_content_name} issuer_content_name]]
        [:div.badge-description.import description]]]
      [:div {:class "media-bottom"}
       (if (= status "ok")
         [:div.checkbox
         [:label {:for (str "checkbox-" import-key)}
          [:input {:type "checkbox"
                    :id (str "checkbox-" import-key)
                    :name "checkbox"
                    :checked checked?
                    :on-change (fn []
                                 (if checked?

                                   (remove-badge-selection import-key state)
                                   (add-badge-selection import-key state)))}]
          (t :badge/Savebadge)]]
        (if (= invalidtype "error")
          [:div
           [:span {:id (str "err" import-key)} error]]
          [:div
           (t (keyword message))]))]]))

(defn badge-grid [state]
  (into [:div {:class "row"
               :id "grid"}]
        (for [badge (:badges @state)]
          (import-grid-element badge state))))

(defn import-info []
  [:div
   [:p
    (t :badge/Importexistbadges1) " "
    [:a {:href "https://backpack.openbadges.org/backpack/" :target "_blank"} "Mozilla Backpack"]
    " " (t :badge/Importexistbadges2) " "
    [:a {:href (path-for "/user/edit/email-addresses")} (t :badge/Mailaddresses)] " " (t :badge/page) "."
    [:br]
    (t :badge/Importbadgesinstructions1) ":"]
   [:ol
    [:li (t :badge/Importbadgesinstructions2) "."]
    [:li (t :badge/Importbadgesinstructions3) "."]
    [:li [:span {:dangerouslySetInnerHTML
         {:__html (t :badge/Importbadgesinstructions4)}}] "."]
    [:li [:span {:dangerouslySetInnerHTML
         {:__html (t :badge/Importbadgesinstructions5)}}] "."]]
   [:p
    (t :badge/Importbadgesresults1) " "
    [:a {:href (path-for "/badge/mybadges")} (t :badge/Mybadges)] " "
    (t :badge/Importbadgesresults2) " "
    [:a {:href (path-for "/badge/mybadges")} (t :badge/Mybadges)]
    " " (t :badge/Importbadgesresults3) "."]])

(defn content [state]
  [:div {:class "import-badges"}
   [m/modal-window]
   [:h1.uppercase-header (t :badge/Importfrom)]
   [import-info]
   (not-activated-banner)
   [:div.import-button
    (if (:ajax-message @state)
      [:div.ajax-message
       [:i {:class "fa fa-cog fa-spin fa-2x "}]
       [:span (translate-text (:ajax-message @state))]])
    (if-not (pos? (count (:badges @state)))
      [:button {:class "btn btn-primary"
                :on-click #(fetch-badges state)
                :disabled (or (:ajax-message @state)
                              (if (not-activated?) "disabled" ""))}
       (t :badge/Importfrom)]

      (if (pos? (count (:ok-badges @state)))
        [:div
         [:button {:class    "btn btn-primary"
                   :on-click #(toggle-select-all state)}
          (if (:all-selected @state)
            (t :badge/Clearall)
            (t :badge/Selectall))]
         [:button {:class    "btn btn-primary"
                   :on-click #(import-badges state)
                   :disabled (or (:ajax-message @state)
                                 (= (count (:badges-selected @state)) 0))}
          (t :badge/Saveselected)]]))]
   (if-not (nil? (:error @state))
     [:div {:class "alert alert-warning"} (t (keyword (:error @state)))])
   [badge-grid state]])


(defn init-data [state]
  (ajax/GET (path-for "/obpv1/user/public-access")
            {:handler (fn [data]
                        (swap! state assoc :permission "success"))}
            (fn [] (swap! state assoc :permission "error"))))

(defn handler [site-navi params]
  (let [state (atom {:permission "initial"
                     :badges []
                     :badges-selected []
                     :error nil
                     :ajax-message nil
                     :all-selected false
                     :ok-badges []})]
    (init-data state)
    (fn []
      (cond
        (= "false" (accepted-terms?)) (js-navigate-to (path-for (str "/user/terms/" (session/get-in [:user :id]))))
        (= "initial" (:permission @state)) (layout/default site-navi [:div ])
        (= "success" (:permission @state))(layout/default site-navi (content state))
        :else(layout/default site-navi (err/error-content))))))

