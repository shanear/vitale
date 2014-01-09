(ns vitale.teams
  (:use [carica.core])
  (:require [clojure.java.jdbc :as sql]
            [clojure.data.xml :as xml]
            [http.async.client :as http]))

(defn save-team
  [team]
  (sql/with-db-connection [db-con (config :db :url)]
    (sql/insert! db-con :teams (assoc (dissoc team :api-id) :api_id (team :api-id)))))

(defn teams
  []
  (sql/with-db-connection [db-con (config :db :url)]
    (map
      #(assoc (dissoc % :api_id) :api-id (:api_id %))
      (into [] (sql/query db-con ["select * from teams"])))))

(defn teams-from-api
  "query the SportsData api and process XML response to return a vector of NBA teams"
  []
  (with-open [client (http/create-client)]
    (let [url (str (config :sports-data :url)
              "league/hierarchy.xml?api_key="
              (config :sports-data :key))
          response (http/GET client url)]
        (->
          response
          http/await
          http/string
          xml/parse-str
          :content
          ((partial map :content))
          ((partial reduce into []))
          ((partial map :content))
          ((partial reduce into []))
          ((partial map :attrs))))))

(defn update-teams
  "update database NBA team cache with api data"
  []
  (sql/with-db-connection [db-con (config :db :url)] (sql/delete! db-con :teams ["true"]))
  (let [teams (teams-from-api)]
    (map save-team (map #(assoc (dissoc % :id) :api-id (:id %)) teams))))

