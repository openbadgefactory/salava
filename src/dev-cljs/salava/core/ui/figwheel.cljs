(ns salava.core.ui.figwheel
  (:require [figwheel.client :as fw]
            [salava.core.ui.main :as m]))

(fw/watch-and-reload
  :websocket-url "ws://latakia.discendum.com:3449/figwheel-ws"
  :jsload-callback m/init!)
