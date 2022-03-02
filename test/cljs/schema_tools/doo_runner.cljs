(ns schema-tools.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            schema-tools.core-test
            schema-tools.walk-test
            schema-tools.select-schema-test
            schema-tools.coerce-test
            schema-tools.swagger.core-test
            schema-tools.openapi.core-test
            schema-tools.experimental.walk-test))

(enable-console-print!)

(doo-tests 'schema-tools.core-test
           'schema-tools.walk-test
           'schema-tools.select-schema-test
           'schema-tools.coerce-test
           'schema-tools.swagger.core-test
           'schema-tools.openapi.core-test
           'schema-tools.experimental.walk-test)
