(ns orgpad.ignite.manager
  (:require [duct.core :as core]
            [duct.core.merge :as merge]
            [duct.logger :as logger]
            [integrant.core :as ig])
  (:import org.apache.ignite.Ignition
           org.apache.ignite.configuration.CacheConfiguration
           org.apache.ignite.cache.CacheAtomicityMode
           org.apache.ignite.cache.CacheWriteSynchronizationMode)
  (:gen-class))

(defonce caches (atom {}))

(defmethod ig/init-key ::setup
  [_ {:keys [logger config-file] :as params}]
  (let [ignite (Ignition/start config-file)]
    (.active ignite true)
    (logger/log logger :info ::setup.init-key config-file)
    (merge params
           {:ignite ignite})))

(defn- mk-cache
  [k {:keys [name config] :as params}]
  (let [cache-cfg (CacheConfiguration. name)
        _ (doto cache-cfg
            (.setAtomicityMode CacheAtomicityMode/TRANSACTIONAL)
            (.setBackups 1)
            (.setWriteSynchronizationMode CacheWriteSynchronizationMode/FULL_SYNC))
        cache (.getOrCreateCache (:ignite config) cache-cfg)]
    (logger/log (:logger config) :info (str ":orgpad.ignite.manager/" k ".init-key") name)
    (swap! caches assoc k cache)
    (merge params {:cache cache})))

(defmethod ig/init-key ::global-orgpad-cache
  [k params]
  (mk-cache k params))

(defmethod ig/init-key ::local-orgpad-cache
  [k params]
  (mk-cache k params))

(defmethod ig/init-key ::history-cache
  [k params]
  (mk-cache k params))

(defmethod ig/init-key ::caches
  [_ caches]
  (into {} (map (juxt :name :cache)) caches))

(defn get-cache
  [key]
  (get @caches key))
