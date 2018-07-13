(ns orgpad.module.ignite
  (:require [duct.core :as core]
            [duct.core.merge :as merge]
            [duct.logger :as logger]
            [integrant.core :as ig])
  (:import org.apache.ignite.Ignition)
  (:gen-class))

(def ^:private default-config
  {:orgpad.ignite.manager/setup
   {:logger (ig/ref :duct/logger)
    :config-file (merge/displace "orgpad/ignite-config.xml")}

   :orgpad.ignite.manager/caches [(ig/ref :orgpad.ignite.manager/global-orgpad-cache)
                                  (ig/ref :orgpad.ignite.manager/local-orgpad-cache)
                                  (ig/ref :orgpad.ignite.manager/history-cache)]

   :orgpad.ignite.manager/history-cache
   {:name "HistoryCache"
    :config (ig/ref :orgpad.ignite.manager/setup)}

   :orgpad.ignite.manager/global-orgpad-cache
   {:name "GlobalCache"
    :config (ig/ref :orgpad.ignite.manager/setup)}

   :orgpad.ignite.manager/local-orgpad-cache
   {:name "LocalCache"
    :config (ig/ref :orgpad.ignite.manager/setup)}
   })

(defmethod ig/init-key :orgpad.module/ignite [_ options]
  {:req #{:duct/logger}
   :fn (fn [config]
         (core/merge-configs config
                             default-config))})
