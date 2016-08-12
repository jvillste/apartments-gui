(defproject apartments-gui.server "0.1.0-SNAPSHOT"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [cor "0.1.0-SNAPSHOT"]
                 [org.clojure/core.async "0.2.385"]
                 [clj-http "3.1.0"]
                 [hickory "0.5.4"]
                 [org.clojure/data.json "0.2.6"]]
  :aot [apartments-gui.server.main]
  :main apartments-gui.server.main
  :uberjar-name "apartments-gui.server.jar")
