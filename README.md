# Schema-tools [![Build Status](https://travis-ci.org/metosin/schema-tools.svg?branch=master)](https://travis-ci.org/metosin/schema-tools)

Common utilities for working with [Prismatic Schema](https://github.com/Prismatic/schema) Maps, both Clojure & ClojureScript.
* common Schema definitions: `any-keys`, `any-keyword-keys`
* schema-aware selectors: `get-in`, `select-keys`, `select-schema`
* schema-aware transformers: `assoc`, `dissoc`, `assoc-in`, `update-in`, `update`, `dissoc-in`, `merge`, `optional-keys`, `required-keys`
  * removes the schema name and ns if the schema (value) has changed.
* meta-data helpers: `schema-with-description` `schema-description`, `resolve-schema` (clj only), `resolve-schema-description` (clj only)
* coercion tools: `or-matcher`, `map-filter-matcher`
* Protocol-based walker for manipulating Schemas: `schema-tools.walk/walk`

[API Docs](http://metosin.github.io/schema-tools/schema-tools.core.html).

## Latest version

[![Clojars Project](http://clojars.org/metosin/schema-tools/latest-version.svg)](http://clojars.org/metosin/schema-tools)

## Examples

Normal `clojure.core` functions don't work well with Schemas:

```clojure
(require '[schema.core :as s])

(s/defschema Address {:street s/Str
                      (s/optional-key :city) s/Str
                      (s/required-key :country) {:name s/Str}})

;; where's my city?
(select-keys Address [:street :city])
; {:street java.lang.String}

; this should not return the original Schema name...
(s/schema-name (select-keys Address [:street :city]))
; Address
```

With schema-tools:

```clojure
(require '[schema-tools.core :as st])

(st/select-keys Address [:street :city])
; {:street java.lang.String, #schema.core.OptionalKey{:k :city} java.lang.String}

(s/schema-name (st/select-keys Address [:street :city]))
; nil
```

### select-schema

Filtering out illegal keys using coercion:

```clojure
(st/select-schema {:street "Keskustori 8"
                   :city "Tampere"
                   :description "Metosin HQ" ; disallowed-key
                   :country {:weather "-18" ; disallowed-key
                             :name "Finland"}}
                  Address)
; {:city "Tampere", :street "Keskustori 8", :country {:name "Finland"}}
```

Json-coercion with filtering out illegal keys in a single sweep:

```clojure
(s/defschema Beer {:beer (s/enum :ipa :apa)})

(def ipa {:beer "ipa" :taste "good"})

(st/select-schema ipa Beer)
; clojure.lang.ExceptionInfo: Value does not match schema: {:beer (not (#{:ipa :apa} "ipa"))}
;     data: {:type :schema.core/error,
;            :schema {:beer {:vs #{:ipa :apa}}},
;            :value {:beer "ipa", :taste "good"},
;            :error {:beer (not (#{:ipa :apa} "ipa"))}}
           
(require '[schema.coerce :as sc])

(st/select-schema ipa Beer sc/json-coercion-matcher)
; {:beer :ipa}
```

## Usage

See the [tests](https://github.com/metosin/schema-tools/tree/master/test/schema_tools).

## License

Copyright © 2014-2015 [Metosin Oy](http://www.metosin.fi)

Distributed under the Eclipse Public License, the same as Clojure.
