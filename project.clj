(defproject salava "3.5.3"
  :description "Salava application server"
  :url "http://salava.org"
  :license {:name "Apache 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.10.0"]

                 [com.stuartsierra/component "0.3.2"]
                 [org.clojure/tools.nrepl "0.2.13"]

                 ; Dependecy conflict overrides
                 [com.google.guava/guava "25.1-jre"] ; Google Core Libraries for Java
                 [commons-logging "1.2"]
                 [commons-codec "1.12"]
                 [com.google.code.findbugs/jsr305 "3.0.2"]

                 ; Database
                 [org.clojure/java.jdbc "0.7.9"]
                 [hikari-cp "2.7.1"]
                 [mysql/mysql-connector-java "8.0.16"]
                 [yesql "0.5.3" :exclusions [instaparse]]
                 [migratus "1.2.3"]

                 ; Server side
                 [http-kit "2.3.0"]
                 [ring/ring-core "1.7.1"]
                 [ring/ring-devel "1.7.1" :exclusions [clj-stacktrace]]
                 [ring/ring-defaults "0.3.2"]
                 [ring-webjars "0.2.0" :exclusions [com.fasterxml.jackson.core/jackson-databind]]
                 [ring/ring-mock "0.4.0"]
                 [compojure "1.6.1"]
                 [cheshire "5.8.1"]
                 [org.clojure/core.memoize "0.7.1"]
                 [metosin/compojure-api "1.1.12" :exclusions [potemkin]]
                 [hiccup "1.0.5"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-time "0.15.1"]
                 [prismatic/schema "1.1.10"]
                 [clojurewerkz/propertied "1.3.0"]
                 [com.draines/postal "2.0.3"]
                 [org.clojure/core.async "0.4.490"]

                 [clj-http "3.10.0"]
                 [enlive "1.1.6" :exclusions [org.jsoup/jsoup]]
                 [markdown-clj "1.10.0"]
                 [ar.com.hjg/pngj "2.1.0"]
                 [com.novemberain/pantomime "2.11.0" :exclusions [javax.activation/activation]]
                 [com.github.kyleburton/clj-xpath "1.4.11"]
                 [digest "1.4.9"]
                 [org.clojure/tools.cli "0.4.2"]
                 [alxlit/autoclave "0.2.0"]
                 [clj.qrgen "0.4.0"]
                 [clj-pdf "2.2.34"]
                 [clj-pdf-markdown "0.2.1"]
                 [org.clojure/core.cache "0.7.2"]

                 ; Client side
                 [org.clojure/clojurescript "1.10.520" :exclusions [com.google.errorprone/error_prone_annotations]]
                 [reagent "0.8.1"]
                 [reagent-utils "0.3.3"]
                 [bidi "2.1.6"]
                 [kibu/pushy "0.3.8"]
                 [com.taoensso/tower "3.1.0-beta3"]
                 [cljs-ajax "0.8.0"]
                 [org.clojars.frozenlock/reagent-modals "0.2.8"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                 [metosin/komponentit "0.3.8"]
                 [com.cemerick/url "0.1.1"]
                 [cljsjs/clipboard "1.6.1-1"]
                 [cljsjs/simplemde "1.11.2-0"]
                 [prismatic/dommy "1.1.0"]


                 [org.webjars/jquery "2.2.4"]
                 [org.webjars/bootstrap "3.3.7"]
                 [org.webjars/font-awesome "4.7.0"]
                 [org.webjars/es5-shim "4.5.9"]
                 [org.webjars/es6-shim "0.20.2"]
                 [org.webjars/leaflet "1.4.0"]

                 ; Logging: use logback with slf4j, redirect JUL, JCL and Log4J:
                 [org.clojure/tools.logging "0.4.1"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [org.slf4j/slf4j-api "1.7.26"]
                 [org.slf4j/jul-to-slf4j "1.7.26"]        ; JUL to SLF4J
                 [org.slf4j/jcl-over-slf4j "1.7.26"]      ; JCL to SLF4J
                 [org.slf4j/log4j-over-slf4j "1.7.26"]    ; Log4j to SLF4J

                 ; Testing
                 [midje "1.9.8" :exclusions [org.clojure/tools.namespace clj-time]]

                 ; Auth
                 [buddy "2.0.0"  :exclusions [org.bouncycastle/bcpkix-jdk15on]]]



  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :java-source-paths ["src/java"]
  :test-paths ["test/clj" "test/cljs" "test/cljc"]

  :profiles {:dev     {:source-paths   ["src/dev-clj"]
                       :dependencies   [[figwheel-sidecar "0.5.18" :exclusions [args4j]]
                                        ;[com.cemerick/piggieback "0.2.1"]
                                        ;[org.clojure/tools.namespace "0.3.0-alpha4"]
                                        [lein-midje "3.2.1"]
                                        [reloaded.repl "0.2.4"]]
                       ;:repl-options   {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                       :plugins        [[lein-pdo "0.1.1"]
                                        [lein-cljsbuild "1.1.7" :exclusions [org.clojure/clojure]]
                                        [lein-scss "0.3.0"]
                                        [lein-figwheel "0.5.18"]]
                       :resource-paths ["target/generated"]}
             :uberjar {:resource-paths ["target/adv"]
                       :main           salava.core.main
                       :omit-source    true
                       :aot            :all}}


  :scss  {:builds
          {:dev {:source-dir "src/scss"
                 :dest-dir   "target/generated/public/css"
                 :executable "sass"
                 :args       ["--line-numbers" "-I" "src/scss" "-t" "nested"]}
           :adv {:source-dir "src/scss"
                 :dest-dir   "target/adv/public/css"
                 :executable "sass"
                 :args       ["-I" "src/scss/" "-t" "compressed"]}}}



  :figwheel {:http-server-root "public"
             :server-port      3450
             :css-dirs         ["target/generated/public/css"]
             :repl             false
             :server-logfile   "target/figwheel-logfile.log"}


  :cljsbuild {:builds {:dev {:source-paths ["src/cljs" "src/cljc" "src/dev-cljs"]
                             :compiler     {:main           "salava.core.ui.figwheel"
                                            :asset-path     "/js/out"
                                            :output-to      "target/generated/public/js/salava.js"
                                            :output-dir     "target/generated/public/js/out"
                                            :source-map     true
                                            :optimizations  :none
                                            :cache-analysis true
                                            :pretty-print   true}}
                       :adv {:source-paths ["src/cljs" "src/cljc"]
                             :compiler     {:main          "salava.core.ui.main"
                                            :externs       ["resources/public/js/externs/jquery.ext.js" "resources/public/js/externs/externs.js"]
                                            :output-to     "target/adv/public/js/salava.js"
                                            :optimizations :advanced
                                            :elide-asserts true
                                            :pretty-print  false}}}}

  :auto-clean        false
  :min-lein-version  "2.7.1"

  :pedantic? :abort

  :aliases {"develop" ["do" "clean" ["pdo" ["figwheel"] ["scss" ":dev" "boring"]]]
            "css" ["do" ["pdo" ["scss" ":dev" "boring"]]]
            "uberjar" ["with-profile" "uberjar" "do" "clean" ["cljsbuild" "once" "adv"] ["scss" ":adv" "once" "boring"] "uberjar"]

            "translate" ["run" "-m" "salava.core.translator/translate"]

            "migrate"         ["run" "-m" "salava.core.migrator/migrate"]
            "rollback"        ["run" "-m" "salava.core.migrator/rollback"]
            "migrator-remove" ["run" "-m" "salava.core.migrator/remove-plugin"]
            "migrator-seed"   ["run" "-m" "salava.core.migrator/seed"]
            "migrator-reset"  ["run" "-m" "salava.core.migrator/reset"]})
