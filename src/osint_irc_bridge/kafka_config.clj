(ns osint-irc-bridge.kafka-config
  (:require
    [clojure.pprint :as p :refer [pprint]]
    [franzy.admin.zookeeper.client :as client]
    [franzy.admin.zookeeper.defaults :as zk-defaults]))

(defn make-zk-config [servers]
  (merge (zk-defaults/zk-client-defaults) {:servers servers} ))

(defn config-str [config-map]
  (str (:ip config-map) ":" (:port config-map)))