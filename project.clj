(defproject metosin/schema-tools "0.10.0"
  :description "Common utilities for Prismatic Schema"
  :url "https://github.com/metosin/schema-tools"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :dependencies [[prismatic/schema "1.1.7"]]
  :plugins [[funcool/codeina "0.5.0"]
            [lein-doo "0.1.8"]]
  :test-paths ["test/clj" "test/cljc"]
  :codeina {:target "doc"
            :src-uri "http://github.com/metosin/schema-tools/blob/master/"
            :src-uri-prefix "#L"}
  :profiles {:dev {:plugins [[jonase/eastwood "0.2.5"]]
                   :dependencies [[criterium "0.4.4"]
                                  [org.clojure/clojure "1.8.0"]
                                  [org.clojure/clojurescript "1.9.946"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}}
  :aliases {"all" ["with-profile" "dev:dev,1.9"]
            "test-clj" ["all" "do" ["test"] ["check"]]
            "test-phantom" ["doo" "phantom" "test"]
            "test-advanced" ["doo" "phantom" "advanced-test"]
            "test-node" ["doo" "node" "node-test"]}
  ;; Below, :process-shim false is workaround for <https://github.com/bensu/doo/pull/141>
  :cljsbuild {:builds [{:id "test"
                        :source-paths ["src" "test/cljc" "test/cljs"]
                        :compiler {:output-to "target/out/test.js"
                                   :output-dir "target/out"
                                   :main schema-tools.doo-runner
                                   :optimizations :none
                                   :process-shim false}}
                       {:id "advanced-test"
                        :source-paths ["src" "test/cljc" "test/cljs"]
                        :compiler {:output-to "target/advanced_out/test.js"
                                   :output-dir "target/advanced_out"
                                   :main schema-tools.doo-runner
                                   :optimizations :advanced
                                   :process-shim false}}
                       ;; Node.js requires :target :nodejs, hence the separate
                       ;; build configuration.
                       {:id "node-test"
                        :source-paths ["src" "test/cljc" "test/cljs"]
                        :compiler {:output-to "target/node_out/test.js"
                                   :output-dir "target/node_out"
                                   :main schema-tools.doo-runner
                                   :optimizations :none
                                   :target :nodejs
                                   :process-shim false}}]})
