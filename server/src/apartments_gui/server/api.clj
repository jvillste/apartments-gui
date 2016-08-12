(ns apartments-gui.server.api
  (:require [apartments-gui.server.core :as core]
            [clojure.core.async :as async]
            [apartments-gui.server.csp :as csp]
            [cor.log :as log]))

(defn save-state [state]
  (spit (:state-file-name state)
        (pr-str state)))

(defonce save-channel (let [channel (async/chan)
                            thorttled-channel (csp/throttle channel
                                                            5000)]
                        (async/go-loop []
                          (when-let [state (async/<! thorttled-channel)]
                            (println "saving")
                            (save-state state)
                            (recur)))
                        
                        channel))


(defn schedule-state-save [state]
  (async/put! save-channel state))


(defn ^:cor/api get-state [state-atom]
  @state-atom)



(defn ^:cor/api refresh-ids [state-atom]
  (log/info "refreshing ids")
  (let [ids (core/get-all-etuovi-lot-ids (:etuovi-query-url @state-atom))]
    (log/info "got" (count ids) "ids")
    (swap! state-atom assoc :ids ids)
    (doseq [id ids]
      (when (not (get-in @state-atom [:data id]))
        (do (log/info "getting data for " id)
            (let [data (core/get-etuovi-lot-data (core/get-hickup (str "http://www.etuovi.com/kohde/" id)))]
              (swap! state-atom
                     assoc-in
                     [:data id]
                     data)))))
    (schedule-state-save @state-atom))
  @state-atom)

(defn ^:cor/api assoc-in-state [state-atom path value]
  (swap! state-atom assoc-in path value)
  (schedule-state-save)
  nil)


