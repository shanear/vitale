(ns vitale.unigrams
  (:use [carica.core]
        [clojure.pprint :only (pprint)])
  (:require [clojure.java.jdbc :as sql]))

(defn get-unigram
  "find occurance data of unigram from DB"
  [word]
  (sql/with-db-connection [db-con (config :db :url)]
    (first (sql/query db-con
      ["select * from unigrams where word=?", word]))))

(defn load-unigrams
  "load all unigrams"
  []
  (sql/with-db-connection [db-con (config :db :url)]
    (into [] (sql/query db-con
        ["select * from unigrams"]))))

(defn unigram-p
  "Get the probability of a tweet containing the unigram-str being related to the tag"
  [tag unigram-str]
  (let [unigram (or (get-unigram unigram-str) {})
        total-count (get unigram :total 1)
        tag-count (get unigram tag 0)]
    (if (< total-count 2) 2/5 ; if not enough data, default to 40%
      (if (zero? tag-count) 1/100 ; Add padding for extremes
        (if (= tag-count total-count) 99/100
          (/ tag-count total-count))))))

(defn inc-unigram
  "record a sighting of the unigram as a part of the tag"
  [word tags]
  (if-let [unigram (get-unigram word)]
    (sql/with-db-connection [db-con (config :db :url)]
      (let [updates (reduce #(assoc % %2 (inc (get % %2 0))) unigram (conj tags :total))]
        (do
          (pprint (merge {:word word} updates))
          (sql/update! db-con :unigrams updates ["word = ?" word]))))
    (let [unigram (merge {:word word :total 1} (zipmap tags (repeat 1)))]
      (do
        (print "New word...")
        (pprint unigram)
        (sql/with-db-connection [db-con (config :db :url)]
          (sql/insert! db-con :unigrams unigram))))))