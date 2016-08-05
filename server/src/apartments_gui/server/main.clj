(ns apartments-gui.server.main
  (:require [ring.adapter.jetty :as jetty]
            [apartments-gui.server.handler :as handler]
            [apartments-gui.server.api :as api])
  (:gen-class))

(def default-port 4011)

(defn -main [& [port]]
  (println "starting in port " default-port)
  (api/load-state)
  (jetty/run-jetty handler/app {:port default-port}))

;; development

(def server (atom nil))

(defn start []
  (api/load-state)
  (println "starting in port " default-port)
  (when @server (.stop @server))
  (reset! server
          (jetty/run-jetty handler/app {:port default-port :join? false})))
