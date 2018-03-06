(ns district.server.graphql
  (:require
    [cljs.core.async :refer [<! chan put!]]
    [cljs.nodejs :as nodejs]
    [clojure.walk :as walk]
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
(def gql (aget graphql-module "graphql"))
(def build-schema (aget graphql-module "buildSchema"))

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


(defn- js-obj->clj [obj]
  (reduce (fn [acc key]
            (assoc acc (keyword key) (aget obj key)))
          {}
          (js->clj (js-keys obj))))


(defn- transform-result-vals [res]
  (walk/prewalk (fn [x]
                  (if (and (nil? (type x))
                           (seq (js-keys x)))
                    (js-obj->clj x)
                    (js->clj x)))
                (js->clj res :keywordize-keys true)))


(defn run-query
  ([query]
   (let [ch (chan)]
     (run-query query #(put! ch %))
     ch))
  ([query callback]
   (.then (gql (:schema @graphql) query (:root-value @graphql)) #(callback (transform-result-vals %)))))


(defn start [{:keys [:port :middlewares :path] :as opts}]
  (let [app (express)
        middlewares (flatten middlewares)
        opts (update opts :schema #(if (string? %) (build-schema %) %))]
    (install-middlewares! app [{:path path :middleware (create-graphql-middleware opts)}])
    (install-middlewares! app (remove error-middleware? middlewares))
    (install-middlewares! app (filter error-middleware? middlewares))
    {:app app
     :server (.listen app port)
     :schema (:schema opts)
     :root-value (clj->js (:root-value opts))}))

