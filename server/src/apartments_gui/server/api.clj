(ns apartments-gui.server.api
  (:require [apartments-gui.server.core :as core]
            [clojure.core.async :as async]
            [apartments-gui.server.csp :as csp]
            [cor.log :as log]
            [cor.web-socket :as web-socket]))

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
  (async/put! save-channel (:persistent-state state)))


(defn ^:cor/api get-state [state-atom]
  (:persistent-state @state-atom))

(defn refresh-etuovi-ids [state-atom]
  (let [ids (core/get-all-etuovi-lot-ids (-> @state-atom :persistent-state :etuovi-query-url ))]
    (log/info "got" (count ids) "ids")
    (swap! state-atom assoc-in [:persistent-state :ids] ids)
    (doseq [id ids]
      (when (not (get-in @state-atom [:persistent-state :data id]))
        (do (log/info "getting data for " id)
            (let [data (core/get-etuovi-lot-data (core/get-hickup (str "http://www.etuovi.com/kohde/" id)))]
              (swap! state-atom
                     assoc-in
                     [:persistent-state :data id]
                     data)))))
    (schedule-state-save @state-atom)))

;; "http://asunnot.oikotie.fi/api/cards?cardType=104&limit=1000&locations=%5B%5B65,6,%22Vantaa%22%5D%5D&offset=0&sortBy=published_desc"
(defn refresh-oikotie-ids [state-atom]
  (let [ids (core/get-oikotie-lot-ids (-> @state-atom :persistent-state :oikotie-query-url))]
    (log/info "got" (count ids) "oikotie ids")
    (swap! state-atom assoc-in [:persistent-state :oikotie-ids] ids)
    (doseq [id ids]
      (when (not (get-in @state-atom [:persistent-state :oikotie-data id]))
        (do (log/info "getting data for " id)
            (let [data (core/get-oikotie-data (str "http://asunnot.oikotie.fi/myytavat-tontit/" id))]
              (swap! state-atom
                     assoc-in
                     [:persistent-state :oikotie-data id]
                     data)))))))


(defn ^:cor/api refresh-ids [state-atom]
  (try (refresh-etuovi-ids state-atom)
       (refresh-oikotie-ids state-atom)
       (schedule-state-save @state-atom)
       (:persistent-state @state-atom)
       (catch Exception e
         [:exception (.getMessage e)])))

(defn ^:cor/api assoc-in-state [state-atom path value]
  (swap! state-atom assoc-in (concat [:persistent-state] path) value)
  (schedule-state-save @state-atom)
  (web-socket/broadcast-message (:channel-socket @state-atom)
                                :apartments/assoc-in-state
                                [path value])
  nil)


