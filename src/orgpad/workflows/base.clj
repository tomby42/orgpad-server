(ns orgpad.workflows.base
  (:require [duct.core :as core]
            [duct.core.merge :as merge]
            [integrant.core :as ig]))

(defmethod ig/init-key :orgpad.workflows/base
  [_ opts]
  [[:in :do-cmd]
   [:do-cmd :out]])
