(ns vitale.core
  (:use
    vitale.maps
    vitale.teams
    [carica.core]
    [clojure.string :only (join)]
    [twitter.oauth]
    [twitter.callbacks]
    [twitter.callbacks.handlers]
    [twitter.api.streaming])
  (:require
    [clojure.data.json :as json]
    [http.async.client :as ac])
  (:import
    (twitter.callbacks.protocols AsyncStreamingCallback)))

(def my-creds
  (make-oauth-creds
    (config :consumer-key)
    (config :consumer-secret)
    (config :access-token)
    (config :access-token-secret)))

(def ^:dynamic
    *streaming-callback*
    (AsyncStreamingCallback.
      (comp println #(:text %) json/read-json #(str %2))
      (comp println response-return-everything)
      exception-print))

(defn start-scanning
  []
  (statuses-filter
    :params {
      :track (join (:alabama team-terms) ",")
      :coords (:tuscaloosa city-coords)}
    :oauth-creds my-creds
    :callbacks *streaming-callback*))

(defn -main [& args] (start-scanning))