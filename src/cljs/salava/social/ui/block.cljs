(ns salava.social.ui.block
  (:require [salava.social.ui.badge-message-modal :refer [badge-message-link]]))


(defn ^:export message_link [message-count badge-id]
  [badge-message-link message-count badge-id])
