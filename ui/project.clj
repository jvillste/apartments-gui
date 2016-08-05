(defproject apartments-gui.ui "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [datascript "0.15.0"]
                 [reagent "0.5.1"]
                 [garden "1.3.0"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                 [com.rpl/specter "0.9.2"]
                 [org.clojure/core.async "0.2.374"]
                 [cljs-http "0.1.40"]
                 [secretary "1.2.3"]
                 [cor "0.1.0-SNAPSHOT"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj"]

  :plugins [[lein-cljsbuild "1.1.1"]
            [lein-figwheel "0.5.0-2"]
            [lein-garden "0.2.6"]
            [lein-doo "0.1.6"]]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target" 
                                    "test/js" 
                                    "resources/public/css/compiled"]

  :garden {:builds [{:id "screen"
                     :source-paths ["src/clj"]
                     :stylesheet apartments-gui.ui.css/screen
                     :compiler {:output-to "../server/resources/public/css/compiled/screen.css"
                                :pretty-print? true}}]}

  :figwheel {:css-dirs ["resources/public/css"]
             :server-port 4449}

  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.1"]
                                  [figwheel-sidecar "0.5.0-1"]]
                   :source-paths ["src/cljs"] }}
  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  
  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/cljs" "dev"]
                        :figwheel {:on-jsload "apartments-gui.ui.core/main"}
                        :compiler {:main apartments-gui.ui.core
                                   :output-to "../server/resources/public/js/compiled/app.js"
                                   :output-dir "../server/resources/public/js/compiled/out"
                                   :asset-path "js/compiled/out"
                                   :source-map-timestamp true}}

                       {:id "test"
                        :source-paths ["src/cljs" "test/cljs"]
                        :compiler {:output-to "resources/public/js/compiled/test.js"
                                   :main apartments-gui.ui.runner
                                   :optimizations :none}}

                       {:id "min"
                        :source-paths ["src/cljs" "prod"]
                        :compiler {:main apartments-gui.ui.core
                                   :output-to "../server/resources/public/js/compiled/app.js"
                                   :optimizations :advanced
                                   :pretty-print false}}]})
