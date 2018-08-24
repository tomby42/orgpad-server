(ns orgpad.tools.orgpad
  (:require
   [datascript.core :as d]
   [clojure.set :as set]))

;; Query determining local data in db
(def local-qry '[:find [(pull ?v [*]) ...]
                 :in $
                 :where
                 [?v :orgpad/type :orgpad/unit-view]])

;; TODO - move props-refs pointing to local entities to local db and remove them from global db
;;        atoms move to global db
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

(defn- is-local?
  [ldb local-ids id]
  (or (-> ldb (d/entity id) :orgpad/type)
      (contains? local-ids id)))

(defn split-global-local-update
  [ldb update-stream]
  (let [update-db (-> (d/empty-db {}) (d/with update-stream) :db-after)
        all-local-ids (->> update-db
                           (d/q '[:find [?v ...]
                                  :in $
                                  :where
                                  [?v :orgpad/type :orgpad/unit-view]])
                           set)
        atom-ids (->> update-db
                       (d/q '[:find [?v ...]
                              :in $
                              :where
                              [?v :orgpad/atom]])
                       set)
        local-ids (set/difference all-local-ids atom-ids)]

    (reduce (fn [[gupdate lupdate] datom]
              (if (or (is-local? ldb local-ids (.-e datom))
                      (and (= (.-a datom) :orgpad/props-refs)
                           (is-local? ldb local-ids (.-v datom))))
                [gupdate (conj lupdate datom)]
                [(conj gupdate datom) lupdate]))
            [[] []] update-stream)))

(comment
(require '[datascript.core :as d])
(require '[datascript.db :as db])
(require '[orgpad.tools.orgpad :as ot])
(def s [(db/datom 1 :orgpad/type :orgpad/unit) (db/datom 1 :orgpad/props-refs 2) (db/datom 1 :orgpad/props-refs 3)
        (db/datom 2 :orgpad/type :orgpad/unit-view)
        (db/datom 3 :orgpad/type :orgpad/unit-view) (db/datom 3 :orgpad/atom "a")])
(ot/split-global-local-update (d/empty-db {}) s)

(def d1 (-> (d/empty-db {}) (d/with s) :db-after))
)
