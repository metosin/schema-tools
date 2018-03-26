#!/bin/bash
set -e
case $1 in
    cljs)
        lein test-cljs
        ;;
    clj)
        lein test-clj
        ;;
    *)
        echo "Please select [clj|cljs]"
        exit 1
        ;;
esac
