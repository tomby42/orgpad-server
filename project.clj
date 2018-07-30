(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
 "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(defproject orgpad "0.1.0-SNAPSHOT"
  :description "Orgpad server"
  :url "http://orgpad.org"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.async "0.4.474"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/data.avl "0.0.17"]
                 [cheshire "5.8.0"]
                 [compojure "1.6.1"]
                 [ring/ring-core "1.6.2"]
                 [ring/ring-devel "1.6.2"]
                 [aleph "0.4.6"]
                 [pandect "0.6.1"]
                 [base64-clj "0.1.1"]
                 [com.rpl/specter "1.1.1"]
                 [io.netty/netty-all "4.1.25.Final"]
                 [io.netty/netty-tcnative-boringssl-static "2.0.11.Final"]
                 [io.netty/netty-tcnative "2.0.11.Final"]
                 [com.taoensso/timbre "4.10.0"]
                 [org.onyxplatform/onyx "0.13.0"]
                 [org.onyxplatform/lib-onyx "0.13.0.0"]
                 [aero "1.1.3"]
                 [duct/core "0.6.2"]
                 [duct/module.logging "0.3.1"]
                 [duct/module.web "0.6.4"]
                 [duct/server.http.aleph "0.1.2"]

                 [org.apache.ignite/ignite-core "2.5.2"]
                 [org.apache.ignite/ignite-spring "2.5.2"]

                 [datascript "0.16.6"]
                 [com.taoensso/sente "1.12.0"]
                 [com.cognitect/transit-clj "0.8.309"]
                 [datascript-transit "0.2.2"
                  :exclusions [com.cognitect/transit-clj
                               com.cognitect/transit-cljs]]
                 ]

  :repositories [["ignite.extra" "http://www.gridgainsystems.com/nexus/content/repositories/external"]]

  :plugins [[duct/lein-duct "0.10.6"]]
  :hooks []

  :main ^:skip-aot orgpad.main
  :resource-paths ["resources" "target/resources"]
  :prep-tasks     ["javac" "compile" ["run" ":duct/compiler"]]
  :uberjar-name "orgpad-standalone.jar"
  :profiles {:dev  {:jvm-opts ["-XX:-OmitStackTraceInFastThrow"]
                    :global-vars {*assert* true}
                    :source-paths   ["dev/src"]
                    :resource-paths ["dev/resources"]
                    :dependencies [[org.clojure/tools.nrepl "0.2.13"]
                                   [org.clojure/tools.namespace "0.2.11"]
                                   [lein-project-version "0.1.0"]
                                   ;; [integrant/repl "0.3.1"]
                                   [integrant/repl "0.2.0"]
                                   [eftest "0.5.2"]
                                   [kerodon "0.9.0"]]}

             :uberjar {:aot :all
                       :uberjar-name "orgpad-standalone.jar"
                       :global-vars {*assert* false}}}

  :source-paths ["src"]
  :java-source-paths ["src/java"]

  :clean-targets ^{:protect false} ["target"])
