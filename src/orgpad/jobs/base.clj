(ns orgpad.jobs.base
  (:require [duct.core :as core]
            [duct.core.merge :as merge]
            [integrant.core :as ig]))

(defmethod ig/init-key :orgpad.jobs/base
  [_ {:keys [workflow catalog lifecycles flow-conditions task-scheduler]}]
  {:workflow (or workflow [])
   :catalog (or catalog [])
   :lifecycles (or lifecycles [])
   :flow-conditions (or flow-conditions [])
   :task-scheduler (or task-scheduler :onyx.task-scheduler/balanced)})
