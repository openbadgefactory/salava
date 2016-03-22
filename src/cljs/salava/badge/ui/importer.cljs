(ns salava.badge.ui.importer
  (:require [reagent.core :refer [atom]]
            [reagent-modals.modals :as m]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.helper :refer [navigate-to]]
            [salava.core.helper :refer [dump]]
            [salava.core.i18n :refer [t]]))

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
       message]]
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
       (map #(:key %))))

(defn fetch-badges [state]
  (swap! state assoc :ajax-message (t :badge/Fetchingbadges))
  (ajax/GET
    "/obpv1/badge/import"
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
    "/obpv1/badge/import_selected"
    {:params  {:keys (:badges-selected @state)}
     :finally (fn []
                (ajax-stop state))
     :handler (fn [data]
                (m/modal! (import-modal data)
                          {:hide #(navigate-to "/badge")}))}))

(defn remove-badge-selection [key state]
  (swap! state assoc :badges-selected
         (remove
           #(= % key)
           (:badges-selected @state))))

(defn add-badge-selection [key state]
  (swap! state assoc :badges-selected
         (conj (:badges-selected @state) key)))

(defn toggle-select-all [state]
  (swap! state update-in [:all-selected] not)
  (if (:all-selected @state)
    (swap! state assoc :badges-selected (:ok-badges @state))
    (swap! state assoc :badges-selected [])))

(defn import-grid-element [element-data state]
  (let [{:keys [image_file name key description status message]} element-data
        checked? (some #(= key %) (:badges-selected @state))]
    [:div {:class "col-xs-12 col-sm-6 col-md-4"
           :key key}
     [:div {:class "media grid-container"}
      [:div.media-content
       (if image_file
         [:div.media-left
          [:img {:src (if-not (re-find #"http" image_file)
                        (str "/" image_file)
                        image_file)}]])
       [:div.media-body
        [:div.media-heading
         name]
        [:div.badge-description description]]]
      [:div {:class "media-bottom"}
       (if (= status "ok")
         [:div.checkbox
          [:label
           [:input {:type "checkbox"
                    :checked checked?
                    :on-change (fn []
                                 (if checked?
                                   (remove-badge-selection key state)
                                   (add-badge-selection key state)))}]
           (t :badge/Savebadge)]]
         [:div message])]]]))

(defn badge-grid [state]
  (into [:div {:class "row"
               :id "grid"}]
        (for [badge (:badges @state)]
          (import-grid-element badge state))))

(defn import-info []
  [:div
   [:p
    "You can import your existing badges from your "
    [:a {:href "https://backpack.openbadges.org/backpack/" :target "_blank"} "Mozilla Backpack"]
    " account. Before you start make sure that the e-mail address associated with your Backpack account is saved at the "
    [:a {:href "/user/edit/email-addresses"} "E-mail addresses"] " page."
    [:br]
    "To import badges from Backpack, badges have to be placed to a public Collection (group). If your badges are not in a public Collection, please follow these instructions:"]
   [:ol
    [:li "Please login to your Mozilla Backpack."]
    [:li "Go to Collections page and drag the badges you want to import under any of the collections."]
    [:li "Check \"public\" checkbox for the collection."]
    [:li "Click \"Import badges from Mozilla Backpack\" button below."]]
   [:p
    "Your imported badges will appear at "
    [:a {:href "/badge/mybadges"} "My badges"] " page. "
    "You can delete unwanted badges at "
    [:a {:href "/badge/mybadges"} "My badges"]
    " page in badge Settings."]])

(defn content [state]
  [:div {:class "import-badges"}
   [m/modal-window]
   [:h1.uppercase-header (t :badge/Importfrom)]
   [import-info]
   [:div.import-button
    (if (:ajax-message @state)
      [:div.ajax-message
       [:i {:class "fa fa-cog fa-spin fa-2x "}]
       [:span (:ajax-message @state)]])
    (if-not (pos? (count (:badges @state)))
      [:button {:class "btn btn-primary"
                :on-click #(fetch-badges state)
                :disabled (:ajax-message @state)}
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
     [:div {:class "alert alert-warning"} (:error @state)])
   [badge-grid state]])

(defn init-data []
  (ajax/GET "/obpv1/user/test" {}))

(defn handler [site-navi params]
  (let [state (atom {:badges []
                     :badges-selected []
                     :error nil
                     :ajax-message nil
                     :all-selected false
                     :ok-badges []})]
    (init-data)
    (fn []
      (layout/default site-navi (content state)))))

