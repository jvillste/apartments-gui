(ns apartments-gui.server.core
  (:require #_[flow-gl.csp :as csp]
            #_(flow-gl.gui [drawable :as drawable]
                           [layout :as layout]
                           [layouts :as layouts]
                           [layout-dsl :as l]
                           [controls :as controls]
                           [gui :as gui]
                           [events :as events]
                           [layoutable :as layoutable]
                           [transformer :as transformer])
            #_(flow-gl.graphics [font :as font])
            [clj-http.client :as http]
            [hickory.core :as hickory]
            [clojure.data.json :as json]
            #_[flow-gl.tools.trace :as trace])
  (:import [java.awt Desktop]
           [java.net URI])
  (:use [clojure.test]))

(defn find-term
  ([term hickup]
   (find-term term hickup []))

  ([term hickup path]
   (loop [hickup hickup
          index 0]
     (when-let [element (first hickup)]
       (let [new-path (conj path index)]
         (cond (= element term)
               new-path

               (vector? element)
               (if-let [result (find-term term element new-path)]
                 result
                 (recur (rest hickup)
                        (inc index)))

               :default
               (recur (rest hickup)
                      (inc index))))))))




(defn navigate [path back-steps forward-steps]
  (concat (take (- (count path) back-steps)
                path)
          forward-steps))

(defn clean-value [value]
  (->> (flatten value)
       (filter string?)
       (map #(.trim %))
       (filter #(not= "" %))
       (interpose " ")
       (apply str)))

(defn get-etuovi-section [page-hickup section-name]
  (if-let [path (find-term section-name page-hickup)]
    (let [section-hickup (as-> path x
                           (navigate x 3 [])
                           (get-in page-hickup x))]
      (as-> section-hickup x
        (find-term :dl x)
        (navigate x 1 [])
        (get-in section-hickup x)
        (->> x
             (filter #(and (vector? %)
                           (#{:dd :dt} (first %))))
             (map clean-value)
             (apply hash-map))))
    {}))

(defn get-oikotie-value [hickup term]
  (let [path (find-term term hickup)]
    (get-in hickup (concat (take (- (count path) 3)
                                 path)
                           [5 2]))))

(defn get-oikotie-section [hickup section-name]
  (let [section-path (find-term section-name hickup)]
    (loop [values {}
           term-index 0]
      (if-let [term (get-in hickup
                            (-> section-path
                                (navigate 3 [5 (+ 3 (* 2 term-index)) 3 3 2])))]
        (recur (assoc values term (get-oikotie-value hickup term))
               (inc term-index))
        values))))

(defn get-oikotie-ids [hickup]
  #_hickup
  (let [result-list-path (find-term {:search-cards "",
                                     :cards "cards",
                                     :ng-if "view === 'list' && !loading",
                                     :class "columns show-for-medium-up search-view-list"}
                                    hickup)
        result-list (get-in hickup
                            (-> result-list-path
                                (navigate 2 [])))]
    (clojure.pprint/pprint result-list)
    #_(->> (map #(when (vector? %)
                   (-> % second :id))
                result-list)
           (filter identity))))

(defn get-time [show-time]
  (-> show-time
      (clojure.string/split #" ")
      (nth 2)))

(defn get-etuovi-show-time [hickup]
  (let [section-path (find-term  "Esittely" hickup)]
    (get-in hickup
            (-> section-path
                (navigate 3 [5 3 2])))))


(defn get-address [show-time]
  (->> (clojure.string/split show-time #", ")
       (drop 2)
       (interpose ", ")
       (apply str)))

(defn get-etuovi-address [hickup]
  (let [section-path (find-term {:id "reference_number"} hickup)]
    (get-in hickup
            (-> section-path
                (navigate 3 [5 3 2])))))


(defn get-hickup [url]
  (into [] (hickory/as-hiccup (hickory/parse (:body (http/get url))))) )

(defn get-oikotie-data [url]
  (let [hickup (get-hickup url)]
    
    (conj (get-oikotie-section hickup "Perustiedot")
          (get-oikotie-section hickup "Hintatiedot ja muut kustannukset")
          (get-oikotie-section hickup "Talon ja tontin tiedot"))))

#_(get-oikotie-data "http://asunnot.oikotie.fi/myytavat-tontit/13405809")




(defn get-oikotie-lot-ids [query-url]
  (-> query-url
      (http/get)
      (:body)
      (json/read-str)
      (get "cards")
      (->> (map (fn [item]
                  (get item "id"))))))

#_ (get-oikotie-lot-ids)


(defn get-sections [hickup section-getter & section-names]
  (reduce conj
          {}
          (map #(section-getter hickup %)
               section-names)))

(defn get-etuovi-lot-data [hickup]
  (get-sections hickup
                get-etuovi-section
                "Kohteen perustiedot"
                "Hinta ja kustannukset"
                "Kohteen lisätiedot"
                "Tontti ja kaavoitus"))



(defn get-etuovi-lot-ids [hickup]
  (let [result-list-path (find-term {:class "results list"} hickup)
        result-list (get-in hickup
                            (-> result-list-path
                                (navigate 1 [21])))]
    (->> (map #(when (vector? %)
                 (-> % second :id))
              result-list)
         (filter identity))))

(defn next-url [hickup]
  (let [span-path (find-term "Seuraava" hickup)]
    (get-in hickup
            (-> span-path
                (navigate 2 [1 :href])))))

(def etuovi-lot-query-url-base "http://www.etuovi.com/myytavat-tontit/")

(defn get-all-etuovi-lot-ids [query-url]
  (loop [url query-url
         ids []]
    (println "getting ids from " (str etuovi-lot-query-url-base url))
    (Thread/sleep 1000)
    (let [hickup (get-hickup (str etuovi-lot-query-url-base url))
          next-url (next-url hickup)
          ids (concat ids (get-etuovi-lot-ids hickup))]
      (if next-url
        (recur next-url
               ids)
        ids))))

#_(let [hickup (get-hickup (str etuovi-lot-query-url-base "tulokset?haku=M180324382&so=ptd"))
        next-url (next-url hickup)]
    (get-etuovi-lot-ids hickup))


#_(http/get (str etuovi-lot-query-url-base "tulokset?haku=M180324382&so=ptd"))

#_(def test-hickup (get-hickup "http://asunnot.oikotie.fi/myytavat-tontit/13328718"))
#_(def test-list-hickup (get-hickup "http://asunnot.oikotie.fi/myytavat-tontit?cardType=104&locations=%5B%5B65,6,%22Vantaa%22%5D%5D"))
#_(clojure.pprint/pprint test-list-hickup)

(defn start []
  #_(get-oikotie-section test-hickup
                         "Perustiedot")

  #_(get-oikotie-ids test-list-hickup )

  #_(get-oikotie-data "http://asunnot.oikotie.fi/myytavat-tontit/13328718"))

;; test


#_(def hickup (get-hickup "http://www.etuovi.com/kohde/9511178"))

#_(get-etuovi-section hickup "Tontti ja kaavoitus")

#_(get-etuovi-lot-data hickup)

#_(get-etuovi-section hickup "Kohteen perustiedot")

#_(get-data "http://asunnot.oikotie.fi/myytavat-tontit/10855056")


#_(def result (json/read-str (:body (http/get "http://asunnot.oikotie.fi/api/cards?cardType=104&limit=1000&locations=%5B%5B65,6,%22Vantaa%22%5D%5D&offset=0&sortBy=published_desc"))))
#_ (->> (get result "cards")
        (map (fn [item]
               (get item "id"))))
