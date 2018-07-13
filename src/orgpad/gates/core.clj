(ns orgpad.gates.core
  (:require [clojure.core.async :refer [chan]]))

(def default-channel-size 1000)

(defonce ^:private channels (atom {}))
(defonce ^:private buffers (atom {}))

(defn get-channel
  [type id size]
  (if-let [ch (get-in @channels [type id])]
    ch
    (do (swap! channels assoc-in [type id] (chan size))
        (get-channel type id size))))

(defn get-buffer
  [type id]
   (if-let [id (get-in @buffers [type id])]
     id
     (do (swap! buffers assoc-in [type id] (atom {}))
         (get-buffer type id))))

(defn- makeid
  [type id]
  (keyword (name type) id))

(defn make-pair-channels-ids
  [[t1 t2]]
  (let [id (str (java.util.UUID/randomUUID))]
    [(makeid t1 id) (makeid t2 id)]))

(defn get-random-channels-pairs
  [[t1 t2]]
  (let [id (-> @channels t1 keys rand-nth)]
    [(get-in @channels [t1 id])
     (get-in @channels [t2 (makeid t2 (name id))])]))

(defn get-reg-channels-ids
  []
  (into {} (map (fn [[k v]] [k (keys v)])) @channels))

(def default-input-channel (get-channel :in :default default-channel-size))
(def default-input-buffer (get-buffer :in :default))
(def default-output-buffer (get-channel :out :default default-channel-size))
