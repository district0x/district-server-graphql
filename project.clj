(defproject district0x/district-server-graphql "1.0.12"
  :description "district0x server module for setting up GraphQL server"
  :url "https://github.com/district0x/district-server-graphql"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[camel-snake-kebab "0.4.0"]
                 [district0x/district-graphql-utils "1.0.5"]
                 [district0x/district-server-config "1.0.1"]
                 [district0x/graphql-query "1.0.4"]
                 [mount "0.1.11"]
                 [org.clojure/clojurescript "1.10.238"]]

  :npm {:dependencies [[express "4.15.3"]
                       [cors "2.8.4"]
                       [express-graphql "0.6.12"]
                       [graphql "0.13.1"]]
        :devDependencies [[ws "2.0.1"]
                          [xhr2 "0.1.4"]]}

  :figwheel {:server-port 4682}

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.8.0"]
                                  [com.cemerick/piggieback "0.2.2"]
                                  [figwheel-sidecar "0.5.14"]
                                  [org.clojure/tools.nrepl "0.2.13"]
                                  [cljs-http "0.1.43"]]
                   :plugins [[lein-cljsbuild "1.1.7"]
                             [lein-figwheel "0.5.14"]
                             [lein-npm "0.6.2"]
                             [lein-doo "0.1.7"]]
                   :source-paths ["dev"]}}

  :cljsbuild {:builds [{:id "tests"
                        :source-paths ["src" "test"]
                        :figwheel {:on-jsload "tests.runner/-main"}
                        :compiler {:main "tests.runner"
                                   :output-to "tests-compiled/run-tests.js"
                                   :output-dir "tests-compiled"
                                   :target :nodejs
                                   :optimizations :none
                                   :source-map true}}]})
