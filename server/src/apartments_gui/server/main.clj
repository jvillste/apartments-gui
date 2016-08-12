(ns apartments-gui.server.main
  (:require [apartments-gui.server.api :as api]
            [cor.server :as server]
            [cor.api :as cor-api])
  (:gen-class))

(def default-port 4011)

(def state-file-name "apartments.edn")

(defn load-state []
  (-> (read-string (slurp state-file-name))
      (assoc :state-file-name state-file-name)))

(defn start-server []
  (println "starting in port " default-port)
  (server/start-server (cor-api/app (load-state)
                                    'apartments-gui.server.api)
                       default-port))

(defn -main [& [port]]
  (start-server))

;; development

(defn start []
  (start-server))
