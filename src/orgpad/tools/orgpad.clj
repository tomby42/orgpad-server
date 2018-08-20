(ns orgpad.tools.orgpad
  (:require
   [datascript.core :as d]))

;; Query determining local data in db
(def local-qry '[:find [(pull ?v [*]) ...]
                 :in $
                 :where
                 [?v :orgpad/type :orgpad/unit-view]])

(defn split-global-local-db
  "Returns [global local] part of db. Global part is common to all clients. Local
  is just for single client."
  [db]
  (let [local-entities (d/q local-qry db)
        global-db (-> db
                      (d/with
                       (into []
                             (map (fn [e] [:db.fn/retractEntity (:db/id e)]))
                             local-entities))
                      :db-after)
        local-db (-> db
                     :schema
                     d/empty-db
                     (d/with local-entities)
                     :db-after)]
    [global-db local-db]))

(defn merge-local-n-global-db
  "Merges global and local part of db"
  [gdb ldb]
  (-> gdb
      (d/with (seq ldb))
      :db-after))

(defn split-global-local-update
  [ldb update-stream]
  (let [update-db (-> (d/empty-db {}) (d/with update-stream) :db-after)
        local-ids (d/q '[:find ?v
                         :in $
                         :where
                         [?v :orgpad/type :orgpad/unit-view]] update-db)]
    (reduce (fn [[lupdate gupdate] datom]
              (if (or (d/entity ldb (.-e datom))
                      (contains? local-ids (.-e datom)))
                [(conj lupdate datom) gupdate]
                [lupdate (conj gupdate datom)]))
            [[] []] update-stream)))
