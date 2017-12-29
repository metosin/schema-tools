# Schema-tools [![Build Status](https://travis-ci.org/metosin/schema-tools.svg?branch=master)](https://travis-ci.org/metosin/schema-tools) [![Downloads](https://jarkeeper.com/metosin/schema-tools/downloads.svg)](https://jarkeeper.com/metosin/schema-tools) [![Dependencies Status](https://jarkeeper.com/metosin/schema-tools/status.svg)](https://jarkeeper.com/metosin/schema-tools)

Common utilities for working with [Prismatic Schema](https://github.com/Prismatic/schema) Maps, both Clojure & ClojureScript.
* common Schema definitions: `any-keys`, `any-keyword-keys`, `open-schema`
* schema-aware selectors: `get-in`, `select-keys`, `select-schema`
* schema-aware transformers: `assoc`, `dissoc`, `assoc-in`, `update-in`, `update`, `dissoc-in`, `merge`, `optional-keys`, `required-keys`
  * removes the schema name and ns if the schema (value) has changed.
* handle schema default values via `default` & `default-coercion-matcher`
* meta-data helpers: `schema-with-description` `schema-description`, `resolve-schema` (clj only), `resolve-schema-description` (clj only)
* coercion tools: `or-matcher`, `map-filter-matcher`, `multi-matcher`, `coercer`, `coerce`
* Protocol-based walker for manipulating Schemas in `schema-tools.walk`: `walk`, `prewalk` and `postwalk`.

[API Docs](http://metosin.github.io/schema-tools/schema-tools.core.html).

## Latest version

[![Clojars Project](http://clojars.org/metosin/schema-tools/latest-version.svg)](http://clojars.org/metosin/schema-tools)

Requires Java 1.8.

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

### Coercion

If a given value can't be coerced to match a schema, ex-info is thrown (like `schema.core/validate`):

```clojure
(require '[schema-tools.coerce :as stc])

(def matcher (constantly nil))
(def coercer (stc/coercer String matcher))

(coercer 123)
; clojure.lang.ExceptionInfo: Could not coerce value to schema: (not (instance? java.lang.String 123))
;      error: (not (instance? java.lang.String 123))
;     schema: java.lang.String
;       type: :schema-tools.coerce/error
;      value: 123

(coercer "123")
; "123"

; same behavior with coerce (creates coercer on each invocation, slower)
(stc/coerce 123 String matcher)
(stc/coerce "123" String matcher)
```

Coercion error `:type` can be overridden in both cases with an extra argument.

```clojure
(stc/coerce 123 String matcher :domain/horror)
; clojure.lang.ExceptionInfo: Could not coerce value to schema: (not (instance? java.lang.String 123))
;      error: (not (instance? java.lang.String 123))
;     schema: java.lang.String
;       type: :domain/horror
;      value: 123
```

### Select Schema

Filtering out illegal schema keys (using coercion):

```clojure
(st/select-schema {:street "Keskustori 8"
                   :city "Tampere"
                   :description "Metosin HQ" ; disallowed-key
                   :country {:weather "-18" ; disallowed-key
                             :name "Finland"}}
                  Address)
; {:city "Tampere", :street "Keskustori 8", :country {:name "Finland"}}
```

Filtering out illegal schema map keys using coercion with additional Json-coercion - in a single sweep:

```clojure
(s/defschema Beer {:beer (s/enum :ipa :apa)})

(def ipa {:beer "ipa" :taste "good"})

(st/select-schema ipa Beer)
; clojure.lang.ExceptionInfo: Could not coerce value to schema: {:beer (not (#{:ipa :apa} "ipa"))}
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

Copyright © 2014-2017 [Metosin Oy](http://www.metosin.fi)

Distributed under the Eclipse Public License, the same as Clojure.
