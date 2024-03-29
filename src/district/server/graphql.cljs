(ns district.server.graphql
  (:require [cljs.nodejs :as nodejs]
            [district.graphql-utils :as graphql-utils]
            [district.server.config :refer [config]]
            [district.server.graphql.middleware :refer [build-schema create-graphql-middleware]]
            [district.shared.async-helpers :refer [promise->]]
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
(def gql-async (aget graphql-module "graphql"))
(def cors (nodejs/require "cors"))
(def body-parser (nodejs/require "body-parser"))

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
    (graphql-utils/js->clj-response (gql-sync #js {:schema (:schema @graphql)
                                                   :source query
                                                   :rootValue (:root-value @graphql)
                                                   :fieldResolver (:field-resolver @graphql) })
                                    {:gql-name->kw (or gql-name->kw
                                                       (:gql-name->kw (:opts @graphql)))})))

(defn run-query-async [query & [{:keys [:kw->gql-name :gql-name->kw]}]]
  (let [query (if-not (string? query)
                (graphql-query query {:kw->gql-name (or kw->gql-name
                                                        (:kw->gql-name (:opts @graphql)))})
                query)]
    (promise-> (gql-async #js {:schema (:schema @graphql)
                               :source query
                               :rootValue (:root-value @graphql)
                               :fieldResolver (:field-resolver @graphql)})
               (fn [data]
                 (graphql-utils/js->clj-response data
                                                 {:gql-name->kw (or gql-name->kw
                                                                    (:gql-name->kw (:opts @graphql)))})))))

(defn start [{:keys [:port :middlewares :path :kw->gql-name :gql-name->kw :context-fn] :as opts}]
  (let [app (express)
        middlewares (flatten middlewares)
        kw->gql-name (or kw->gql-name graphql-utils/kw->gql-name)
        gql-name->kw (or gql-name->kw graphql-utils/gql-name->kw)
        opts (cond-> opts
               true
               (update :schema build-schema)

               (map? (:root-value opts))
               (update :root-value #(graphql-utils/clj->js-root-value % {:kw->gql-name kw->gql-name
                                                                         :gql-name->kw gql-name->kw}))

               true
               (merge {:kw->gql-name kw->gql-name :gql-name->kw gql-name->kw}))]
    (install-middlewares! app (remove error-middleware? middlewares))
    (install-middlewares! app (filter error-middleware? middlewares))
    (install-middlewares! app [(cors) (.json body-parser)])
    (create-graphql-middleware app path opts context-fn)

    {:app app
     :server (.listen app port)
     :schema (:schema opts)
     :root-value (:root-value opts)
     :field-resolver (:field-resolver opts)
     :opts opts}))
