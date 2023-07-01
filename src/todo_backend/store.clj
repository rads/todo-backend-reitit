(ns todo-backend.store
  (:require [clojure.set :refer [rename-keys]]
            [next.jdbc :as jdbc]
            [next.jdbc.connection :as jdbc-connection]
            [next.jdbc.result-set :as rs]
            [next.jdbc.sql :as sql])
  (:import (com.zaxxer.hikari HikariDataSource)))

(defn get-connection-pool []
  (jdbc/with-options
    (jdbc-connection/->pool
      HikariDataSource
      {:jdbcUrl (System/getenv "JDBC_DATABASE_URL")})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn wrap-db [handler]
  (let [db (get-connection-pool)]
    (fn [req]
      (handler (assoc req :db db)))))

(defn as-row [row]
  (rename-keys row {:order :position}))

(defn as-todo [row]
  (rename-keys row {:position :order}))

(defn create-todos [db todo]
  (as-todo (sql/insert! db :todos (as-row todo))))

(defn get-todo [db id]
  (as-todo (sql/get-by-id db :todos id)))

(defn update-todo [db body id]
  (sql/update! db :todos (as-row body) {:id id})
  (get-todo db id))

(defn delete-todos [db id]
  (sql/delete! db :todos {:id id}))

(defn get-all-todos [db]
  (jdbc/execute! db ["SELECT * FROM todos;"]))

(defn delete-all-todos [db]
  (sql/delete! db :todos [true]))
