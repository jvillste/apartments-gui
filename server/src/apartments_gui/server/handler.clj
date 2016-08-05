(ns apartments-gui.server.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.cors :as cors]
            [apartments-gui.server.api :as api]
            [cor.api :as cor-api]))

(defroutes app-routes

  (routes (cor-api/api-route "/api"
                             'apartments-gui.server.api)

          (route/resources "/")

          (route/not-found "Not Found")))

(def app (cors/wrap-cors app-routes
                         :access-control-allow-origin #".*"
                         :access-control-allow-methods [:head :options :get :put :post :delete :patch]))

;; curl -X POST "http://localhost:3001" -d "[:hello :me]"

