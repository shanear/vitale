(ns vitale.unigrams
  (:use [carica.core]
        [clojure.pprint :only (pprint)])
  (:require [clojure.java.jdbc :as sql]))

(defn find-unigram
  [word]
  (sql/with-db-connection [db-con (config :db :url)]
    (first (sql/query db-con
      ["select * from unigrams where word=?", word]))))

(defn inc-unigram
  [word tags]
  (if-let [unigram (find-unigram word)]
    (sql/with-db-connection [db-con (config :db :url)]
      (let [updates (reduce #(assoc % %2 (inc (get % %2 0))) unigram (conj tags :total))]
        (do
          (pprint (merge {:word word} updates))
          (sql/update! db-con :unigrams updates ["word = ?" word]))))
    (let [unigram (merge {:word word :total 1} (zipmap tags (repeat 1)))]
      (do
        (pprint unigram)
        (sql/with-db-connection [db-con (config :db :url)]
          (sql/insert! db-con :unigrams unigram))))))