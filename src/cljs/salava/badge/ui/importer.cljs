(ns salava.badge.ui.importer
  (:require [reagent.core :refer [atom]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [clojure.walk :refer [keywordize-keys]]
            [ajax.core :as ajax]
            [salava.core.ui.grid :refer [badge-grid-element]]
            [salava.core.ui.layout :as layout]
            [salava.core.helper :refer [dump]]
            [salava.core.i18n :refer [t]]))

(defn import-modal []
  [:div
   [:div.modal-header
    [:button {:type "button"
              :class "close"
              :data-dismiss "modal"
              :aria-label "OK"
              }
     [:span {:aria-hidden "true"
             :dangerouslySetInnerHTML {:__html "&times;"}}]]
    [:h4.modal-title (t :badge/Badgessaved)]]
   [:div.modal-body
    [:p (t :badge/Badgessavedsuccessfully)]]
   [:div.modal-footer
    [:button {:type "button"
              :class "btn btn-default btn-primary"
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
    (str (session/get :apihost) "/obpv1/badge/import/1")
    {:finally (fn []
                (ajax-stop state))
     :handler (fn [x]
                (let [data (keywordize-keys x)]
                  (swap! state assoc :error (:error data))
                  (swap! state assoc :badges (:badges data))
                  (swap! state assoc :ok-badges (ok-badge-keys (:badges data)))))}))

(defn import-badges [state]
  (swap! state assoc :ajax-message (t :badge/Savingbadges))
  (ajax/POST
    (str (session/get :apihost) "/obpv1/badge/import_selected/1")
    {:params  {:keys (:badges-selected @state)}
     :finally (fn []
                (ajax-stop state))
     :handler (fn []
                (m/modal! (import-modal)
                          {:hide #(.replace js/window.location "/")}))}))

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
        checked? (some #(= key %) (:badges-selected @state))
        input-id (str "input-" key)]
    [:div {:class "col-xs-6 col-sm-4 grid-container"
           :key key}
     [:div.media
      (if image_file
        [:div.media-left
         [:img {:src (if-not (re-find #"http" image_file)
                       (str "/" image_file)
                       image_file)}]])
      [:div.media-body
       name]]

      (if (= status "ok")
        [:div
         [:input {:type "checkbox"
                 :checked checked?
                 :id input-id
                 :on-change (fn []
                              (if checked?
                                (remove-badge-selection key state)
                                (add-badge-selection key state)))}]
         [:label {:for input-id}
         (t :badge/Savebadge)]]
        [:div message])]))

(defn badge-grid [state]
  [:div
   (for [badge (:badges @state)]
     (import-grid-element badge state))])

(defn content [state]
  [:div {:class "import-badges"}
   [:h2 (t :badge/Import)]
   [:div.import-button
    (if (:ajax-message @state)
      [:h3 (:ajax-message @state)])
    (if-not (pos? (count (:badges @state)))
      [:button {:class "btn btn-default btn-primary"
                :on-click #(fetch-badges state)
                :disabled (:ajax-message @state)}
       (t :badge/Importfrom)]

      (if (pos? (count (:ok-badges @state)))
        [:div
         [:button {:class    "btn btn-default btn-primary"
                   :on-click #(toggle-select-all state)}
          (if (:all-selected @state)
            (t :badge/Clearall)
            (t :badge/Selectall))]
         [:button {:class    "btn btn-default btn-primary"
                   :on-click #(import-badges state)
                   :disabled (or (:ajax-message @state)
                                 (= (count (:badges-selected @state)) 0))}
          (t :badge/Saveselected)]]))]
   (if-not (nil? (:error @state))
     [:div {:class "alert alert-warning"} (:error @state)])
   [badge-grid state]])

(defn handler [site-navi params]
  (let [state (atom {:badges []
                     :badges-selected []
                     :error nil
                     :ajax-message nil
                     :all-selected false
                     :ok-badges []})]
    (fn []
      (layout/default site-navi (content state)))))

