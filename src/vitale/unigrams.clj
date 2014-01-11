(ns vitale.unigrams
  (:use [carica.core]
        [clojure.pprint :only (pprint)])
  (:require [clojure.java.jdbc :as sql]))

(defn get-unigram
  "find occurance data of unigram from DB"
  [team word]
  (sql/with-db-connection [db-con (config :db :url)]
    (first (sql/query db-con
      ["select * from unigrams where team_name=? and word=?", team, word]))))

(defn load-unigrams
  "load all unigrams"
  []
  (sql/with-db-connection [db-con (config :db :url)]
    (into [] (sql/query db-con
        ["select * from unigrams"]))))

(defn unigram-p
  "Get the probability of a tweet containing the unigram-str being related to the NBA team"
  [team unigram-str]
  (let [unigram (or (get-unigram team unigram-str) {})
        non-nba-count (get unigram :non_nba 1)
        nba-count (get unigram :nba 0)
        total-count (+ nba-count non-nba-count)]
    (if (< total-count 2)     2/5     ; if not enough data, default to 40% P
    (if (zero? nba-count)     1/100   ; Add padding for extremes
    (if (zero? non-nba-count) 99/100
      (/ nba-count total-count))))))

(defn inc-unigram
  "record unigram relating to a word containing the team name and whether it was NBA related"
  [team word nba?]
  (let [col     (if nba? :nba :non_nba)
        unigram (get-unigram team word)]
    (sql/with-db-connection [db-con (config :db :url)]
      (if unigram
        (let [update {col (inc (get unigram col 0))}]
          (do
            (pprint (merge unigram update))
            (sql/update! db-con :unigrams update ["team_name=? AND word = ?" team, word])))
        (let [new-unigram {:team_name team :word word col 1}]
          (do
            (print "New word...")
            (pprint new-unigram)
            (sql/insert! db-con :unigrams new-unigram)))))))
