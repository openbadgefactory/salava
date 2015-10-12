(defproject salava "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://salava.org"
  :license {:name "Apache 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.7.0"]

                 [com.stuartsierra/component "0.3.0"]
                 [org.clojure/tools.nrepl "0.2.11"]

                 ; Database
                 [org.clojure/java.jdbc "0.4.2"]
                 [hikari-cp "1.3.1"]
                 [mysql/mysql-connector-java "5.1.36"]
                 [yesql "0.5.1"]
                 [migratus "0.8.7"]

                 ; Server side
                 [http-kit "2.1.19"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-devel "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [ring-webjars "0.1.1"]
                 [compojure "1.4.0"]
                 [metosin/compojure-api "0.23.1"]
                 [hiccup "1.0.5"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-time "0.11.0"]
                 [prismatic/schema "0.4.4"]
                 [clojurewerkz/propertied "1.2.0"]

                 ; Client side
                 [org.clojure/clojurescript "1.7.122"]
                 [reagent "0.5.1"]
                 [bidi "1.21.0"]

                 [org.webjars/jquery "2.1.4"]
                 [org.webjars/bootstrap "3.3.5"]
                 [org.webjars/font-awesome "4.4.0"]
                 [org.webjars/es5-shim "4.0.6"]

                 ; Logging: use logback with slf4j, redirect JUL, JCL and Log4J:
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [org.slf4j/slf4j-api "1.7.12"]
                 [org.slf4j/jul-to-slf4j "1.7.12"]        ; JUL to SLF4J
                 [org.slf4j/jcl-over-slf4j "1.7.12"]      ; JCL to SLF4J
                 [org.slf4j/log4j-over-slf4j "1.7.12"]    ; Log4j to SLF4J

                 ]

  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :test-paths ["test/clj" "test/cljs" "test/cljc"]
  :profiles {:dev {:source-paths ["src/dev-clj"]
                   :dependencies [[figwheel "0.4.0" :exclusions [org.clojure/clojurescript]]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [reloaded.repl "0.2.0"]]
                   :plugins [[lein-pdo "0.1.1"]
                             [lein-cljsbuild "1.0.5"]
                             [lein-scss "0.2.2"]
                             [lein-figwheel "0.2.7" :exclusions [org.clojure/clojurescript]]]
                   :resource-paths ["target/generated"]}
             :uberjar {:resource-paths  ["target/adv"]
                       :main  salava.main
                       :aot   [salava.main]}}

  :scss  {:builds
          {:dev {:source-dir "src/scss"
                   :dest-dir   "target/generated/public/css"
                   :executable "sassc"
                   :args       ["-m" "-l" "-I" "src/scss" "-t" "nested"]}
           :adv {:source-dir "src/scss"
                 :dest-dir   "target/adv/public/css"
                 :executable "sassc"
                 :args       ["-I" "src/scss/" "-t" "compressed"]}}}


  :figwheel {:http-server-root  "public"
             :server-port       3449
             :css-dirs          ["target/generated/public/css"]
             :repl              false
             :server-logfile    "target/figwheel-logfile.log"}

  :cljsbuild {:builds {:dev {:source-paths ["src/cljs" "src/cljc" "src/dev-cljs"]
                             :compiler {:main            "salava.core.ui.figwheel"
                                        :asset-path      "/js/out"
                                        :output-to       "target/generated/public/js/salava.js"
                                        :output-dir      "target/generated/public/js/out"
                                        :source-map      true
                                        :optimizations   :none
                                        :cache-analysis  true
                                        :pretty-print    true}}
                       :adv {:source-paths ["src/cljs" "src/cljc"]
                             :compiler {:main           "salava.core.ui.main"
                                        :output-to      "target/adv/public/js/salava.js"
                                        :optimizations  :advanced
                                        :elide-asserts  true
                                        :pretty-print   false}}}}

  :uberjar-name      "salava.jar"
  :auto-clean        false
  :min-lein-version  "2.5.2"

  :aliases {"develop" ["do" "clean" ["pdo" ["figwheel"] ["scss" ":dev" "boring"]]]
            "uberjar" ["with-profile" "uberjar" "do" ["cljsbuild" "once" "adv"] ["scss" ":adv" "once" "boring"] "uberjar"]

            "translate"       ["run" "-m" "salava.core.i18n/translate"]

            "migrate"         ["run" "-m" "salava.core.migrator/migrate"]
            "rollback"        ["run" "-m" "salava.core.migrator/rollback"]
            "migrator-remove" ["run" "-m" "salava.core.migrator/remove-plugin"]
            "migrator-seed"   ["run" "-m" "salava.core.migrator/seed"]
            "migrator-reset"  ["run" "-m" "salava.core.migrator/reset"]})
