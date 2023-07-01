(ns todo-backend.handlers
  (:require [ring.util.response :as rr]
            [todo-backend.store :as store]))

(defn append-todo-url [todo request]
  (let [host (-> request :headers (get "host" "localhost"))
        scheme (name (:scheme request))
        id (:id todo)]
    (merge todo {:url (str scheme "://" host "/todos/" id)})))

(defn list-all-todos [{:keys [db] :as request}]
  (-> #(append-todo-url % request)
      (map (store/get-all-todos db))
      rr/response))

(defn create-todo [{:keys [db body-params] :as request}]
  (-> (store/create-todos db body-params)
      (append-todo-url request)
      rr/response))

(defn delete-all-todos [{:keys [db]}]
  (store/delete-all-todos db)
  (rr/status 204))

(defn retrieve-todo [{:keys [db parameters] :as request}]
  (let [id (-> parameters :path :id)]
    (-> (store/get-todo db id)
        (append-todo-url request)
        rr/response)))

(defn ok [body]
  {:status 200
   :body body})

(defn update-todo [{:keys [db parameters body-params] :as req}]
  (-> body-params
      (store/update-todo db (get-in parameters [:path :id]))
      (append-todo-url req)
      ok))

(defn remove-todo [{:keys [db parameters]}]
  (store/delete-todos db (get-in parameters [:path :id]))
  {:status 204})
