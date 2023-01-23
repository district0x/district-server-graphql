(ns district.server.graphql.utils
  (:require [camel-snake-kebab.extras :refer [transform-keys]]
            [cljs.nodejs :as nodejs]
            [district.graphql-utils :refer [js->clj-objects]]))

(def make-executable-schema (aget (nodejs/require "@graphql-tools/schema") "makeExecutableSchema"))

(defn- build-resolvers
  "Given a map like {:Type {:field1 resolver-fn}}, a kw->gql-name and gql-name->kw,
  builds a resolvers map suitable for graphql-tools/make-executable-schema."
  [resolvers-map kw->gql-name gql-name->kw]
  (clj->js
   (reduce-kv (fn [r type-name fields-map]
                (assoc r (kw->gql-name type-name) 
                       (reduce-kv
                        (fn [fm field-name field-fn]
                          (assoc fm (kw->gql-name field-name)
                                 (fn [o args ctx info]
                                   (field-fn o
                                             (->> args
                                                  ; prevent issues with objects with null prototype
                                                  ; (https://github.com/graphql/graphql-js/pull/504)
                                                  (js->clj-objects)
                                                  (transform-keys gql-name->kw))
                                             ctx
                                             info))))
                        {}
                        fields-map)))
              {}
              resolvers-map)))
 
(defn build-default-field-resolver
  "Default resolver that tries to return a keyword property
  given a gql-name, assuming obj is a map"
  [gql-name->kw]
  (fn [obj _ _ info]
    (when (map? obj)
     (get obj (gql-name->kw (.-fieldName info))))))

(defn build-schema
  "schema-str: A string containig a graphql schema definition.
  resolvers-map: A map like {:Type {:field1 resolver-fn}}.
  kw->gql-name: A fn for serializing keywords to gql names.
  gql-name->kw: A fn for parsing keywords from gql names."
  [schema-str resolvers-map {:keys [kw->gql-name gql-name->kw]}]
  (make-executable-schema (js-obj "typeDefs" schema-str
                                  "resolvers" (build-resolvers resolvers-map
                                                               kw->gql-name
                                                               gql-name->kw))))
