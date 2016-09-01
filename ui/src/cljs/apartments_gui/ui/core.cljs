(ns apartments-gui.ui.core
  (:require [reagent.core :as reagent :refer [atom]]
            [datascript.core :as d]
            [cljs.test :refer-macros [deftest is testing run-tests]]
            [cljs.core.async :as async]
            [cljs.pprint :as pprint]
            [secretary.core :as secretary]
            [cor.api :as cor-api]
            [cor.state :as state]
            [cor.routing :as routing]
            [cor.string :as string]
            [clojure.string :as clojure-string])
  (:require-macros [cljs.core.async.macros :as async]))

(enable-console-print!)



(defn etuovi-addresses [state]
  (reduce (fn [addresses lot]
            (conj addresses 
                  (->> (clojure-string/split (get lot "Sijainti:") #" ")
                       (drop 2)
                       (interpose " ")
                       (apply str)
                       (clojure-string/lower-case))))
          #{}
          (-> state :data vals)))

(defn unique-oikotie-ids [state]
  (let [etuovi-addresses (etuovi-addresses state)]
    (filter (fn [id]
              (not (contains? etuovi-addresses
                              (clojure-string/lower-case (get-in state [:oikotie-data id "Sijainti"])))))
            (:oikotie-ids state))))

(defn refresh [transaction-channel]
  (state/apply-to-state transaction-channel assoc
                        :querying true
                        :error nil)
  (cor-api/call-and-apply-to-state transaction-channel
                                   [:refresh-ids]
                                   (fn [result state]
                                     (println "result" (first result))
                                     (if (= :exception (first result))
                                       (assoc state
                                              :error (second result)
                                              :querying false)
                                       (assoc state
                                              :state result
                                              :unique-oikotie-ids (unique-oikotie-ids result)
                                              :error nil
                                              :querying false)))))

(defn load [transaction-channel]
  (cor-api/call-and-apply-to-state transaction-channel
                                   [:get-state]
                                   (fn [result state]
                                     (assoc state :state result
                                            :unique-oikotie-ids (unique-oikotie-ids result)))))

(defn assoc-in-server-state [path value]
  (cor-api/call [:assoc-in-state path value]
                (fn [result])))

(defn text-editor-view [attributes transaction-channel path on-change value]
  [:textarea (conj attributes
                   {:value value
                    :on-change (fn [e]
                                 (let [value (.-value (.-target e))]
                                   (on-change value)
                                   (state/apply-to-state transaction-channel
                                                         (fn [state]
                                                           (assoc-in state path value)))))})])

(defn text-editor [attributes transaction-channel path on-change state]
  [text-editor-view
   attributes
   transaction-channel
   path
   on-change
   (get-in state path)])

(defn comment-editor [transaction-channel source state id]
  [text-editor-view
   {:rows 1
    :cols 80}
   transaction-channel
   [:state source :comments id]
   (fn [new-value]
     (assoc-in-server-state [source :comments id]
                            new-value))
   (get-in state [:state source :comments id])])

(defn new-rating [current-rating rating checked]
  (if checked
    rating
    (if (> current-rating
           rating)
      rating
      (dec rating))))

(defn rating-checkbox [transaction-channel source current-rating id rating]
  [:input {:type "checkbox"
           :checked (>= current-rating
                        rating)
           :on-change (fn [e]
                        (let [value (.-checked (.-target e))
                              new-rating (new-rating current-rating
                                                     rating
                                                     value)]
                          (assoc-in-server-state [source :ratings id] new-rating)
                          
                          (state/apply-to-state transaction-channel
                                                (fn [state]
                                                  (assoc-in state [:state source :ratings id] new-rating)))))}])

(defn rating-editor-view [transaction-channel source current-rating id]
  [:div (for [rating (range 1 6)]
          [rating-checkbox transaction-channel source current-rating id rating])])

(defn rating-editor [transaction-channel source state id]
  [rating-editor-view
   transaction-channel
   :etuovi
   (or (get-in state [:state source :ratings id])
       0)
   id])

(defn background-color [rating]
  (get ["white"
        "#d0ffd0"
        "#a0ffa0"
        "#90ff90"
        "#70ff70"
        "#00ff00"]
       rating))

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
            (str column)])
         [:th "kommentit"]
         [:th "arvosana"]
         [:th "käyty"]]
        (for [id ids]
          [:tr {:key id
                :style {:background-color (background-color (or (get-in server-state [:etuovi :ratings id])
                                                                0))}}
           [:td [:a {:href (str "http://www.etuovi.com/kohde/" id)}
                 (str id)]]
           [:td (str (or (get-in server-state [:data id "Myyntihinta:"])
                         (get-in server-state [:data id "Velaton lähtöhinta:"])
                         (get-in server-state [:data id "Velaton hinta :"])
                         (get-in server-state [:data id "Velaton hinta:"])))]
           (for [column data-columns]
             [:td {:key column
                   :style {:white-space "nowrap"}}
              (string/substring (str (get-in server-state [:data id column]))
                                0 60)])
           [:td (comment-editor transaction-channel
                                :etuovi
                                state
                                id)]
           [:td (rating-editor transaction-channel
                               :etuovi
                               state
                               id)]])]])))


