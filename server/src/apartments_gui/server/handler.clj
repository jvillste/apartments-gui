(ns apartments-gui.server.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.cors :as cors]
            [apartments-gui.server.api :as api]))

(defn handle-post [body]
  (try (let [[command & arguments] body]
         (println "handling " (pr-str body))
         (let [result (if-let [function @(get (ns-publics 'apartments-gui.server.api) (symbol (name command)))]
                        (apply function arguments)
                        (str "unknown command: " command))]

           (let [result-message (pr-str result)]
             (println "result " (subs result-message 0 (min (.length result-message)
                                                            300))))
           
           result))
       (catch Exception e
         (println "Exception in handle- post" e)
         (.printStackTrace e *out*)
         (throw e))))

(defroutes app-routes
  (POST "/api" {body :body} (-> body
                                slurp
                                read-string
                                handle-post
                                pr-str))
  
  (route/not-found "Not Found"))

(def app (cors/wrap-cors app-routes
                         :access-control-allow-origin #".*"
                         :access-control-allow-methods [:head :options :get :put :post :delete :patch]))

;; curl -X POST "http://localhost:3001" -d "[:hello :me]"

