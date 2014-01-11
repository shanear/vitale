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
      :track (join "," (map :name (get-teams)))}
    :oauth-creds my-creds
    :callbacks *gather-training-tweets-callback*))

; (defn print-if-nba
;   "print string if it's predicted to be related to the NBA"
;   [tweet-str]
;   (if (> (team-p :nba tweet-str) 0.59) (println tweet-str)))

; (def ^:dynamic
;     *stream-nba-tweets-callback*
;     (AsyncStreamingCallback.
;       (comp print-if-nba #(:text %) json/read-json #(str %2))
;       (comp println response-return-everything)
;       exception-print))

; (defn stream-nba-tweets
;   "begin scanning twitter and "
;   []
;   (statuses-filter
;     :params {
;       :track (join "," (map :name (get-teams)))}
;     :oauth-creds my-creds
;     :callbacks *stream-nba-tweets-callback*))

(defn find-teams-in
  [s]
  (filter
    #(re-find (re-pattern %) (.toLowerCase s))
    (map #(.toLowerCase (:name %)) (get-teams))))

 (defn classify-team-in-tweet
   [tweet team]
   (println (:text tweet))
   (println (format "Does %s refer to the NBA team? (y)es (n)o (d)on't know" team))
   (let [response (read-line)]
     (case response
       "y" (record-unigrams (:text tweet) team true)
       "n" (record-unigrams (:text tweet) team false)
       (println "Moving on...")))
   (println (format "Tweet NBA P: %.9f" (team-p team (:text tweet))))
   (println "\n"))

 (defn classify-tweet
   [tweet]
   (doseq [team (find-teams-in (:text tweet))]
     (classify-team-in-tweet tweet team))
   (delete-tweet tweet))

 (defn start-training
   []
   (doseq [tweet (load-tweets)] (classify-tweet tweet)))

(defn -main [& args] 1)