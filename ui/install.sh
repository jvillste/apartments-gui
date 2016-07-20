#!/bin/bash
lein clean
lein cljsbuild once min
cp resources/public/index.html ../server/resources/public/index.html
cp resources/public/js/compiled/app.js ../server/resources/public/js/compiled/app.js
cd ../server
lein uberjar
cp target/apartments-gui.server.jar ~/Dropbox/bin/
