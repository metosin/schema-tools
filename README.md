# Schema-tools [![Build Status](https://travis-ci.org/metosin/schema-tools.png?branch=master)](https://travis-ci.org/metosin/schema-tools)

Common utilities for working with [Prismatic Schema](https://github.com/Prismatic/schema) Maps, both Clojure & ClojureScript.
* common Schema definitions: `any-keys`, `any-keyword-keys`
* Schema-aware selectors: `get-in`, `select-keys`, `select-schema`
* Schema-aware transformers: `assoc`, `dissoc`, `assoc-in`, `update-in`, `update`, `dissoc-in`, `merge`, `optional-keys`, `required-keys`
* Protocol-based walker for manipulating Schemas: `schema-tools.walk/walk`

[API Docs](http://metosin.github.io/schema-tools/schema-tools.core.html).

## Latest version

[![Clojars Project](http://clojars.org/metosin/schema-tools/latest-version.svg)](http://clojars.org/metosin/schema-tools)

## Examples

Comparison to normal `clojure.core` functions:

```clojure
(require '[schema.core :as s])

(s/defschema Address {:street s/Str
                      (s/optional-key :city) s/Str
                      (s/required-key :country) {:name s/Str}})

(select-keys Address [:street :city]) ; fail
; => {:street java.lang.String}

(require '[schema-tools.core :as st])

(st/select-keys Address [:street :city]) ; works
; => {:street java.lang.String, #schema.core.OptionalKey{:k :city} java.lang.String}
````

Filtering out extra keys (without validation errors):

```clojure
(st/select-schema Address {:street "Keskustori 8"
                           :city "Tampere"
                           :description "Metosin HQ" ; disallowed-key
                           :country {:weather "-18" ; disallowed-key
                                     :name "Finland"}})
; => {:city "Tampere", :street "Keskustori 8", :country {:name "Finland"}}
```

## Usage

See the [tests](https://github.com/metosin/schema-tools/blob/master/test/schema_tools/core_test.cljx).

## Todo
- [ ] `deep-merge`
- [ ] Verify performance
- [ ] Context-aware Schemas
- [ ] XSD Generation(?)
- [ ] Common predicates missing from the schema.core
- [ ] Separate package for common finnish predicates, `YTunnus`, `Hetu` etc.

## License

Copyright © 2014-2015 [Metosin Oy](http://www.metosin.fi)

Distributed under the Eclipse Public License, the same as Clojure.
