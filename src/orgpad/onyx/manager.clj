(ns orgpad.onyx.manager
  (:require [duct.core :as core]
            [duct.core.merge :as merge]
            [duct.logger :as logger]
            [integrant.core :as ig]
            [onyx.plugin.core-async]
            [onyx.api]
            [onyx.job]
            [lib-onyx.peer :as peer]
            [aero.core :refer [read-config]]
            [clojure.java.io :refer [resource]]))

(defmethod ig/init-key ::setup
  [_ {:keys [fname logger n-peers]}]
  (let [config (-> fname resource read-config)
        env (onyx.api/start-env (:env-config config))
        peer-group (onyx.api/start-peer-group (:peer-config config))
        peers (onyx.api/start-peers n-peers peer-group)]
    (logger/log logger :info ::setup.init-key config)
    {:env env
     :peer-group peer-group
     :peers peers
     :onyx-config config
     :logger logger}))

(defmethod ig/halt-key! ::setup
  [_ {:keys [peers peer-group env logger]}]
  (logger/log logger :info ::setup.halt-key!)

  (doseq [v-peer peers]
    (onyx.api/shutdown-peer v-peer))

  (onyx.api/shutdown-peer-group peer-group)
  (onyx.api/shutdown-env env))

(defmethod ig/init-key ::submit-job
  [_ {:keys [config logger jobs]}]
  (logger/log logger :info ::submit-job.init-key {:jobs jobs :config config})
  {:jobs-info
   (doall (map #(onyx.api/submit-job (-> config :onyx-config :peer-config) %) jobs))
   :onyx-config (:onyx-config config)
   :logger logger})

(defmethod ig/halt-key! ::submit-job
  [_ {:keys [jobs-info logger onyx-config]}]
  (logger/log logger :info ::submit-job.halt-key! jobs-info)
  (doseq [job-info jobs-info]
    (onyx.api/kill-job (:peer-config onyx-config) (:job-id job-info))))

(defmethod ig/init-key ::jobs
  [_ jobs]
  jobs)
