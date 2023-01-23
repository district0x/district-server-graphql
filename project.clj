(defproject district0x/district-server-graphql "1.0.19-SNAPSHOT"
  :description "district0x server module for setting up GraphQL server"
  :url "https://github.com/district0x/district-server-graphql"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[camel-snake-kebab "0.4.0"]
                 [district0x/async-helpers "0.1.3"]
                 [district0x/district-graphql-utils "1.0.11"]
                 [district0x/district-server-config "1.0.1"]
                 [district0x/graphql-query "1.0.6"]
                 [mount "0.1.16"]
                 [org.clojure/clojurescript "1.10.520"]]

  :npm {:dependencies [[express "4.18.2"]
                       [cors "2.8.4"]
                       [graphql "16.6.0"]
                       ["@graphql-tools/schema" "9.0.13"]
                       ["@apollo/server" "4.3.0"]]
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

  :deploy-repositories [["snapshots" {:url "https://clojars.org/repo"
                                      :username :env/clojars_username
                                      :password :env/clojars_password
                                      :sign-releases false}]
                        ["releases"  {:url "https://clojars.org/repo"
                                      :username :env/clojars_username
                                      :password :env/clojars_password
                                      :sign-releases false}]]

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["deploy"]]

  :cljsbuild {:builds [{:id "nodejs-tests"
                        :source-paths ["src" "test"]
                        :figwheel {:on-jsload "tests.runner/-main"}
                        :compiler {:main "tests.runner"
                                   :output-to "tests-compiled/run-tests.js"
                                   :output-dir "tests-compiled"
                                   :target :nodejs
                                   :optimizations :none
                                   :source-map true}}]})
