(ns vitale.tweets
  (:use vitale.unigrams
        [carica.core]
        [clojure.string :only (replace)])
  (:require [clojure.java.jdbc :as sql]
            [clojure.data.xml :as xml]
            [http.async.client :as http]))

(defn retweet?
  [tweet-str]
  (= "RT" (subs tweet-str 0 2)))

(defn save-tweet
  [tweet-str]
  (when-not (retweet? tweet-str)
    (do
      (println (str "Saving Tweet... " tweet-str))
      (sql/with-db-connection [db-con (config :db :url)]
        (sql/insert! db-con :tweets {:text tweet-str})))))

(defn load-tweets
  []
  (sql/with-db-connection [db-con (config :db :url)]
    (into [] (sql/query db-con
      ["select * from tweets"]))))

(defn clear-tweets
  []
  (sql/with-db-connection [db-con (config :db :url)]
    (sql/delete! db-con :tweets ["true"])))

(defn delete-tweet
  [tweet]
  (sql/with-db-connection [db-con (config :db :url)]
    (sql/delete! db-con :tweets ["id = ?" (:id tweet)])))

(defn filter-urls
  [s]
  (replace s #"https?\:\/\/[^\s]+" ""))

(defn filter-special-chars
  [s]
  (replace s #"\&[a-z]+;" ""))

(defn tweet-unigrams
  [tweet-str]
  (-> tweet-str
      .toLowerCase
      filter-urls
      filter-special-chars
      (#(re-seq #"[0-9]+[:-][0-9]+|\@?[\pL\pN_]+\'?[\pL\pN]*" %))
      ((partial filter #(not= 1 (count %))))
      ))

(defn decompose-tweet
  [tweet & tags]
  (println (:text tweet))
  (delete-tweet tweet)
  (doseq
    [unigram (tweet-unigrams (:text tweet))]
    (inc-unigram unigram tags)))