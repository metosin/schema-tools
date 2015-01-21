(defproject metosin/schema-tools "0.1.2"
  :description "Common utilities for Prismatic Schema"
  :url "https://github.com/metosin/schema-tools"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [prismatic/schema "0.3.3"]]
  :plugins [[codox "0.8.10"]]

  :cljx {:builds [{:rules :clj
                   :source-paths ["src"]
                   :output-path "target/generated/src"}
                  {:rules :cljs
                   :source-paths ["src"]
                   :output-path "target/generated/src"}
                  {:rules :clj
                   :source-paths ["test"]
                   :output-path "target/generated/test"}
                  {:rules :cljs
                   :source-paths ["test"]
                   :output-path "target/generated/test"}]}
  :prep-tasks [["cljx" "once"]]
  :source-paths ["src" "target/generated/src"]
  :test-paths ["test" "target/generated/test"]

  :cljsbuild
  {:builds [{:id "test"
             :source-paths ["src" "target/generated/src" "target/generated/test"]
             :compiler {:output-to "target/generated/js/tests.js"
                        :source-map "target/generated/js/tests.map.js"
                        :output-dir "target/generated/js/out"
                        :optimizations :simple
                        :cache-analysis true}}]
   :test-commands
   {"unit" ["node" "target/generated/js/tests.js"]}}

  :codox {:src-dir-uri "http://github.com/metosin/schema-tools/blob/master/"
          :src-linenum-anchor-prefix "L"
          :src-uri-mapping {#"target/generated/src" #(str "src/" % "x")}}

  :profiles {:dev {:plugins [[com.keminglabs/cljx "0.5.0"]
                             [jonase/eastwood "0.2.1"]
                             [lein-cljsbuild "1.0.4"]]
                   :dependencies [[criterium "0.4.3"]
                                  [org.clojure/clojurescript "0.0-2665"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0-alpha5"]]}}
  :aliases {"all" ["with-profile" "dev:dev,1.7"]
            "test-clj" ["do" ["cljx" "once"] ["test"] ["check"]]
            "test-node" ["do" ["cljx"] ["cljsbuild" "test"]]})
