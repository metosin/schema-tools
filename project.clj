(defproject metosin/schema-tools "0.1.0-SNAPSHOT"
  :description "Common tools for Prismatic Schema"
  :url "https://github.com/metosin/schema-tools"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [prismatic/schema "0.3.3"]
                 [prismatic/plumbing "0.3.5"]]
  :plugins [[codox "0.8.10"]]

  :cljx {:builds [{:rules :clj
                   :source-paths ["src"]
                   :output-path "target/generated/src"}
                  {:rules :cljs
                   :source-paths ["src"]
                   :output-path "target/generated/src"}]}
  :prep-tasks [["cljx" "once"]]
  :source-paths ["src" "target/generated/src"]

  :codox {:src-dir-uri "http://github.com/metosin/schema-tools/blob/master/"
          :src-linenum-anchor-prefix "L"
          :src-uri-mapping {#"target/generated/src" #(str "src/" % "x")}}

  :profiles {:dev {:plugins [[lein-midje "3.1.3"]
                             [com.keminglabs/cljx "0.5.0"]]
                   :dependencies [[midje "1.6.3"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0-alpha2"]]}}
  :aliases {"all" ["with-profile" "dev:dev,1.7"]
            "test-ancient" ["midje"]})