(defn oikotie-lot-table [transaction-channel ids state]
  (when (not (empty? (:state state)))
    (let [server-state (:state state)
          data-columns ["Sijainti"
                        "Rakennusoikeus"
                        "Tontin pinta-ala"]]
      [:table {:class "table"}
       [:tbody
        [:tr 
         [:th "id"]
         [:th "hinta"]
         (for [column data-columns]
           [:th {:key column}
            (str column)])]
        (for [id ids]
          (if (contains? (:etuovi-addresses state)
                         (clojure-string/lower-case (get-in server-state [:oikotie-data id "Sijainti"])))
            nil
            [:tr {:key id
                  :style {:background-color (background-color (or (get-in server-state [:etuovi :ratings id])
                                                                  0))}}
             [:td [:a {:href (str "http://asunnot.oikotie.fi/myytavat-tontit/" id)}
                   (str id)]]
             [:td (str (or (get-in server-state [:oikotie-data id "Myyntihinta"])
                           (get-in server-state [:oikotie-data id "Velaton lähtöhinta:"])
                           (get-in server-state [:oikotie-data id "Velaton hinta :"])
                           (get-in server-state [:oikotie-data id "Velaton hinta:"])))]
             (for [column data-columns]
               [:td {:key column
                     :style {:white-space "nowrap"}}
                (string/substring (str (get-in server-state [:oikotie-data id column]))
                                  0 60)])
             [:td (comment-editor transaction-channel :oikotie state id)]
             [:td (rating-editor transaction-channel :oikotie state id)]]))]])))

(defonce state-atom (state/create-state-atom))

(defn page []

  (when (not (:state @state-atom))
    (load (:transaction-channel @state-atom)))
  
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

       (when-let [error (:error state)]
         [:span {:style {:color "red"}} error])


       #_[:pre (pr-str (-> state :unique-oikotie-ids))]

       [:h1 "Oikotie"]
       (text-editor {:rows 1
                     :cols 200}
                    transaction-channel
                    [:state :oikotie-query-url]
                    (fn [new-value]
                      (assoc-in-server-state [:oikotie-query-url] new-value))
                    state)
       
       (oikotie-lot-table transaction-channel
                          (-> state :unique-oikotie-ids)
                          state)

       [:h1 "Etuovi"]
       (text-editor {:rows 1
                     :cols 200}
                    transaction-channel
                    [:state :etuovi-query-url]
                    (fn [new-value]
                      (assoc-in-server-state [:etuovi-query-url] new-value))
                    state)
       
       (lot-table transaction-channel
                  (-> state :state :ids)
                  state)

       [:h1 "Poistuneet Oikotiestä"]
       
       (oikotie-lot-table transaction-channel
                          (clojure.set/difference (apply hash-set
                                                         (keys (-> state :state :oikotie :comments)))
                                                  (apply hash-set (-> state :state :oikotie-ids)))
                          state)
       
       [:h1 "Poistuneet Etuovesta"]
       
       (lot-table transaction-channel
                  (clojure.set/difference (apply hash-set
                                                 (keys (-> state :state :etuovi :comments)))
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



