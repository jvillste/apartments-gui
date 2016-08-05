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


(defn ^:cor/api get-state []
  @state)

(defn load-state []
  (reset! state (read-string (slurp state-file-name))))

(defn ^:cor/api refresh-ids []
  (let [ids (apartments/get-all-etuovi-lot-ids (:etuovi-query-url @state))]
    (swap! state assoc :ids ids)
    (doseq [id ids]
      (if (not (get-in @state [:data id]))
        (do (println "getting data for " id)
            (swap! state
                   assoc-in
                   [:data id]
                   (apartments/get-etuovi-lot-data (apartments/get-hickup (str "http://www.etuovi.com/kohde/" id)))))))
    (schedule-state-save))
  @state)

(defn ^:cor/api assoc-in-state [path value]
  (swap! state assoc-in path value)
  (schedule-state-save)
  nil)


