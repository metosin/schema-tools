(ns schema-tools.coerce-test
  (:require #+clj [clojure.test :refer [deftest testing is]]
    #+cljs [cljs.test :as test :refer-macros [deftest testing is]]
            [schema.coerce :as sc]
            [clojure.string :as str]
            [schema-tools.coerce :as stc]
            [schema.utils :as su]))

(deftest forwarding-matcher-test
  (let [string->vec (fn [schema]
                      (if (vector? schema)
                        (fn [x]
                          (str/split x #","))))
        string->long (fn [schema]
                       (if (= Long schema)
                         (fn [x]
                           (Long/parseLong x))))
        string->vec->long (stc/forwarding-matcher string->vec string->long)
        string->long->vec (stc/forwarding-matcher string->long string->vec)]
    (testing "string->vec->long is able to parse \"1,2,3\" "
      (is (= ((sc/coercer [Long] string->vec->long) "1,2,3") [1, 2, 3])))
    (testing "string->long->vec fails to parse \"1,2,3\" "
      (is (su/error? ((sc/coercer [Long] string->long->vec) "1,2,3"))))))
