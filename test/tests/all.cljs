(ns tests.all
  (:require
    [cljs-http.client :as client]
    [cljs.core.async :refer [<!]]
    [cljs.nodejs :as nodejs]
    [cljs.test :refer-macros [deftest is testing use-fixtures async]]
    [district.server.graphql]
    [mount.core :as mount])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(set! js/XMLHttpRequest (nodejs/require "xhr2"))

(use-fixtures
  :each
  {:after
   (fn []
     (mount/stop))})

(deftest tests
  (async done
    (let [schema "type Query { hello: String}"
          root {:hello (constantly "Hello world")}]

      (-> (mount/with-args
            {:graphql {:port 6333
                       :path "/graphql"
                       :schema schema
                       :root-value root
                       :graphiql true}})
        (mount/start)))

    (go
      (is (= (-> (<! (client/post "http://localhost:6333/graphql"
                                  {:json-params {:query "{hello}"}}))
               :body
               :data)
             {:hello "Hello world"}))
      (done))))

