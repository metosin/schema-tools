(defproject metosin/schema-tools "0.6.2"
  :description "Common utilities for Prismatic Schema"
  :url "https://github.com/metosin/schema-tools"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :dependencies [[prismatic/schema "1.0.3"]]
  :plugins [[codox "0.8.13"]]

  :codox {:src-dir-uri "http://github.com/metosin/schema-tools/blob/master/"
          :src-linenum-anchor-prefix "L"}

  :profiles {:dev {:plugins [[jonase/eastwood "0.2.1"]]
                   :dependencies [[criterium "0.4.3"]
                                  [org.clojure/clojure "1.7.0"]
                                  [org.clojure/clojurescript "1.7.145"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0-beta2"]]}}
  :aliases {"all" ["with-profile" "dev:dev,1.8"]
            "test-clj" ["all" "do" ["test"] ["check"]]})
