# Schema-tools [![Build Status](https://travis-ci.org/metosin/schema-tools.png?branch=master)](https://travis-ci.org/metosin/schema-tools)

Common utilities on top of [Prismatic Schema](https://github.com/Prismatic/schema):
* common Schema definitions: `any-keys`, `any-keyword-keys`
* Schema-aware selectors: `get-in`, `select-keys`, `select-schema`
* Schema-aware transformers: `dissoc`, `assoc-in`, `update-in`, `dissoc-in`, `with-optional-keys`, `with-required-keys`

[API Docs](http://metosin.github.io/schema-tools/schema-tools.core.html).

## Latest version

[![Clojars Project](http://clojars.org/metosin/schema-tools/latest-version.svg)](http://clojars.org/metosin/schema-tools)

## Examples

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

```clojure
(st/select-schema Address {:street "Keskustori 8"
                           :city "Tampere"
                           :description "Metosin HQ" ; disallowed-key
                           :country {:weather "-18" ; disallowed-key
                                     :name "Finland"}})
; => {:street "Keskustori 8", :city "Tampere", :country {:name "Finland"}}
```

## Usage

See the [facts](https://github.com/metosin/schema-tools/blob/master/test/schema_tools/core_test.clj).

## Todo
- [ ] `update`, `merge`, `deep-merge`
- [ ] Verify performance
- [ ] Context-aware Schemas
- [ ] XSD Generation(?)
- [ ] Common predicates missing from the schema.core
- [ ] Separate package for common finnish predicates, `YTunnus`, `Hetu` etc.

## License

Copyright © 2014 Metosin Oy

Distributed under the Eclipse Public License, the same as Clojure.
