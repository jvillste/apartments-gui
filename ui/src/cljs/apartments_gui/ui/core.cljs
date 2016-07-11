(ns apartments-gui.ui.core
  (:require [reagent.core :as reagent :refer [atom]]
            [datascript.core :as d]
            [cljs.test :refer-macros [deftest is testing run-tests]]
            [cljs.core.async :as async]
            [cljs.pprint :as pprint]
            [cljs-http.client :as http]
            [apartments-gui.ui.routing :as routing]
            [apartments-gui.ui.ui :as ui]
            [secretary.core :as secretary])
  (:require-macros [cljs.core.async.macros :as async]))

(enable-console-print!)


(defn refresh [transaction-channel]
  (ui/call-and-apply-to-state transaction-channel
                              [:refresh-ids]
                              (fn [result state]
                                (assoc state :state result))))

(defn load [transaction-channel]
  (ui/call-and-apply-to-state transaction-channel
                              [:get-state]
                              (fn [result state]
                                (assoc state :state result))))

(defn set-comment [id comment]
  (ui/call [:set-comment id comment]
           (fn [result])))

(defn comment-editor [transaction-channel state id]
  [:input {:type "text"
           :style {:width "300px"}
           :value (get-in state [:comments id])
           :on-change (fn [e]
                        (let [value (.-value (.-target e))]
                          (set-comment id value)
                          (ui/apply-to-state transaction-channel
                                             (fn [state]
                                               (assoc-in state [:state :comments id] value)))))}])

(defn possible-editor [transaction-channel state id]
  [:input {:type "checkbox"
           :checked (get-in state [:possible id])
           :on-change (fn [e]
                        (let [value (.-checked (.-target e))]
                          (ui/call [:set-possible id value]
                                   (fn [result]))
                          (ui/apply-to-state transaction-channel
                                             (fn [state]
                                               (assoc-in state [:state :possible id] value)))))}])

(defn lot-table [transaction-channel state]
  (when (not (empty? state))
    (let [data-columns ["Sijainti:"
                        "Rakennusoikeus:"
                        "Tontin pinta-ala:"]]
      [:table {:class "table"}
       [:tbody
        [:tr 
         [:th "id"]
         [:th "hinta"]
         (for [column data-columns]
           [:th {:key column}
            (str column)])]
        (for [id  (:ids state)]
          [:tr {:key id
                :style {:background-color (if (get-in state [:possible id])
                                            "lightgreen"
                                            "white")}}
           [:td [:a {:href (str "http://www.etuovi.com/kohde/" id)}
                 (str id)]]
           [:td (str (or (get-in state [:data id "Myyntihinta:"])
                         (get-in state [:data id "Velaton lähtöhinta:"])
                         (get-in state [:data id "Velaton hinta :"])
                         (get-in state [:data id "Velaton hinta:"])))]
           (for [column data-columns]
             [:td {:key column
                   :style {:white-space "nowrap"}}
              (str (get-in state [:data id column] ))])
           [:td (comment-editor transaction-channel state id)]
           [:td (possible-editor transaction-channel state id)]])]])))

(defn page []
  
  (async/go-loop []
    (when-let [command (async/<! (:transaction-channel @ui/state-atom))]
      (swap! ui/state-atom command)
      (recur)))

  (load (:transaction-channel @ui/state-atom))
  
  (fn []

    (let [state @ui/state-atom
          transaction-channel (:transaction-channel state)]
      [:div

       [:input {:type "button"
                :value "Refresh" 
                :on-click (fn []
                            (refresh transaction-channel))}]

       #_[:pre (pr-str (:state state))]

       (lot-table transaction-channel
                  (:state state))])))


;; routing


(secretary/defroute "/" []
  (println "front page"))


;; startup

(defn ^:export main []
  (routing/setup-routes)
  (reagent/render-component [page] (.-body js/document)))



(run-tests (cljs.test/empty-env :cljs.test/pprint))



