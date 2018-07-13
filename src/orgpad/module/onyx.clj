(ns orgpad.module.onyx
  (:require [duct.core :as core]
            [duct.core.merge :as merge]
            [integrant.core :as ig]))

(defn- submit-job-config
  [config]
  {:orgpad.onyx.manager/submit-job
   {:logger (ig/ref :duct/logger)
    :config (ig/ref :orgpad.onyx.manager/setup)
    :jobs (ig/ref :orgpad.onyx.manager/jobs)}})

(def ^:private default-config
  {:orgpad.onyx.manager/jobs []
   :orgpad.onyx.manager/setup
   {:logger (ig/ref :duct/logger)
    :n-peers 10
    :fname (merge/displace "orgpad/onyx-config.edn")}})

(defmethod ig/init-key :orgpad.module/onyx [_ options]
  {:req #{:duct/logger}
   :fn (fn [config]
         (core/merge-configs config
                             (submit-job-config config)
                             default-config))})
