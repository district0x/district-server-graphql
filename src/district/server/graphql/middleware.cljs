(ns district.server.graphql.middleware
  (:require
    [camel-snake-kebab.core :as cs :include-macros true]
    [cljs.nodejs :as nodejs]
    [district.graphql-utils :as graphql-utils]))

(def GraphQL (nodejs/require "graphql"))
(def graphqlHTTP (nodejs/require "express-graphql"))
(def gql-build-schema (aget GraphQL "buildSchema"))

(defn build-schema [schema]
  (cond-> schema
    (string? schema) gql-build-schema
    true graphql-utils/add-keyword-type
    true graphql-utils/add-date-type))

(defn create-graphql-middleware [opts]
  (-> opts
    (update :schema build-schema)
    (->> (map (fn [[k v]] [(cs/->camelCaseString k) v])))
    (->> (into {}))
    clj->js
    graphqlHTTP))