(ns orgpad.lifecycles.base
  (:require [duct.core :as core]
            [duct.core.merge :as merge]
            [integrant.core :as ig]
            [orgpad.gates.core :as gates]))

(defn inject-do-cmd-state
  [{:keys [onyx.core/task-map onyx.core/fn-params] :as pipeline} lifecycle]
  (let [;; params (:orgpad.server/params task-map)
        params (:orgpad.server/ignite-caches lifecycle)]
    {:onyx.core/params [{:caches-keys
                         params}]}))


(defn inject-in-ch
  [event lifecycle]
  {:core.async/buffer (gates/get-buffer :in (:core.async/id lifecycle))
   :core.async/chan (gates/get-channel :in (:core.async/id lifecycle) (:core.async/size lifecycle))})

(defn inject-out-ch
  [event lifecycle]
  {:core.async/chan (gates/get-channel :out (:core.async/id lifecycle) (:core.async/size lifecycle))})

(def in-calls
  {:lifecycle/before-task-start inject-in-ch})

(def out-calls
  {:lifecycle/before-task-start inject-out-ch})

(def do-cmd-calls
  {:lifecycle/before-task-start inject-do-cmd-state})

(defmethod ig/init-key :orgpad.lifecycles/base
  [_ {:keys [channel-name channel-size caches]}]
  (let [channel-name (or channel-name :default)]
    [{:lifecycle/task :in
      :core.async/id channel-name
      :core.async/size (or channel-size gates/default-channel-size)
      :lifecycle/calls ::in-calls}
     {:lifecycle/task :in
      :lifecycle/calls :onyx.plugin.core-async/reader-calls}
     {:lifecycle/task :out
      :lifecycle/calls ::out-calls
      :core.async/id channel-name
      :core.async/size (inc (or channel-size gates/default-channel-size))
      :lifecycle/doc "Lifecycle for writing to a core.async chan"}
     {:lifecycle/task :out
      :lifecycle/calls :onyx.plugin.core-async/writer-calls}
     {:lifecycle/task :do-cmd
      :lifecycle/calls ::do-cmd-calls
      :orgpad.server/ignite-caches (keys caches)}]))
