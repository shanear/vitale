(ns vitale.core
  (:use
    vitale.unigrams
    vitale.tweets
    vitale.teams
    [carica.core]
    [clojure.string :only (join)]
    [twitter.oauth]
    [twitter.callbacks]
    [twitter.callbacks.handlers]
    [twitter.api.streaming])
  (:require
    [clojure.java.jdbc :as sql]
    [clojure.data.xml :as xml]
    [clojure.data.json :as json]
    [http.async.client :as http])
  (:import
    (twitter.callbacks.protocols AsyncStreamingCallback))
  (:gen-class))

(def my-creds
  (make-oauth-creds
    (config :twitter :consumer-key)
    (config :twitter :consumer-secret)
    (config :twitter :access-token)
    (config :twitter :access-token-secret)))

(def ^:dynamic
    *streaming-callback*
    (AsyncStreamingCallback.
      (comp save-tweet #(:text %) json/read-json #(str %2))
      (comp println response-return-everything)
      exception-print))

(defn start-scanning
  []
  (statuses-filter
    :params {
      :track (join "," (map :name (teams)))}
    :oauth-creds my-creds
    :callbacks *streaming-callback*))

(defn start-classifying
  []
  (doseq [tweet (load-tweets)] (classify-tweet tweet)))

(defn classify-tweet
  [tweet]
  (println (:text tweet))
  (println "Is this tweet NBA related? (y)es (n)o (d)on't know")
  (let [response (read-line)]
    (case response
      "y" (decompose-tweet tweet :nba)
      "n" (decompose-tweet tweet)
      (delete-tweet tweet)))
  (println "\n"))


(defn -main [& args] (start-scanning))