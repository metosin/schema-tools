(ns schema-tools.runner
  (:require [cljs.test :as test]
            [cljs.nodejs :as nodejs]
            schema-tools.core-test))

(nodejs/enable-util-print!)

(defn -main []
  (test/run-all-tests))

(set! *main-cli-fn* -main)
