(ns district.server.graphql.middleware
  (:require
    [camel-snake-kebab.core :as cs :include-macros true]
    [cljs.nodejs :as nodejs]
    [district.graphql-utils :as graphql-utils]))

;; (def GraphQL (nodejs/require "graphql"))
(def graphqlHTTP (nodejs/require "express-graphql"))
;; (def gql-build-schema (aget GraphQL "buildSchema"))
(def make-executable-schema (aget (nodejs/require "graphql-tools") "makeExecutableSchema"))

(defn build-schema [resolvers schema]
  (-> (make-executable-schema (js-obj "typeDefs" schema
                                      "resolvers" resolvers))
      graphql-utils/add-keyword-type
      graphql-utils/add-date-type))

(defn create-graphql-middleware [opts]
  (-> opts
      (->> (map (fn [[k v]] [(cs/->camelCaseString k) v])))
      (->> (into {}))
      clj->js
      graphqlHTTP))
