(ns osint-irc-bridge.chunker
  (:require [clojure.spec :as s]
            [clojure.pprint :as p :refer [pprint]]
            [clojure.core.async
             :as a
             :refer [>! <! >!! <!! go chan buffer close! thread
                     alts! alts!! timeout go-loop onto-chan]]))

(defn time-chunker
  ([msg-chan time]
    (time-chunker msg-chan time (chan)))
  ([msg-chan time output-channel]
   (go-loop [chunk []
             timeout-channel (timeout time)]
     (let [[msg c] (alts! [timeout-channel msg-chan])]
       (if (= c msg-chan)
         (do (recur (conj chunk msg) timeout-channel))
         (do (>! output-channel chunk)
             (recur [] (timeout time))))))
   output-channel))

#_(def msg-chan (chan 1000))

#_(def output-chan (chan 1 osint-irc-bridge.accumulate/accumulate-td))

#_(time-chunker msg-chan 10000 output-chan)

#_(go-loop []
    (pprint (<! output-chan))
    (recur))

#_(go-loop []
    (<! (timeout (+ 1000 (rand 200))))
    (>! msg-chan (first (first (s/exercise :osint-irc-bridge.core/irc-msg 1))))
    (recur))