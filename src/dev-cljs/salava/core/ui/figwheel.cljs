(ns salava.core.ui.figwheel
  (:require [figwheel.client :as fw]
            [salava.core.ui.main :as m]))

(fw/watch-and-reload
  :websocket-url "ws://elvis.discendum.com:3450/figwheel-ws";"ws://localhost:3450/figwheel-ws"
  :jsload-callback m/init!)
