(ns osint-irc-bridge.core
  (:require [clojure.spec :as s]
            [irclj.core :as irc]
            [clojure.pprint :as p :refer [pprint]]
            [clj-time.local :as l]
            [clj-time.core :as t]
            [clojure.spec.test :as stest]
            [osint-irc-bridge.chunker :as chunker]
            [clojure.core.async
             :as a
             :refer [>! <! >!! <!! go chan buffer close! thread
                     alts! alts!! timeout go-loop onto-chan]])
  (:gen-class))

#_(defn -main [config-edn-file]
  (let [config (read-string (slurp config-edn-file))
        kafka (osint-irc-bridge.kafka-config/make-zk-config (map osint-irc-bridge.kafka-config/config-str (:kafka config)))]

    (osint-irc-bridge.kafka/into-kafka )

    (println )))


(defmacro dbg [body]
  `(let [x# ~body]
     (println "dbg:" '~body "=" x#)
     x#))

(s/def ::nick (s/and string? #(>= (count %) 1)))
(s/def ::text string?)
(s/def ::channel (s/and string? #(>= (count %) 1)))
(s/def ::recvtime inst?)
(s/def ::irc-msg (s/keys :req [::nick ::text ::channel ::recvtime]))

(s/def ::msg-chunk (s/* ::irc-msg))

(s/def ::name ::channel)

;; ::other-output is { time<long>  output<chan>}

(s/def ::channel-config (s/keys :req [::name ::original-output]
                                :opt [::other-output]))

(defn strip-and-timestamp [msg]
  {::nick     (:nick msg)
   ::channel  (:target msg)
   ::text     (:text msg)
   ::recvtime (l/local-now)})

(s/fdef strip-and-timestamp
        :ret ::irc-msg)

#_(stest/instrument `strip-and-timestamp)

(def strip-and-timestamp-td (map strip-and-timestamp))

(def channels (ref {}))

(def main-irc-chan (chan 100 strip-and-timestamp-td))

(def mut-main-irc-chan (atom main-irc-chan))

(defn join! [channels connection channel-name]
  (dosync
    (alter channels assoc (keyword channel-name) {::name            channel-name
                                                  ::original-output (chan)}))

  (irc/join connection channel-name))

(defn print-irc [channels]
  (go-loop []
    (let [chans (map #(::original-output (second %)) @channels)
          [irc-msg c] (alts! chans)]
      (pprint irc-msg))
    (recur)))

(defn channel-multiplex! [channels input-chan]
  (go-loop []
    (let [irc-msg (<! input-chan)
          channel-name (::channel irc-msg)
          channel-name-keyword (keyword channel-name)]
      (if (@channels channel-name-keyword)
        (>! (::original-output (@channels channel-name-keyword))
            irc-msg)
        (println "PANIC UNKOWN CHANNEL")))
    (recur)))

;; chunk-period must support InTimeUnitProtocol (in-millis)
(defn add-time-chunker-chan! [channels channel-name output-channel-name chunk-period output-chan]
  (let [channel-name-keyword (keyword channel-name)]
    (dosync
      (alter channels
             assoc-in
             [channel-name-keyword ::other-output output-channel-name]
             output-chan))
    (let [msg-chan (dbg (::original-output (channel-name-keyword @channels)))]
      (chunker/time-chunker msg-chan
                            (dbg (t/in-millis chunk-period))
                            output-chan))))

#_(defn init1 []
  (let [main-irc (chan 100 strip-and-timestamp-td)
        connection (irc/connect "irc.freenode.net" 6667 "OSINT_BOT12" :callbacks {:privmsg (fn [_ msg] (>!! main-irc msg))})]
    (join! channels connection "#luxeria")
    (join! channels connection "#luxeria")
    (channel-multiplex! channels main-irc)
    (add-time-chunker-chan! channels "#luxeria" :acc-min (t/seconds 1) (chan 1000 osint-irc-bridge.accumulate/accumulate-td))
    (add-time-chunker-chan! channels "#luxeria" :acc-min (t/seconds 15) (chan 1000 osint-irc-bridge.accumulate/accumulate-td))
    (add-time-chunker-chan! channels "#luxeria" :acc-min (t/minutes 1) (chan 1000 osint-irc-bridge.accumulate/accumulate-td))))

