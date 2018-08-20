(ns orgpad.functions.base
  (:require
   [clojure.stacktrace :as st]
   [datascript.core :as d]
   [datascript.transit :as dt]
   [orgpad.ignite.manager :as im]
   [orgpad.tools.orgpad :as ot]))

(defn- exist-orgpad?
  [{:keys [params] :as cmd}]
  (assoc cmd
         :reply :orgpad.server.reply/sender
         :result (->> params
                      :orgpad.server/uuid
                      (.get (im/get-cache :orgpad.ignite.manager/global-orgpad-cache))
                      nil?
                      not)))

(defn get-caches
  []
  {:global-cache (im/get-cache :orgpad.ignite.manager/global-orgpad-cache)
   :local-cache (im/get-cache :orgpad.ignite.manager/local-orgpad-cache)
   :history-cache (im/get-cache :orgpad.ignite.manager/history-cache)})

(defn- create-orgpad
  [{:keys [params] :as cmd}]
  (let [{:keys [global-cache local-cache history-cache]} (get-caches)
        ouid (str (d/squuid))
        [gdb ldb] (ot/split-global-local-db (-> params :db dt/read-transit-str))]

    (.put global-cache ouid {:db gdb :history-counter 0})
    (.put local-cache (str ouid "/" (:sender cmd)) {:db ldb :atom (:atom params)})
    (.put history-cache (str ouid "/" 0) {:db (:db params)
                                          :atom (:atom params)
                                          :user (:sender cmd)})

    (println "creating orgpad uuid:" ouid "/" (:sender cmd))
    (println "From client:" (-> params :db dt/read-transit-str))
    (println "GDB:" gdb)
    (println "LDB:" ldb)
    (println "---------------")

    (assoc cmd
           :reply :orgpad.server.reply/sender
           :result {:orgpad.server/uuid ouid})))

(def empty-ldb {:db (d/empty-db {})
                :atom {:app-state {:mode :write}}})

(defn- connect-to-orgpad
  [{:keys [params] :as cmd}]
  (let [{:keys [global-cache local-cache history-cache]} (get-caches)
        ouid (:orgpad.server/uuid params)
        gdb (->> ouid (.get global-cache) :db)
        ldb (.get local-cache (str ouid "/" (:sender cmd)))
        ldb' (if (nil? ldb)
               (do
                 (.put local-cache (str ouid "/" (:sender cmd)) empty-ldb)
                 empty-ldb)
               ldb)]

    (assoc cmd
           :reply :orgpad.server.reply/sender
           :result {:db (-> gdb (ot/merge-local-n-global-db (:db ldb')) dt/write-transit-str)
                    :atom (:atom ldb')})))

(defn- update-orgpad
  [{:keys [params] :as cmd}]
  (println "update-orgpad: " cmd)
  (let [ouid (:orgpad.server/uuid params)
        {:keys [global-cache local-cache history-cache]} (get-caches)
        tx (im/get-tx!)]
    (try
      (let [global-info (.get global-cache ouid)
            ldb (.get local-cache (str ouid "/" (:sender cmd)))
            ldb' (if (nil? ldb) empty-ldb ldb)
            [update-gdb update-ldb] (ot/split-global-local-update (:db ldb')
                                                                  (-> params :db dt/read-transit-str))
            new-gdb (-> global-info :db (d/with update-gdb) :db-after)
            new-ldb (-> ldb' :db (d/with update-ldb) :db-after)
            new-atom (-> ldb' :atom (merge (:atom params)))
            cntr (-> global-info :history-counter inc)]

        (.put global-cache ouid {:db new-gdb :history-counter cntr})
        (.put local-cache (str ouid "/" (:sender cmd)) {:db new-ldb :atom new-atom})
        (.put history-cache (str ouid "/" cntr) {:db (:db params)
                                                 :atom (:atom params)
                                                 :user (:sender cmd)})

        (.commit tx)

        (println "updating orgpad uuid:" ouid "/" (:sender cmd))
        (println "From client db:" (-> params :db dt/read-transit-str))
        (println "From client atom:" (-> params :atom))
        (println "GDB:" new-gdb)
        (println "LDB:" new-ldb)
        (println "Atom:" new-atom)
        (println "history counter:" cntr)
        (println "---------------")

        (-> cmd
            (assoc
             :reply :orgpad.server.reply/orgpad-all
             :exclude #{(:sender cmd)}
             :result {:db (-> update-gdb dt/write-transit-str)})
            (dissoc :params)))
      (finally (.close tx)))))

(defn do-cmd
  [params cmd]

  (println "@@@@@@@@@@@@@@@@@@@@@@@@@")
  (println "Do cmd: " cmd)
  (println "@@@@@@@@@@@@@@@@@@@@@@@@@")

  (try
    (case (:action cmd)
      :orgpad.server/exist-orgpad?
      (exist-orgpad? cmd)

      :orgpad.server/create-orgpad
      (create-orgpad cmd)

      :orgpad.server/connect-to-orgpad
      (connect-to-orgpad cmd)

      :orgpad.server/update-orgpad
      (update-orgpad cmd)

      (assoc cmd
             :reply :orgpad.server.reply/sender
             :result :orgpad.server/error
             :error :orgpad.server/unknown-command))

    (catch Exception e
      (st/print-stack-trace e)
      (assoc cmd
             :reply :orgpad.server.reply/sender
             :error (.getMessage e)
             :result :orgpad.server/error))))
