(ns apartments-gui.ui.ui
  (:require [reagent.core :as reagent :refer [atom]]
            [datascript.core :as d]
            [cljs.test :refer-macros [deftest is testing run-tests]]
            [cljs.core.async :as async]
            [cljs.pprint :as pprint]
            [cljs-http.client :as http]
            [cljs.reader :as reader])
  (:require-macros [cljs.core.async.macros :as async]))



;; generic server communication

(goog-define file-url "http://localhost:4011/file")
(goog-define api-url "http://localhost:4011/api")

(defn call-to-chan [body]
  (let [channel (async/chan)]
    (async/go (let [response (-> (async/<! (http/post api-url
                                                      {:edn-params body
                                                       :with-credentials? false}))
                                 (:body)
                                 (cljs.reader/read-string)
                                 (or :nil))]
                (async/>! channel response)
                (async/close! channel)))
    channel))


(defn post-file-to-chan [file-input-id command]
  (let [channel (async/chan)
        file (-> (.getElementById js/document file-input-id)
                 .-files
                 (aget 0))]
    (async/go (let [response (-> (async/<! (http/post file-url
                                                      {:multipart-params [["file" file]
                                                                          ["command" (pr-str command)]]
                                                       :with-credentials? false}))
                                 (:body)
                                 (cljs.reader/read-string)
                                 (or :nil))]
                (async/>! channel response)
                (async/close! channel)))
    channel))

(defn handle-result-from-chan [chan callback]
  (async/go (callback (async/<! chan))))


(defn call [body callback]
  (-> (call-to-chan body)
      (handle-result-from-chan callback)))

(defn post-file [file-input-id command callback]
  (-> (post-file-to-chan file-input-id command)
      (handle-result-from-chan callback)))



;; generic UI code

(defonce state-atom (reagent/atom {:transaction-channel (async/chan)}))

(defn apply-to-state [transaction-channel function & arguments]
  (async/put! transaction-channel
              (fn [state]
                (apply function state arguments))))

(defn call-and-apply-to-state [transaction-channel command function]
  (call command
        (fn [result]
          (apply-to-state transaction-channel (fn [state]
                                  (function result state))))))

(defn page-state-path [state path]
  (concat [:page-state (:page state)]
          path))

(defn get-in-page-state [state path]
  (get-in state
          (page-state-path state path)))

(defn update-in-page-state [state path function & arguments]
  (apply update-in
         state
         (page-state-path state path)
         function
         arguments))

(defn assoc-in-page-state [state path value]
  (assoc-in state
            (page-state-path state path)
            value))

(defn assoc-many-in-page-state [state & kvs]
  (apply update-in-page-state state [] assoc kvs))



