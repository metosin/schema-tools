try {
    require("source-map-support").install();
} catch(err) {
}
require("../target/generated/js/out/goog/bootstrap/nodejs.js");
require("../target/generated/js/out/tests.js");
goog.require("schema_tools.runner");
goog.require("cljs.nodejscli");
