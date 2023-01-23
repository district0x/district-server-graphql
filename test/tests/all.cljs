(ns tests.all
  (:require
   [cljs-http.client :as client]
   [cljs.core.async :refer [<!]]
   [cljs.nodejs :as nodejs]
   [cljs.test :refer-macros [deftest is testing use-fixtures async]]
   [district.server.graphql :refer [run-query]]
   [mount.core :as mount]
   [district.graphql-utils :as graphql-utils]
   [district.server.graphql.utils :as utils]
   [graphql-query.core :refer [graphql-query]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(set! js/XMLHttpRequest (nodejs/require "xhr2"))

(use-fixtures
  :each
  {:after
   (fn []
     (mount/stop))})

(deftest tests
  (async done
    (let [schema "
          type Query {
            search: [Item]
          }
          type Item {
            title: String
          }"
          root {:search (constantly [{:title "abc"}])}]

      (-> (mount/with-args
            {:graphql {:port 6333
                       :path "/graphql"
                       :schema schema
                       :root-value root
                       :graphiql true}})
        (mount/start))

      (is (:data (run-query {:queries [[:search [:title]]]}))
          {:search [{:title "abc"}]})

      (is (:data (run-query "{search {title}}"))
          {:search [{:title "abc"}]})

      (go
        (is (= (-> (<! (client/post "http://localhost:6333/graphql"
                                    {:json-params {:query "{search {title}}"}}))
                 :body
                 :data)
               {:search [{:title "abc"}]}))
        (done)))))

(deftest context-tests
  (async done
    (let [resolvers-map {:Query {:search
                                 (fn [_ _ ctx _]
                                   [{:title (:result ctx)}])}}
          schema (utils/build-schema "
          type Query {
            search: [Item]
          }
          type Item {
            title: String
          }" resolvers-map {:kw->gql-name identity :gql-name->kw identity})
          context-fn (fn [] {:result "abc"})
          field-resolver (utils/build-default-field-resolver graphql-utils/gql-name->kw)]

      (-> (mount/with-args
            {:graphql {:port 6333
                       :path "/graphql"
                       :schema schema
                       :field-resolver field-resolver
                       :context-fn context-fn
                       :graphiql true}})
        (mount/start))

      (go
        (is (= (-> (<! (client/post "http://localhost:6333/graphql"
                                    {:json-params {:query "{search {title}}"}}))
                 :body
                 :data)
               {:search [{:title "abc"}]}))
        (done)))))

;; https://github.com/apollographql/graphql-tools/
(deftest graphql-tools-site-test
  (let [all-posts (atom [{:id "P1" :title "Post1" :author "A1" :votes 0}
                         {:id "P2" :title "Post2" :author "A1" :votes 5}
                         {:id "P3" :title "Post3" :author "A1" :votes 10}])
        all-authors [{:id "A1" :first-name "FN1" :last-name "LN1" :posts ["P1" "P2" "P3"]}]
        schema "type Author {
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
        resolvers {:Query {:posts (fn [obj {:keys [min-votes] :as args}]
                                    (filter #(> (:votes %) min-votes) @all-posts))}
                   :Mutation {:upvote-post (fn [obj {:keys [post-id] :as args}]
                                             (swap! @all-posts
                                                    #(mapv (fn [p]
                                                             (if (= (:id p) post-id)
                                                               (update p :votes inc)
                                                               p))
                                                           %)))}
                   :Author {:posts (fn [{:keys [posts] :as author}]
                                     (filter (set posts) @all-posts))
                            :id #(:id %)
                            :first-name #(:first-name %)}
                   :Post {:author (fn [{:keys [author] :as post}]
                                    (first (filter #(= (:id %) author) all-authors)))
                          :id #(:id %)
                          :title #(:title %)
                          :votes #(:votes %)}}]

    (-> (mount/with-args 
          {:graphql {:port 6333
                     :path "/graphql"
                     :schema (utils/build-schema schema
                                                 resolvers
                                                 {:kw->gql-name graphql-utils/kw->gql-name
                                                  :gql-name->kw graphql-utils/gql-name->kw})
                     :graphiql true}})
        (mount/start))
    (let [q1 (graphql-query {:queries [[:posts {:min-votes 4}
                                        [:id :title :votes [:author [:id :first-name]]]]]}
                            {:kw->gql-name graphql-utils/kw->gql-name})] 
 
      (testing "Query should work if called with run-query"
        (is (= (get-in (run-query q1) [:data :posts])
               [{:id "P2" :title "Post2" :author {:id "A1" :first-name "FN1"} :votes 5}
                {:id "P3" :title "Post3" :author {:id "A1" :first-name "FN1"} :votes 10}])))
  
      (async done
             (go
               (testing "Query should work if called from HTTP"
                 (let [post-result (<! (client/post "http://localhost:6333/graphql"
                                                    {:json-params {:query q1}}))]
                   (is (= (-> post-result
                              :body
                              :data
                              :posts)
                          [{:id "P2" :title "Post2" :author {:id "A1" :firstName "FN1"} :votes 5}
                           {:id "P3" :title "Post3" :author {:id "A1" :firstName "FN1"} :votes 10}]))))
               (done))))))
