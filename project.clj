(defproject metosin/schema-tools "0.10.3"
  :description "Common utilities for Prismatic Schema"
  :url "https://github.com/metosin/schema-tools"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v20.html"}
  :dependencies [[prismatic/schema "1.1.9"]]
  :plugins [[funcool/codeina "0.5.0"]
            [lein-doo "0.1.10"]]
  :test-paths ["test/clj" "test/cljc"]
  :codeina {:target "doc"
            :src-uri "http://github.com/metosin/schema-tools/blob/master/"
            :src-uri-prefix "#L"}
  :profiles {:dev {:plugins [[jonase/eastwood "0.2.6"]]
                   :dependencies [[criterium "0.4.4"]
                                  [org.clojure/clojure "1.9.0"]
                                  [org.clojure/clojurescript "1.10.238"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.10 {:dependencies [[org.clojure/clojure "1.10.0-alpha4"]
                                   [org.clojure/clojurescript "1.10.238"]]}}
  :aliases {"all" ["with-profile" "dev:dev,1.8"]
            "all-cljs" ["with-profile" "dev:dev,1.10"]
            "test-clj" ["all" "do" ["test"] ["check"]]
            "test-cljs" ["all-cljs" "do" ["test-node"] ["test-chrome"] ["test-advanced"]]
            "test-chrome" ["doo" "chrome-headless" "test" "once"]
            "test-advanced" ["doo" "phantom" "advanced-test" "once"]
            "test-node" ["doo" "node" "node-test" "once"]}
  :cljsbuild {:builds [{:id "test"
                        :source-paths ["src" "test/cljc" "test/cljs"]
                        :compiler {:output-to "target/out/test.js"
                                   :output-dir "target/out"
                                   :main schema-tools.doo-runner
                                   :optimizations :none}}
                       {:id "advanced-test"
                        :source-paths ["src" "test/cljc" "test/cljs"]
                        :compiler {:output-to "target/advanced_out/test.js"
                                   :output-dir "target/advanced_out"
                                   :main schema-tools.doo-runner
                                   :optimizations :advanced}}
                       ;; Node.js requires :target :nodejs, hence the separate
                       ;; build configuration.
                       {:id "node-test"
                        :source-paths ["src" "test/cljc" "test/cljs"]
                        :compiler {:output-to "target/node_out/test.js"
                                   :output-dir "target/node_out"
                                   :main schema-tools.doo-runner
                                   :optimizations :none
                                   :target :nodejs}}]})
