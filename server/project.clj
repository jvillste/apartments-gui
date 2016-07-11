(defproject apartments-gui.server "0.1.0-SNAPSHOT"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ring/ring-jetty-adapter "1.3.2"]
                 [hiccup "1.0.5"]
                 [compojure "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [com.taoensso/timbre "4.0.2"]
                 [ring-cors "0.1.7"]
                 [datascript "0.15.0"]
                 [environ "1.0.0"]
                 [apartments "0.1.0-SNAPSHOT"]
                 [org.clojure/core.async "0.2.385"]]
  :plugins [[lein-ring "0.9.7"]]
  :aot [apartments-gui.server.main]
  :main apartments-gui.server.main
  :uberjar-name "apartments-gui.server.jar"
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}})
