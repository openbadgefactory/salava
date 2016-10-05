(defproject salava "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://salava.org"
  :license {:name "Apache 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.8.0"]

                 [com.stuartsierra/component "0.3.1"]
                 [org.clojure/tools.nrepl "0.2.12"]

                 ; Google Core Libraries for Java
                 [com.google.guava/guava "19.0"]

                 ; Database
                 [org.clojure/java.jdbc "0.6.1"]
                 [hikari-cp "1.7.1"]
                 [mysql/mysql-connector-java "5.1.39"]
                 [yesql "0.5.3"]
                 [migratus "0.8.26"]

                 ; Server side
                 [http-kit "2.1.19"]
                 [ring/ring-core "1.5.0" :exclusions [org.clojure/tools.reader]]
                 [ring/ring-devel "1.5.0"]
                 [ring/ring-defaults "0.2.1"]
                 [ring-webjars "0.1.1"]
                 [compojure "1.5.1"]
                 [org.clojure/core.memoize "0.5.8"]
                 [metosin/compojure-api "1.1.3"]
                 [hiccup "1.0.5"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-time "0.12.0"]
                 [prismatic/schema "1.1.2"]
                 [clojurewerkz/propertied "1.2.0"]
                 [com.draines/postal "2.0.0" :exclusions [commons-codec]]

                 [clj-http "3.3.0"]
                 [enlive "1.1.6"]
                 [markdown-clj "0.9.89"]
                 [ar.com.hjg/pngj "2.1.0"]
                 [com.novemberain/pantomime "2.8.0" :exclusions [org.bouncycastle/bcprov-jdk15on]]
                 [com.github.kyleburton/clj-xpath "1.4.5"]
                 [digest "1.4.4"]
                 [org.clojure/tools.cli "0.3.5"]
                 [alxlit/autoclave "0.2.0"]
                 [clj.qrgen "0.4.0"]

                 ; Client side
                 [org.clojure/clojurescript "1.9.89" :exclusions [com.google.code.findbugs/jsr305]]
                 [reagent "0.5.1"]
                 [reagent-utils "0.1.9"]
                 [bidi "2.0.9"]
                 [kibu/pushy "0.3.6"]
                 [com.taoensso/tower "3.1.0-beta3" :exclusions  [com.taoensso/encore]]
                 [cljs-ajax "0.5.8"]
                 [org.clojars.frozenlock/reagent-modals "0.2.5"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]

                 [org.webjars/jquery "2.2.4"]
                 [org.webjars/bootstrap "3.3.6"]
                 [org.webjars/font-awesome "4.6.3"]
                 [org.webjars/es5-shim "4.1.14"]
                 [org.webjars/es6-shim "0.20.2"]

                 ; Logging: use logback with slf4j, redirect JUL, JCL and Log4J:
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.7"]
                 [org.slf4j/slf4j-api "1.7.21"]
                 [org.slf4j/jul-to-slf4j "1.7.21"]        ; JUL to SLF4J
                 [org.slf4j/jcl-over-slf4j "1.7.21"]      ; JCL to SLF4J
                 [org.slf4j/log4j-over-slf4j "1.7.21"]    ; Log4j to SLF4J

                 ; Testing
                 [midje "1.8.3"]

                 ; Auth
                 [buddy "0.9.0"]

                 ]

  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :java-source-paths ["src/java"]
  :test-paths ["test/clj" "test/cljs" "test/cljc"]
  :profiles {:dev {:source-paths ["src/dev-clj"]
                   :dependencies [[figwheel "0.5.4-5"]
                                  [com.cemerick/piggieback "0.2.1"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [lein-midje "3.2"]
                                  [reloaded.repl "0.2.2"]]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                   :plugins [[lein-pdo "0.1.1"]
                             [lein-cljsbuild "1.1.3" :exclusions  [org.clojure/clojure]]
                             [lein-scss "0.2.4"]
                             [lein-figwheel "0.5.4-5"]]
                   :resource-paths ["target/generated"]}
             :uberjar {:resource-paths  ["target/adv"]
                       :main  salava.core.main
                       :aot   [salava.core.main]}}

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
                                        :externs        ["resources/public/js/externs/jquery.ext.js" "resources/public/js/externs/externs.js"]
                                        :output-to      "target/adv/public/js/salava.js"
                                        :optimizations  :advanced
                                        :elide-asserts  true
                                        :pretty-print   false}}}}

  :uberjar-name      "salava.jar"
  :auto-clean        false
  :min-lein-version  "2.6.1"

  :pedantic? :abort

  :aliases {"develop"         ["do" "clean" ["pdo" ["figwheel"] ["scss" ":dev" "boring"]]]
            "uberjar"         ["with-profile" "uberjar" "do" ["cljsbuild" "once" "adv"] ["scss" ":adv" "once" "boring"] "uberjar"]

            "translate"       ["run" "-m" "salava.core.translator/translate"]

            "migrate"         ["run" "-m" "salava.core.migrator/migrate"]
            "rollback"        ["run" "-m" "salava.core.migrator/rollback"]
            "migrator-remove" ["run" "-m" "salava.core.migrator/remove-plugin"]
            "migrator-seed"   ["run" "-m" "salava.core.migrator/seed"]
            "migrator-reset"  ["run" "-m" "salava.core.migrator/reset"]
            "test"            ["run" "-m" "user/do-test"]})
