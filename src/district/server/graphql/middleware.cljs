(ns district.server.graphql.middleware
  (:require
    [camel-snake-kebab.core :as cs :include-macros true]
    [cljs.nodejs :as nodejs]
    [district.graphql-utils :as graphql-utils]))

(def GraphQL (nodejs/require "graphql"))
(def ApolloServer (aget (nodejs/require "@apollo/server") "ApolloServer"))
(def express-middleware (aget (nodejs/require "@apollo/server/express4") "expressMiddleware"))
(def gql-build-schema (aget GraphQL "buildSchema"))

(defn build-schema [schema]
  (cond-> schema
    (string? schema) gql-build-schema
    true graphql-utils/add-keyword-type
    true graphql-utils/add-date-type))

(defn create-graphql-middleware [app path opts context-fn]
  (let [opts (-> opts
                 (update :schema build-schema)
                 (->> (map (fn [[k v]] [(cs/->camelCaseString k) v])))
                 (->> (into {}))
                 clj->js)
        server (new ApolloServer opts)]
    (.then (.start server)
           (fn []
             (let [middleware (express-middleware server (when (fn? context-fn) (clj->js {:context context-fn})))]
               (.use app path middleware))))))
