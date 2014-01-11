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
        [:id :serial "PRIMARY KEY"]
        [:team_name :varchar "NOT NULL"]
        [:word :varchar "NOT NULL"]
        [:nba :integer "NOT NULL" "DEFAULT 0"]
        [:non_nba :integer "NOT NULL" "DEFAULT 0"])
      "CREATE UNIQUE INDEX ON unigrams (team_name, word)")))

(defn -main []
  (print "Creating database tables...") (flush)
  ;(create-teams)
  ;(create-tweets)
  (create-unigrams)
  (println " done"))