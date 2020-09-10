(ns salava.admin.ui.modal
  (:require
   [salava.extra.spaces.ui.report :as report]))

(def ^:export modalroutes
  {:report {:badges report/badges-modal}})
