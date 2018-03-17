(ns schema-tools.swagger.core
  (:require [clojure.walk :as walk]))

;;
;; generate the swagger spec
;;

(defn swagger-spec
  "Transforms data into a swagger2 spec. WIP"
  ([x]
   (swagger-spec x nil))
  ([x options]
   (walk/postwalk
     (fn [x]
       (if (map? x)
         (dissoc x ::parameters ::responses)
         x))
     x)))

