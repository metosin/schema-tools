#!/bin/bash
set -e
lein trampoline run -m clojure.main scripts/build.clj
node target/generated/js/out/tests.js
