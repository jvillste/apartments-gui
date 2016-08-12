(ns apartments-gui.ui.core
  (:require [reagent.core :as reagent :refer [atom]]
            [datascript.core :as d]
            [cljs.test :refer-macros [deftest is testing run-tests]]
            [cljs.core.async :as async]
            [cljs.pprint :as pprint]
            [secretary.core :as secretary]
            [cor.api :as cor-api]
            [cor.routing :as routing])
  (:require-macros [cljs.core.async.macros :as async]))

(enable-console-print!)

(defn refresh [transaction-channel]
  (cor-api/apply-to-state transaction-channel
                          assoc :querying true)
  (cor-api/call-and-apply-to-state transaction-channel
                                   [:refresh-ids]
                                   (fn [result state]
                                     (assoc state
                                            :state result
                                            :querying false))))

(defn load [transaction-channel]
  (cor-api/call-and-apply-to-state transaction-channel
                                   [:get-state]
                                   (fn [result state]
                                     (assoc state :state result))))

(defn assoc-in-server-state [path value]
  (cor-api/call [:assoc-in-state path value]
                (fn [result])))

(defn text-editor [attributes transaction-channel path on-change state]
  [:textarea (conj attributes
                   {:value (get-in state path)
                    :on-change (fn [e]
                                 (let [value (.-value (.-target e))]
                                   (on-change value)
                                   (cor-api/apply-to-state transaction-channel
                                                           (fn [state]
                                                             (assoc-in state path value)))))})])

(defn comment-editor [transaction-channel state id]
  (text-editor {:rows 1
                :cols 80}
               transaction-channel
               [:state :comments id]
               (fn [new-value]
                 (assoc-in-server-state [:comments id]
                                        new-value))
               state))

(defn possible-editor [transaction-channel state id]
  [:input {:type "checkbox"
           :checked (get-in state [:possible id])
           :on-change (fn [e]
                        (let [value (.-checked (.-target e))]
                          (assoc-in-server-state [:possible id] value)
                          (cor-api/apply-to-state transaction-channel
                                                  (fn [state]
                                                    (assoc-in state [:state :possible id] value)))))}])

(defn lot-table [transaction-channel ids state]
  (when (not (empty? (:state state)))
    (let [server-state (:state state)
          data-columns ["Sijainti:"
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
        (for [id ids]
          [:tr {:key id
                :style {:background-color (if (get-in server-state [:possible id])
                                            "lightgreen"
                                            "white")}}
           [:td [:a {:href (str "http://www.etuovi.com/kohde/" id)}
                 (str id)]]
           [:td (str (or (get-in server-state [:data id "Myyntihinta:"])
                         (get-in server-state [:data id "Velaton lähtöhinta:"])
                         (get-in server-state [:data id "Velaton hinta :"])
                         (get-in server-state [:data id "Velaton hinta:"])))]
           (for [column data-columns]
             [:td {:key column
                   :style {:white-space "nowrap"}}
              (str (get-in server-state [:data id column] ))])
           [:td (comment-editor transaction-channel state id)]
           [:td (possible-editor transaction-channel server-state id)]])]])))

(defonce state-atom (cor-api/create-state-atom))

(defn page []

  (load (:transaction-channel @state-atom))
  
  (fn []

    (let [state @state-atom
          transaction-channel (:transaction-channel state)]
      [:div

       [:input {:type "button"
                :value "Hae tontit" 
                :on-click (fn []
                            (refresh transaction-channel))}]
       (when (:querying state)
         [:span "Haetaan..."])

       (text-editor {:rows 1
                     :cols 200}
                    transaction-channel
                    [:state :etuovi-query-url]
                    (fn [new-value]
                      (assoc-in-server-state [:etuovi-query-url] new-value)
                      #_(set-comment id new-value))
                    state)

       #_[:pre (pr-str (:state state))]

       (lot-table transaction-channel
                  (-> state :state :ids)
                  state)
       
       [:h1 "Poistuneet"]
       
       (lot-table transaction-channel
                  (clojure.set/difference (apply hash-set
                                                 (keys (-> state :state :comments)))
                                          (apply hash-set (-> state :state :ids)))
                  state)])))


;; routing


(secretary/defroute "/" []
  (println "front page"))


;; startup

(defn ^:export main []
  (routing/setup-routes)
  (reagent/render-component [page] (.-body js/document)))



(run-tests (cljs.test/empty-env :cljs.test/pprint))



