(ns salava.badge.ui.assertion
  (:require [salava.core.i18n :refer [t]]))

(defn assertion-modal [assertion]
  [:div {:id "badge-settings"}
   [:div.modal-body
    [:div.row
     [:div.col-md-12
      [:button {:type         "button"
                :class        "close"
                :data-dismiss "modal"
                :aria-label   "OK"
                }
       [:span {:aria-hidden             "true"
               :dangerouslySetInnerHTML {:__html "&times;"}}]]]]
    [:table {:id "assertion-info"}
     [:thead
      [:tr
       [:th (t :badge/Property)]
       [:th (t :badge/Value)]]]
     (into
       [:tbody]
       (for [prop-1 (keys assertion)
             :let [val-1 (get assertion prop-1)]]
         (if (map? val-1)
           (for [prop-2 (keys val-1)
                 :let [val-2 (get-in assertion [prop-1 prop-2])]]
             (if (map? val-2)
               (for [prop-3 (keys val-2)
                     :let [val-3 (get-in assertion [prop-1 prop-2 prop-3])]]
                 (if-not (map? val-3)
                   [:tr {:class (str "tr-" prop-1 "-" prop-2 "-" prop-3)}
                    [:td (name prop-1) " " (name prop-2) " " (name prop-3)]
                    [:td (or val-3 "-")]]))
               [:tr {:class (str "tr-" prop-1 "-" prop-2)}
                [:td (str (name prop-1) " " (name prop-2))]
                [:td  (or (str val-2) "-")]
                ]))
           [:tr {:class (str "tr-" prop-1)}
            [:td (name prop-1)]
            [:td (or (str val-1) "-")]
            ])))]]
   [:div.modal-footer
    [:button {:type         "button"
              :class        "btn btn-primary"
              :data-dismiss "modal"}
     (t :core/Close)]]])
