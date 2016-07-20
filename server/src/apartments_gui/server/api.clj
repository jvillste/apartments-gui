(ns apartments-gui.server.api
  (:require [apartments.core :as apartments]
            [apartments.lots :as lots]
            [clojure.core.async :as async]
            [apartments-gui.server.csp :as csp]))

(defonce state (atom {}))

(def state-file-name "apartments.edn")

(defn save-state []
  (spit state-file-name
        (pr-str @state)))

(defonce save-channel (let [channel (async/chan)
                            thorttled-channel (csp/throttle channel
                                                            5000)]
                        (async/go-loop []
                          (when-let [_ (async/<! thorttled-channel)]
                            (println "saving")
                            (save-state)
                            (recur)))
                        
                        channel))


(defn schedule-state-save []
  (async/put! save-channel 1))


(defn get-state []
  @state)

(defn load-state []
  (reset! state (read-string (slurp state-file-name))))

(defn refresh-ids []
  (let [ids (apartments/get-all-etuovi-lot-ids "tulokset?haku=M132770175")]
    (swap! state assoc :ids (apartments/get-all-etuovi-lot-ids "tulokset?haku=M132770175"))
    (doseq [id ids]
      (if (not (get-in @state [:data id]))
        (do (println "getting data for " id)
            (swap! state
                   assoc-in
                   [:data id]
                   (apartments/get-etuovi-lot-data (apartments/get-hickup (str "http://www.etuovi.com/kohde/" id)))))))
    (schedule-state-save))
  @state)

(defn set-comment [id comment]
  (swap! state assoc-in [:comments id] comment)
  (schedule-state-save)
  nil)

(defn set-possible [id comment]
  (swap! state assoc-in [:possible id] comment)
  (schedule-state-save)
  nil)


#_ (apartments/get-etuovi-lot-data (apartments/get-hickup "http://www.etuovi.com/kohde/c34822"))
