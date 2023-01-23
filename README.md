# district-server-graphql

[![CircleCI](https://circleci.com/gh/district0x/district-server-graphql.svg?style=svg)](https://circleci.com/gh/district0x/district-server-graphql)

Clojurescript-node.js [mount](https://github.com/tolitius/mount) module for a district server, that sets up [GraphQL](http://graphql.org/) server.
It uses [expressjs](https://expressjs.com/) and [apollo-server](https://www.apollographql.com) to set up the server.

## Installation

[![Clojars Project](https://img.shields.io/clojars/v/district0x/district-server-graphql.svg)](https://clojars.org/district0x/district-server-graphql)

Include `[district.server.graphql]` in your CLJS file, where you use `mount/start`

## API Overview

**Warning:** district0x modules are still in early stages, therefore API can change in a future.

- [district.server.graphql](#districtservergraphql)
- [run-query](#run-query)
- [district.server.graphql.middleware](#districtservergraphqlmiddleware)
  - [create-graphql-middleware](#create-graphql-middleware)
- [district.server.graphql.utils](#districtservergraphqlutils)
  - [build-schema](#build-schema)

## Usage
You can pass following args to graphql module:
* `:port` Port at which HTTP server will start
* `:path` Path of GraphQL endpoint
* `:middlewares` Collection of expressjs [middlewares](http://expressjs.com/en/guide/using-middleware.html) you want to install.
See list of [district-server-middlewares](https://github.com/search?q=topic%3Adistrict-server-middleware+org%3Adistrict0x&type=Repositories).
* `:gql-name->kw` Function for converting GraphQL names into keywords. Default: [gql-name->kw](https://github.com/district0x/district-graphql-utils#gql-name-kw)
* `:kw->gql-name` Function for converting keywords into GraphQL names. Default: [kw->gql-name](https://github.com/district0x/district-graphql-utils#kw-gql-name)
* `:context-fn` Function to use to generate the context of the resolvers from the request 
* All [ApolloServer options](https://www.apollographql.com/docs/apollo-server/api/apollo-server) as kebab-cased keywords

```clojure
(ns my-district
    (:require [mount.core :as mount]
              [district.server.graphql]))

  (def schema "type Query { hello: String}")
  (def root {:hello (constantly "Hello world")})

  (-> (mount/with-args
        {:graphql {:port 6200
                   :path "/graphql"
                   :schema schema
                   :root-value root
                   :graphiql true}})
    (mount/start))
```

That's all you need to do to set up GraphQL server!

## Module dependencies
### [district-server-config](https://github.com/district0x/district-server-config)
`district-server-graphql` gets initial args from config provided by `district-server-config/config` under the key `:graphql`. These args are then merged together with ones passed to `mount/with-args`.

If you wish to use custom modules instead of dependencies above while still using `district-server-graphql`, you can easily do so by [mount's states swapping](https://github.com/tolitius/mount#swapping-states-with-states).

## district.server.graphql
This namespace contains mount module as well as some helper functions

#### <a name="run-query">`run-query [query]`
Will run GraphQL query. Transforms response from JS objects into CLJS data structures.
You can pass query string or [graphql-query](https://github.com/district0x/graphql-query) data structure.

```clojure
(run-query "{hello}")
;; => {:data {:hello "Hello world"}}

(run-query {:queries [:hello]})
;; => {:data {:hello "Hello world"}}
```

## district.server.graphql.middleware
This namespace contains function for creating GraphQL expressjs middleware

#### <a name="create-graphql-middleware">`create-graphql-middleware [opts]`
Creates expressjs graphql middleware. Pass same opt as you'd pass into [ApolloServer options](https://www.apollographql.com/docs/apollo-server/api/apollo-server).
For schema you can pass either string or built GraphQL object.

### <a name="districtservergraphqlutils"> district.server.graphql.utils

#### <a name="build-schema">`build-schema [schema-str resolvers-map {:keys [kw->gql-name gql-name->kw]}]`
Builds a GraphQLSchema from a schema string and a resolvers map.
- schema-str: A string containig a graphql schema definition.
- resolvers-map: A map like {:Type {:field1 resolver-fn}}.
- kw->gql-name: A fn for serializing keywords to gql names.
- gql-name->kw: A fn for parsing keywords from gql names.

```clojure
(let [schema "type Author {
                         id: ID!
                         firstName: String
                         lastName: String
                         posts: [Post]
                       }

                       type Post {
                         id: ID!
                         title: String
                         author: Author
                         votes: Int
                       }

                       type Query {
                         posts(minVotes: Int): [Post]
                       }

                       type Mutation {
                         upvotePost (postId: ID!): Post
                       }

                       schema {
                         query: Query
                         mutation: Mutation
                       }"
      resolvers {:Query {:posts (fn [obj {:keys [min-votes] :as args}])}
                 :Mutation {:upvote-post (fn [obj {:keys [post-id] :as args}])}
                 :Author {:posts (fn [{:keys [posts] :as author}])}
                 :Post {:author (fn [{:keys [author] :as post}])}}]

  (build-schema schema resolvers {:kw->gql-name kw->gql-name
                                  :gql-name->kw gql-name->kw}))
```

## Development
```bash
# To start REPL and run tests
lein deps
lein repl
(start-tests!)

# In other terminal
node tests-compiled/run-tests.js

# To run tests without REPL
lein doo node "nodejs-tests" once
```
