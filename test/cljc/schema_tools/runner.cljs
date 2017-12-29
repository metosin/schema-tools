(ns schema-tools.runner
  (:require [cljs.test :as test]
            [cljs.nodejs :as nodejs]
            schema-tools.core-test
            schema-tools.walk-test
            schema-tools.select-schema-test
            schema-tools.coerce-test
            schema-tools.experimental.walk-test))

(nodejs/enable-util-print!)

(def status (atom nil))

(defn -main []
  (test/run-all-tests #"^schema-tools.*-test$")
  (js/process.exit @status))

(defmethod test/report [:cljs.test/default :end-run-tests] [m]
  (reset! status (if (test/successful? m) 0 1)))

(set! *main-cli-fn* -main)
