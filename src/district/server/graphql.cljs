(ns district.server.graphql
  (:require
    [cljs.core.async :refer [<! chan put!]]
    [cljs.nodejs :as nodejs]
    [district.graphql-utls :as graphql-utils]
    [district.server.config :refer [config]]
    [district.server.graphql.middleware :refer [create-graphql-middleware]]
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
(def build-schema (aget graphql-module "buildSchema"))
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
    (mount/start-with-args {:graphql opts} #'district.server.graphql/graphql)))


(defn run-query [query]
  (graphql-utils/js->clj-response (gql-sync (:schema @graphql) query (:root-value @graphql))))


(defn start [{:keys [:port :middlewares :path] :as opts}]
  (let [app (express)
        middlewares (flatten middlewares)
        opts (cond-> opts
               (string? (:schema opts))
               (update :schema build-schema)

               (map? (:root-value opts))
               (update :root-value graphql-utils/clj->js-root-value))]
    (install-middlewares! app [(cors) {:path path :middleware (create-graphql-middleware opts)}])
    (install-middlewares! app (remove error-middleware? middlewares))
    (install-middlewares! app (filter error-middleware? middlewares))
    {:app app
     :server (.listen app port)
     :schema (:schema opts)
     :root-value (:root-value (:root-value opts))
     :opts opts}))

