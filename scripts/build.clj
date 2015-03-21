(require 'cljs.closure)

(defrecord SourcePaths [paths]
  cljs.closure/Compilable
  (-compile [_ opts]
    (mapcat #(cljs.closure/-compile % opts) paths)))

(cljs.closure/build
  (SourcePaths. ["src" "test" "target/generated/src"])
  {:output-to "target/generated/js/out/tests.js"
   :source-map "target/generated/js/out/tests.map.js"
   :output-dir "target/generated/js/out"
   :optimizations :none
   :target :nodejs})

(shutdown-agents)
