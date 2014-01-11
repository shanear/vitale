(ns vitale.tweets
  (:use vitale.unigrams
        [carica.core])
  (:require [clojure.java.jdbc :as sql]
            [clojure.data.xml :as xml]
            [clojure.string :as string]
            [http.async.client :as http]))

(defn retweet?
  "is this tweet a retweet?"
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
  "filter twitter urls from a string"
  [s]
  (string/replace s #"https?\:\/\/[^\s]+" ""))

(defn filter-special-chars
  "filter twitter special characters from string. examples: &amp; &lt;"
  [s]
  (string/replace s #"\&[a-z]+;" ""))

(defn tweet-unigrams
  "break a tweet into its relevant unigrams"
  [tweet-str]
  (-> tweet-str
      .toLowerCase
      filter-urls
      filter-special-chars
      (#(re-seq #"[0-9]+[:-][0-9]+|\@?[\pL\pN_]+\'?[\pL\pN]*" %))
      ((partial filter #(not= 1 (count %))))
      ))

(defn record-unigrams
  "break a tweet down into its unigrams and record tagging"
  [tweet-str team nba?]
  (println tweet-str)
  (doseq
    [word (disj (set (tweet-unigrams tweet-str)) team)]
    (inc-unigram team word nba?)))

(defn team-p
  "the probability that a tweet containing the NBA team name is referencing the NBA"
  [team tweet-str]
  (let [unigrams (tweet-unigrams tweet-str)
        ps       (map (partial unigram-p team) unigrams)

        ; Use top 4 most "interesting" words to calculate probability
        top-ps   (take 4 (sort-by #(- (Math/abs (- 0.5 (float %)))) ps))]
  (float (/ (reduce + top-ps) (count top-ps)))))

