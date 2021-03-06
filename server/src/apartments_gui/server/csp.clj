(ns apartments-gui.server.csp
  (:require [clojure.core.async :as async]))

(defn tap-new
  ([mult]
   (tap-new mult (async/chan)))

  ([mult channel]
   (async/tap mult channel)
   channel))

(defn throttle [input-channel interval]
  (let [mult (async/mult input-channel)
        throttled-channel (async/chan)
       ;; unthrottled-channel (tap-new mult)
        unthrottled-channel-2 (tap-new mult)]

    (async/go-loop [value (async/<! unthrottled-channel-2)]
      (when value
        (async/alt! (async/timeout interval) (do (async/>! throttled-channel value)
                                                 (recur (async/<! unthrottled-channel-2)))
                    unthrottled-channel-2 ([value] (recur value)))))
    #_[throttled-channel unthrottled-channel]
    throttled-channel))