(defn add-channel
  ([publisher conn channel-name]
   (add-channel publisher conn channel-name (chan)))
  ([publisher conn channel-name output]
    #_(irc/join conn channel-name)
   (a/sub publisher channel-name output)
   output))

(defn producer-by-spec [msg-chan spec channel-name time]
  (let [close-channel (chan)]
    (go-loop [timer-chan (timeout time)]
      (let [[msg c] (alts! [close-channel timer-chan])]
        (when-not (= c close-channel)
          (>! msg-chan (if (empty? (seq channel-name))
                         (first (rand-nth (s/exercise spec 100)))
                         (assoc (first (rand-nth (s/exercise spec 100))) ::channel channel-name)))
          (recur (timeout time)))))
    close-channel))

(mac inc)

#_(def irc-chan (chan 100 strip-and-timestamp-td))
#_(def conn (irc/connect "irc.freenode.net" 6667 "OSINT_BOT12" :callbacks {:privmsg (fn [_ msg] (>!! main-irc msg))}))

(def infinte-msg-channel (chan))

(def irc-publisher (a/pub infinte-msg-channel ::channel))

#_(def luxeria-close-chan (producer-by-spec infinte-msg-channel ::irc-msg "#luxeria" 5000))
#_(go (close! luxeria-close-chan))
(def clojure-close-chan (producer-by-spec infinte-msg-channel ::irc-msg "#clojure" 2000))
#_(go (close! clojure-close-chan))
#_(def haskell-close-chan (producer-by-spec infinte-msg-channel ::irc-msg "#haskell" 5000))
#_(go (close! haskell-close-chan))



#_(def haskell-topic-channel (chan))
#_(def haskell-sub (a/sub irc-publisher "#haskell" haskell-topic-channel))

#_(def haskell-mult (a/mult haskell-topic-channel))

#_(def haskell-mult-15s (chan 1))
#_(def haskell-mult-15s-tab (a/tap haskell-mult haskell-mult-15s))
#_(def haskell-15s-acc (chunker/time-chunker haskell-mult-15s (t/in-millis (t/seconds 15)) (chan 1 #_:osint-irc-bridge.accumulate/accumulate-td)))

#_(def haskell-mult-30s (chan 1))
#_(def haskell-mult-30s-tab (a/tap haskell-mult haskell-mult-30s))
#_(def haskell-30s-acc (chunker/time-chunker haskell-mult-30s (t/in-millis (t/seconds 23)) (chan 1 #_:osint-irc-bridge.accumulate/accumulate-td)))

#_(let []
  (go-loop [msg (<! infinte-msg-channel)]
    (println "main: " msg)
    (recur (<! infinte-msg-channel))))

#_(let [c (chan)
      sub (a/sub irc-publisher "#luxeria" c)]
  (go-loop [msg (<! c)]
    (println "luxeria: " msg)
    (recur (<! c))))

(let [c (chan)
      sub (a/sub irc-publisher "#clojure" c)]
  (go-loop [msg (<! c)]
    (println "clojure: " msg)
    (recur (<! c))))

#_(go (while true
      (let [[v c] (alts! [haskell-15s-acc haskell-30s-acc])]
        (condp = c
          haskell-15s-acc (do (println "15: " (type v) ) (pprint v) )
          haskell-30s-acc (do (println "23: " (type v)) (pprint v))))))

#_(let []
  (go-loop [msg (<! haskell-mult-15s)]
    (println "haskell 15: " msg)
    (recur (<! haskell-mult-15s))))

#_(let []
  (go-loop [msg (<! haskell-mult-30s)]
    (println "haskell 30: " msg)
    (recur (<! haskell-mult-30s))))

#_(def output (chunker/time-chunker (a/to-chan [1 2 4 5])
                                    (t/in-millis (t/minutes 1))
                                    (chan 100 osint-irc-bridge.accumulate/accumulate-td)))

#_(pprint @channels)

#_(irc/kill connection)


#_(print-irc channels)

#_(pprint (::original-output (@channels :#luxeria)))

#_(reset! mut-main-irc-chan (chan 100))