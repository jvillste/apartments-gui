(ns apartments-gui.server.main
  (:require [apartments-gui.server.api :as api]
            [cor.server :as server]
            [cor.api :as cor-api])
  (:use clojure.test)
  (:gen-class))

(def default-port 4011)

(def state-file-name "apartments.edn")

(def migrations [(fn [state]
                   (-> (as-> state state
                         
                         (reduce (fn [state [id is-possible]]
                                   (if is-possible
                                     (assoc-in state [:etuovi :ratings id] 1)
                                     state))
                                 state
                                 (-> state :possible))
                         
                         (reduce (fn [state [id comment]]
                                   (assoc-in state [:etuovi :comments id] comment))
                                 state
                                 (-> state :comments)))
                       
                       (dissoc :possible
                               :comments)))])

(defn migrate [state]
  (-> (reduce (fn [state migration]
                (migration state))
              state
              (drop (or (:version state)
                        0)
                    migrations))
      (assoc :version (count migrations))))

(deftest migrate-test
  (is (= {:etuovi {:ratings {"1" 1}
                   :comments {"1" "foo"
                              "3" "bar"}},
          :version 1}
         (migrate {:possible {"1" true
                              "3" false}
                   :comments {"1" "foo"
                              "3" "bar"}}))))

(defn load-state []
  (-> {:persistent-state (-> (read-string (slurp state-file-name))
                             (migrate))} 
      (assoc :state-file-name state-file-name)))

(defn start-server []
  (println "starting in port " default-port)
  (server/start-server (cor-api/app-with-web-socket (load-state)
                                                    'apartments-gui.server.api)
                       default-port))

(defn -main [& [port]]
  (start-server))

;; development


(defn start []
  (start-server))
