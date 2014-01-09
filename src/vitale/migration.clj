(ns vitale.migration
  (:use [carica.core])
  (:require [clojure.java.jdbc :as sql]))

(defn create-teams []
  (sql/with-db-connection [db-con (config :db :url)]
    (sql/db-do-commands db-con
      (sql/create-table-ddl :teams
        [:id :serial "PRIMARY KEY"]
        [:api_id :varchar "NOT NULL"]
        [:market :varchar "NOT NULL"]
        [:name :varchar "NOT NULL"]
        [:alias :varchar "NOT NULL"]))))

(defn create-tweets []
  (sql/with-db-connection [db-con (config :db :url)]
    (sql/db-do-commands db-con
      (sql/create-table-ddl :tweets
        [:id :serial "PRIMARY KEY"]
        [:text :text "NOT NULL"]))))

(defn create-unigrams []
  (sql/with-db-connection [db-con (config :db :url)]
    (sql/db-do-commands db-con
      (sql/create-table-ddl :unigrams
        [:word :varchar "NOT NULL" "PRIMARY KEY"]
        [:total :integer "NOT NULL" "DEFAULT 0"]
        [:nba :integer "NOT NULL" "DEFAULT 0"]))))

(defn -main []
  (print "Creating database tables...") (flush)
  ;(create-teams)
  ;(create-tweets)
  (create-unigrams)
  (println " done"))