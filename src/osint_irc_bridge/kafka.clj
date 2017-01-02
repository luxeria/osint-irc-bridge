(ns osint-irc-bridge.kafka
  (:require [franzy.serialization.serializers :as serializers]
            [osint-irc-bridge.kafka-config :as config]
            [franzy.clients.producer.client :as producer]
            [franzy.clients.producer.protocols :refer :all]
            [clojure.core.async
             :as a
             :refer [>! <! >!! <!! go chan buffer close! thread
                     alts! alts!! timeout go-loop onto-chan]]
            [clojure.spec :as s]))

(defn into-kafka
  ([kafka-brokers topic key msg-chan]
   (into-kafka kafka-brokers topic key msg-chan (chan (a/sliding-buffer 100))))
  ([kafka-brokers topic key msg-chan output-chan]
   (go-loop []
     (with-open [p (producer/make-producer
                     {:bootstrap.servers kafka-brokers
                      :client.id         "cljproducertest1"}
                     (serializers/keyword-serializer)
                     (serializers/edn-serializer))]
       (let [next-msg (<! msg-chan)
             kafka-send-opj {:topic topic :partition 0 :key key :value next-msg}
             send-fut (send-async! p kafka-send-opj)]
         (>! output-chan send-fut)))
     (recur))
   output-chan))

#_(
    (def msg-chan (chan 100))
    (def output-chan (chan 100))

    (go-loop []
      (<! (timeout (+ 1000 (rand 200))))
      (>! msg-chan (first (first (s/exercise :osint-irc-bridge.core/irc-msg 1))))
      (recur))

    (go-loop []
      (clojure.pprint/pprint @(<! output-chan))
      (recur))

    (into-kafka (config/topics-to-create :edn-topic) :testkey msg-chan output-chan))
