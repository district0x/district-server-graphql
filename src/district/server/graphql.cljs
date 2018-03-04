(ns district.server.graphql
  (:require
    [cljs.nodejs :as nodejs]
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

(defn stop [api-server]
  (.close (:server @api-server)))

(defn- error-middleware? [f]
  (when (fn? f)
    (= (aget f "length") 4)))

(defn- install-middlewares! [app middlewares]
  (doseq [middleware middlewares]
    (if (map? middleware)
      (.use app (:path middleware) (:middleware middleware))
      (.use app middleware))))

(defn start [{:keys [:port :middlewares :path] :as opts}]
  (let [app (express)
        middlewares (flatten middlewares)]
    (install-middlewares! app [{:path path :middleware (create-graphql-middleware opts)}])
    (install-middlewares! app (remove error-middleware? middlewares))
    (install-middlewares! app (filter error-middleware? middlewares))
    {:app app :server (.listen app port)}))

