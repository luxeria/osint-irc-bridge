(ns osint-irc-bridge.accumulate
  (:require [clojure.spec :as s]
            [clojure.pprint :as p :refer [pprint]]
            [clojure.spec.test :as stest]
            [clojure.core.async
             :as a
             :refer [>! <! >!! <!! go chan buffer close! thread
                     alts! alts!! timeout go-loop]]))


(s/fdef accumulate
        :args [:osint-irc-bridge.core/msg-chunk])

(defn accumulate [msg-chunk]
  (reduce (fn [acc msg]
            (let [nick  (:osint-irc-bridge.core/nick  msg)]
              (if nick
                (update-in acc [nick] conj msg)
                (assoc acc nick [msg])))) {} msg-chunk))

(def accumulate-td (map accumulate))

#_(pprint (accumulate (map first (s/exercise :osint-irc-bridge.core/irc-msg 100))))