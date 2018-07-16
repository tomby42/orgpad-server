(ns orgpad.catalogs.base
  (:require [duct.core :as core]
            [duct.core.merge :as merge]
            [integrant.core :as ig]))

(defmethod ig/init-key :orgpad.catalogs/base
  [_ {:keys [batch-timeout batch-size logger]}]
  [{:onyx/name :in
    :onyx/plugin :onyx.plugin.core-async/input
    :onyx/type :input
    :onyx/medium :core.async
    :onyx/max-peers 1
    :onyx/batch-timeout batch-timeout
    :onyx/batch-size batch-size
    :onyx/doc "Reads segments from a core.async channel"}

   {:onyx/name :do-cmd
    :onyx/fn :orgpad.functions.base/do-cmd
    :onyx/type :function
    :onyx/batch-timeout batch-timeout
    :onyx/batch-size batch-size
    ;; :orgpad.server/params {:caches (keys caches)}
    }

   {:onyx/name :out
    :onyx/plugin :onyx.plugin.core-async/output
    :onyx/type :output
    :onyx/medium :core.async
    :onyx/max-peers 1
    :onyx/batch-timeout batch-timeout
    :onyx/batch-size batch-size
    :onyx/doc "Writes segments to a core.async channel"}])
