(ns district.server.graphql.middleware
  (:require
    [camel-snake-kebab.core :as cs :include-macros true]
    [cljs.nodejs :as nodejs]))

(def graphql-module (nodejs/require "graphql"))
(def graphqlHTTP (nodejs/require "express-graphql"))
(def build-schema (aget graphql-module "buildSchema"))

(defn create-graphql-middleware [opts]
  (-> opts
    (update :schema #(if (string? %) (build-schema %) %))
    (->> (map (fn [[k v]] [(cs/->camelCaseString k) v])))
    (->> (into {}))
    clj->js
    graphqlHTTP))