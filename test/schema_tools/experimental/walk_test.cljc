(ns schema-tools.experimental.walk-test
  (:require #?(:clj  [clojure.test :refer [deftest testing is]]
               :cljs [cljs.test :as test :refer-macros [deftest testing is]])
            schema-tools.experimental.walk
            [schema-tools.walk :as sw]
            [schema.core :as s]
            [schema.experimental.abstract-map :as abstract-map :include-macros true]))

(s/defschema Animal
  (abstract-map/abstract-map-schema
   :type
   {:age s/Num
    :vegan? s/Bool}))

(abstract-map/extend-schema Cat Animal [:cat] {:fav-catnip s/Str})

(deftest abstract-map-test
  (let [k (atom [])]
    (sw/walk Animal (fn [x] (swap! k conj x) x) identity)
    (is (= [Cat {:age s/Num, :vegan? s/Bool}] @k)))
  (let [k (atom [])]
    (sw/walk Cat (fn [x] (swap! k conj x) x) identity)
    (is (= [Animal {:age s/Num, :vegan? s/Bool, :fav-catnip s/Str, :type (s/enum :cat)}] @k))))
