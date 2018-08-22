(ns orgpad.handler.com
  (:require [compojure.core :as compojure :refer :all]
            [integrant.core :as ig]
            [ring.middleware.params :as params]
            [ring.middleware.keyword-params :as kparams]
            [compojure.route :as route]
            [aleph.http :as http]
            [aleph.netty :as netty]
            [byte-streams :as bs]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [taoensso.timbre :as timbre :refer (tracef debugf infof warnf errorf error)]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.aleph :refer (get-sch-adapter)]
            [taoensso.sente.packers.transit :as sente-transit]
            [clojure.core.async :refer [chan <! >! tap untap close! put! timeout go go-loop]]
            [orgpad.gates.core :as gates]))

(defonce sente-server (atom nil))
(defonce orgpads->connections (atom {}))

;;;; Sente setup

(defn init-sente
  []
  (let [;; transit packing is a workaround for bug CLJ-1544 when
        ;; clojure.tools.reader.reder_types namespace is not compiled

        packer (sente-transit/get-transit-packer)
        ;; packer :edn ; Default packer, a good choice in most cases

        chsk-server
        (sente/make-channel-socket-server!
         (get-sch-adapter) {:packer packer
                            :user-id-fn (fn [req] (or (get-in req [:session :uid])
                                                      (str (java.util.UUID/randomUUID))))})

        {:keys [connected-uids]} chsk-server]

    ;; We can watch this atom for changes if we like
    (add-watch connected-uids :connected-uids
               (fn [_ _ old new]
                 (when (not= old new)
                   ;; TODO: when connections drops clean out orgpads->connections
                   (infof "Connected uids change: %s" new))))
    chsk-server))

;;;; Sente event handlers

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id ; Dispatch on event-id
  )

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg) ; Handle event-msgs on a single thread
  ;; (future (-event-msg-handler ev-msg)) ; Handle event-msgs on a thread pool
  )

(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (debugf "Unhandled event: %s" event)
    (when ?reply-fn
      (?reply-fn {:umatched-event-as-echoed-from-from-server event}))))

(defmethod -event-msg-handler
  :chsk/ws-ping [msg] nil)

(defmethod -event-msg-handler
  :chsk/uidport-open
  [{:as ev-msg, :keys [uid]}]
  (let [chsk-send! (:send-fn @sente-server)]
    ;; (chsk-send! uid [])
    ))

(def ^:private conjn (fnil conj #{}))

;; Command structure
;;
;; {:action #{:orgpad.server/connect-to-orgpad
;;            :orgpad.server/disconnect-from-orgpad
;;            :orgpad.server/create-orgpad
;;            :orgpad.server/exist-orgpad?
;;            :orgpad.server/update-orgpad}
;;
;;  :params {:orgpad.server/uuid str}}

(defmethod -event-msg-handler
  :orgpad.server/cmd
  [{:as ev-msg :keys [event uid ?data ring-req ?reply-fn send-fn]}]
  (case (:action ?data)
    :orgpad.server/connect-to-orgpad
    (swap! orgpads->connections update (get-in ?data [:params :orgpad.server/uuid]) conjn uid)
    :orgpad.server/disconnect-from-orgpad
    (swap! orgpads->connections update (get-in ?data [:params :orgpad.server/uuid]) disj uid)
    nil)

  (put! gates/default-input-channel (assoc ?data :sender uid)))

;;;; Sente event router (our `event-msg-handler` loop)

(defn stop-router! []
  (when-let [stop-fn (:sente-router_ @sente-server)]
    (stop-fn)))

(defn start-router! []
  (stop-router!)
  (swap! sente-server
         assoc
         :sente-router_
         (sente/start-server-chsk-router!
          (:ch-recv
           @sente-server) event-msg-handler)))

(defn start-repeater! []
  (let [chsk-send! (:send-fn @sente-server)]
    (swap! sente-server assoc :orgpad.server/repeater
           (go-loop []
             (let [cmd (<! gates/default-output-buffer)]
               (println "*************************")
               (println "Result cmd: " cmd)
               (println "orgpads->connections" @orgpads->connections)
               (println "*************************")
               (when cmd
                 (case (:reply cmd)
                   :orgpad.server.reply/all
                   (doseq [uid (-> @sente-server :connected-uids deref :any)]
                     (when (->> uid contains? (:exclude cmd) not)
                       (chsk-send! uid [:orgpad.server/response cmd])))
                   :orgpad.server.reply/orgpad-all
                   (doseq [uid (get @orgpads->connections (get-in cmd [:params :orgpad.server/uuid]))]
                     (when (->> uid (contains? (:exclude cmd)) not)
                       (chsk-send! uid [:orgpad.server/response cmd])))
                   :orgpad.server.reply/sender
                   (chsk-send! (:sender cmd) [:orgpad.server/response cmd])
                   nil)
                 (recur)))))))

(defn stop-repeater! []
  (when-let [ch (:orgpad.server/repeater @sente-server)]
    (close! ch)))

;;;; Duct setup

(defmethod ig/init-key :orgpad.handler/com [_ options]
  (let [cfg (init-sente)]
    (reset! sente-server cfg)
    (start-router!)
    (start-repeater!)
    (println ":orgpad.handler/com init")

    (-> (compojure/routes
         (GET "/com" [] (:ajax-get-or-ws-handshake-fn cfg))
         (POST "/com" [] (:ajax-post-fn cfg)))
        kparams/wrap-keyword-params
        params/wrap-params)))

(defmethod ig/init-key :orgpad.handber/ssl-conf [_ options]
  (netty/self-signed-ssl-context))

(comment
  ;; :ssl-context #ig/ref :orgpad.handler/ssl-conf

  (let [conn @(http/websocket-client "ws://localhost:3000/com")
        ]

  (s/put-all! conn
    (->> 10 range (map str)))

  (->> conn
    (s/transform (take 10))
    s/stream->seq
    doall))    ;=> ("0" "1" "2" "3" "4" "5" "6" "7" "8" "9")
  )
