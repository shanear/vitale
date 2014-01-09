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
    *gather-training-tweets-callback*
    (AsyncStreamingCallback.
      (comp save-tweet #(:text %) json/read-json #(str %2))
      (comp println response-return-everything)
      exception-print))

(defn gather-training-tweets
  "begin scanning twitter and saving tweets into training database"
  []
  (statuses-filter
    :params {
      :track (join "," (map :name (teams)))}
    :oauth-creds my-creds
    :callbacks *gather-training-tweets-callback*))

(defn print-if-nba
  "print string if it's predicted to be related to the NBA"
  [tweet-str]
  (if (> (tweet-tag-p :nba tweet-str) 0.59) (println tweet-str)))

(def ^:dynamic
    *stream-nba-tweets-callback*
    (AsyncStreamingCallback.
      (comp print-if-nba #(:text %) json/read-json #(str %2))
      (comp println response-return-everything)
      exception-print))

(defn stream-nba-tweets
  "begin scanning twitter and "
  []
  (statuses-filter
    :params {
      :track (join "," (map :name (teams)))}
    :oauth-creds my-creds
    :callbacks *stream-nba-tweets-callback*))

(defn classify-tweet
  [tweet]
  (println (:text tweet))
  (println "Is this tweet NBA related? (y)es (n)o (d)on't know")
  (let [response (read-line)]
    (case response
      "y" (decompose-tweet tweet :nba)
      "n" (decompose-tweet tweet)
      (delete-tweet tweet)))
  (println (format "Tweet NBA P: %.9f" (tweet-tag-p :nba (:text tweet))))
  (println "\n"))

(defn start-training
  []
  (doseq [tweet (load-tweets)] (classify-tweet tweet)))

(defn -main [& args] (start-training))