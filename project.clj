(defproject osint-irc-bridge "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [irclj "0.5.0-alpha4"]
                 [clj-time "0.12.2"]
                 [org.clojure/core.async "0.2.395"]
                 [org.clojure/test.check "0.9.0"]
                 [ymilky/franzy "0.0.1"]
                 [ymilky/franzy-admin "0.0.1"]
                 [ymilky/travel-zoo "0.0.2"]]
  :main osint-irc-bridge.core)