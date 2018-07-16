(ns orgpad.functions.base
  (:require
   [datascript.core :as d]
   [orgpad.ignite.manager :as im]))

(defn- exist-orgpad?
  [{:keys [params] :as cmd}]
  (-> cmd
      (assoc :reply :orgpad.server.reply/sender)
      (assoc :result (->> params
                          :orgpad.server/uuid
                          (.get (im/get-cache :orgpad.ignite.manager/global-orgpad-cache))
                          nil?
                          not))))

(defn- create-orgpad
  [{:keys [params] :as cmd}]

  (-> cmd
      (assoc :reply :orgpad.server.reply/sender)))

(defn do-cmd
  [params cmd]

  (case (:action cmd)
    :orgpad.server/exist-orgpad?
    (exist-orgpad? cmd)

    :orgpad.server/create-orgpad
    (create-orgpad cmd)

    cmd))
