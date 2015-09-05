(ns schema-tools.runner
  (:require [cljs.test :as test]
            [cljs.nodejs :as nodejs]
            schema-tools.core-test
            schema-tools.walk-test
            schema-tools.select-schema-test
            schema-tools.coerce-test
            schema-tools.experimental.walk-test))

(nodejs/enable-util-print!)

(defn -main []
  (test/run-all-tests #"^schema-tools.*-test$"))

(set! *main-cli-fn* -main)
