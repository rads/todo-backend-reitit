(ns todo-backend.core
  (:require [muuntaja.core :as m]
            [reitit.coercion.schema :as rcs]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as rrc]
            [reitit.ring.middleware.muuntaja :as rrmm]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.cors :refer [wrap-cors]]
            [schema.core :as s]
            [todo-backend.handlers :as todo]
            [todo-backend.migration :refer [migrate]]
            [todo-backend.store :as store]))

(defn append-todo-url [todo request]
  (let [host (-> request :headers (get "host" "localhost"))
        scheme (name (:scheme request))
        id (:id todo)]
    (merge todo {:url (str scheme "://" host "/todos/" id)})))

(def router
  (ring/router
    [["/swagger.json" {:get
                       {:no-doc  true
                        :swagger
                        {:basePath "/"
                         :info     {:title       "Todo-Backend API"
                                    :description "This is a implementation of the Todo-Backend API REST, using Clojure, Ring/Reitit and next-jdbc."
                                    :version     "1.0.0"}}
                        :handler (swagger/create-swagger-handler)}}]
     ["/todos" {:get     {:summary "Retrieves the collection of Todo resources."
                          :handler todo/list-all-todos}
                :post    {:summary "Creates a Todo resource."
                          :handler todo/create-todo}
                :delete  {:summary "Removes all Todo resources"
                          :handler todo/delete-all-todos}
                :options (fn [_] {:status 200})}]
     ["/todos/:id" {:parameters {:path {:id s/Int}}
                    :get        {:summary "Retrieves a Todo resource."
                                 :handler todo/retrieve-todo}
                    :patch      {:summary "Updates the Todo resource."
                                 :handler todo/update-todo}
                    :delete     {:summary "Removes the Todo resource."
                                 :handler todo/remove-todo}}]]
    {:data {:muuntaja   m/instance
            :coercion   rcs/coercion
            :middleware [rrmm/format-middleware
                         rrc/coerce-exceptions-middleware
                         rrc/coerce-response-middleware
                         rrc/coerce-request-middleware
                         [wrap-cors :access-control-allow-origin  #".*"
                          :access-control-allow-methods [:get :put :post :patch :delete]]]}}))

(def app-routes
  (ring/ring-handler
   router
   (ring/routes
    (swagger-ui/create-swagger-ui-handler {:path "/"})
    (ring/create-default-handler
      {:not-found (constantly {:status 404 :body "Not found"})}))
   {:middleware [store/wrap-db]}))

(defn -main [port]
  (migrate)
  (jetty/run-jetty #'app-routes {:port (Integer. port)
                                 :join? false}))

(comment
  (def server (jetty/run-jetty #'app-routes {:port 3000
                                             :join? false})))
