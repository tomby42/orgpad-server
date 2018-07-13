(ns orgpad.main
  (:gen-class)
  (:require [clojure.java.io :as io]
            [duct.core :as duct]

            [orgpad.catalogs.base]
            [orgpad.functions.base]
            [orgpad.gates.core]
            [orgpad.handler.com]
            [orgpad.jobs.base]
            [orgpad.lifecycles.base]
            [orgpad.module.onyx]
            [orgpad.module.ignite]
            [orgpad.onyx.manager]
            [orgpad.ignite.manager]
            [orgpad.workflows.base]))

(duct/load-hierarchy)

(defn -main [& args]
  (let [keys (or (duct/parse-keys args) [:duct/daemon])]
    (-> (duct/read-config (io/resource "orgpad/config.edn"))
        (duct/prep keys)
        (duct/exec keys))))
