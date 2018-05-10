(ns district.server.graphql
  (:require [camel-snake-kebab.extras :refer [transform-keys]]
            [cljs.nodejs :as nodejs]
            [district.graphql-utils :as graphql-utils]
            [district.server.config :refer [config]]
            [district.server.graphql.middleware
             :refer
             [build-schema create-graphql-middleware]]
            [graphql-query.core :refer [graphql-query]]
            [mount.core :as mount :refer [defstate]]))

(declare start)
(declare stop)

(defstate graphql
  :start (start (merge (:graphql @config)
                       (:graphql (mount/args))))
  :stop (stop graphql))

(def express (nodejs/require "express"))
(def graphql-module (nodejs/require "graphql"))
(def gql-sync (aget graphql-module "graphqlSync"))
(def cors (nodejs/require "cors"))

(defn stop [graphql]
  (.close (:server @graphql)))


(defn- error-middleware? [f]
  (when (fn? f)
    (= (aget f "length") 4)))


(defn- install-middlewares! [app middlewares]
  (doseq [middleware middlewares]
    (if (map? middleware)
      (.use app (:path middleware) (:middleware middleware))
      (.use app middleware))))


(defn restart [opts]
  (let [opts (merge (:opts @graphql) opts)]
    (mount/stop #'district.server.graphql/graphql)
    (mount/start-with-args (merge (mount/args) {:graphql opts}) #'district.server.graphql/graphql)))


(defn run-query [query & [{:keys [:kw->gql-name :gql-name->kw]}]]
  (let [query (if-not (string? query)
                (graphql-query query {:kw->gql-name (or kw->gql-name
                                                        (:kw->gql-name (:opts @graphql)))})
                query)]
    (graphql-utils/js->clj-response (gql-sync (:schema @graphql)
                                              query
                                              (:root-value @graphql)
                                              nil nil nil
                                              (:field-resolver @graphql))
                                    {:gql-name->kw (or gql-name->kw
                                                       (:gql-name->kw (:opts @graphql)))})))

(defn- build-resolvers
  "Given a map like {:Type {:field1 resolver-fn}} and
  a kw->gql-name, build a resolvers map as required by graphql-tools."
  [resolvers-map kw->gql-name]
  (clj->js
   (reduce-kv (fn [r type-name fields-map]
                (assoc r (kw->gql-name type-name) 
                       (reduce-kv
                        (fn [fm field-name field-fn]
                          (assoc fm (kw->gql-name field-name)
                                 (fn [o args ctx info]
                                   (field-fn o
                                             (transform-keys kw->gql-name args)
                                             ctx
                                             info))))
                        {}
                        fields-map)))
              {}
              resolvers-map)))

(defn default-field-resolver
  "Default resolver that tries to return a keyword property
  given a gql-name, assuming obj is a map"
  [gql-name->kw obj _ _ info]
  (when (map? obj)
   (get obj (gql-name->kw (.-fieldName info)))))

(defn start [{:keys [:port :middlewares :path :kw->gql-name :gql-name->kw :resolvers :field-resolver] :as opts}]
  (let [app (express)
        middlewares (flatten middlewares)
        kw->gql-name (or kw->gql-name graphql-utils/kw->gql-name)
        gql-name->kw (or gql-name->kw graphql-utils/gql-name->kw)
        opts (cond-> opts
               true
               (update :schema (partial build-schema (build-resolvers resolvers kw->gql-name)))      

               (map? (:root-value opts))
               (update :root-value #(graphql-utils/clj->js-root-value % {:kw->gql-name kw->gql-name
                                                                         :gql-name->kw gql-name->kw}))

               true
               (merge {:kw->gql-name kw->gql-name :gql-name->kw gql-name->kw}))]
    (install-middlewares! app [(cors) {:path path :middleware (create-graphql-middleware opts)}])
    (install-middlewares! app (remove error-middleware? middlewares))
    (install-middlewares! app (filter error-middleware? middlewares))
    {:app app
     :server (.listen app port)
     :schema (:schema opts)
     :root-value (:root-value opts)
     :resolvers resolvers
     :field-resolver (or field-resolver (partial default-field-resolver gql-name->kw))
     :opts opts}))

