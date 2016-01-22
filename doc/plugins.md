# Plugins


NOTICE: Features and APIs described in this document are not yet finalized and
may change. No backwards compatibility guaranteed.

---

Plugins in Salava are independent, self-contained software modules. Most
features are implemented in plugins. Salava core is a thin layer which ties
everything together but has very little business logic of its own.

Basic site functionality is built with these default plugins:

- badge
- page
- gallery
- file 
- translator
- user


Enabled plugins are listed in resources/config/core.edn and are discoverable by
their names. Plugin names should be descriptive, singular English words
(use letters a-z). Plugin names are used in namespaces, filenames and routes.

Dependencies between plugins should be limited. When writing plugins,
modifications to shared code must be avoided unless absolutely
necessary. One exception is general-purpose code which can be
common to any web app. These kinds of functions can be moved to
core.

## Synopsis

A simple hello world plugin might look something like this:


Server side routes and handler code:

```
file: src/clj/salava/hello/main.clj

(ns salava.hello.main
  (:require [yesql.core :refer [defqueries]]
            [salava.core.util :refer [get-db]]))

(defqueries "sql/hello/main.sql")

(defn get-counter
  "Get current counter value"
  [ctx]
  (first (select-counter {} (get-db ctx))))

(defn increment-counter
  "Increment current counter value"
  [ctx]
  (increment-counter! {} (get-db ctx)))
```

```
file: src/clj/salava/hello/routes.clj

(ns salava.hello.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [salava.core.layout :as layout]
            [salava.hello.main :as main]))

(defn route-def [ctx]
  (routes
    (context "/hello" []
             (layout/main ctx "/"))

    (context "/obpv1/hello" []
      (GET "/counter" []
            :summary "Get current counter value"
            (ok (main/get-counter ctx)))

      (POST "/counter" []
            :summary "Increment current counter value"
            (ok {:success (boolean (main/increment-counter ctx))})))))
```


Client side routes, navigation and handler code:

```
file src/cljs/salava/hello/ui/routes.cljs

(ns salava.hello.ui.routes
  (:require [salava.core.i18n :as i18n :refer [t]]
            [salava.hello.ui.main :as main]))

(defn ^:export routes [context]
  {"/hello" [["" main/handler]]})

(defn ^:export navi [context]
  {"/hello" {:weight 99 :title (t :hello/Hello) :breadcrumb (t :hello/Hello)}})

```

```
file: src/cljs/salava/hello/ui/main.cljs

(ns salava.hello.ui.main
  (:require [reagent.core :refer [atom]]
            [ajax.core :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]))

(defn inc-counter [state]
  (ajax/POST "/obpv1/hello/counter"
             {:handler (fn [status]
                         (when (status "success")
                           (swap! state inc)))}))

(defn content [state]
  [:div
   [:h1 (t :hello/Hellocounter)]
   [:div {:class "counter-val"} (str @state)]
   [:button {:type "button"
             :class "btn btn-primary"
             :on-click #(inc-counter state)}
    (t :hello/Increment)]])

(defn init-state [state]
  (ajax/GET "/obpv1/hello/counter"
            {:handler (fn [data]
                        (let [counter (get data "value" 0)]
                          (reset! state counter)))}))

(defn handler [site-navi]
  (let [state (atom 0)]
    (init-state state)
    (fn []
      (layout/default site-navi (content state)))))
```


Up/down migrations and seed data:

```
file: resources/migrations/hello/sql/201601221421-counter-table.up.sql

CREATE TABLE `hello_counter` (
  `value` bigint(20) unsigned DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8\_unicode\_ci;
```

```
file: resources/migrations/hello/sql/201601221421-counter-table.down.sql

DROP TABLE IF EXISTS `hello_counter`;
```
```
file: resources/migrations/hello/seed/data.edn

({:table "hello\_counter"
  :data  {:value 0}})
```


SQL queries for Yesql:

```
file: resources/sql/hello/main.sql

-- name: select-counter
SELECT value FROM hello\_counter

-- name: increment-counter!
UPDATE hello\_counter SET value = value + 1
```


Translation keys:

```
file: resources/i18n/en/hello\_en.properties

Hello=Hello
Hellocounter=Hello counter!
Increment=Increment
```


Style sheet:

```
file: src/scss/hello.scss

.counter-val {
    margin: 50px 0;
    font-size: 100px;
}
```


Finally, enable our new plugin:

```
file: resources/config/core.edn

...
; Append our plugin to the list
:plugins [:badge :page :gallery :file :user :hello]
...

```

```
file: src/cljc/salava/registry.cljc

(ns salava.registry
   (:require
     ; SERVER SIDE
     #?@(:clj [[salava.core.helper]])
     ; CLIENT SIDE
     ; List all your clojurescript route files here:
     #?@(:cljs [[salava.badge.ui.routes]
                [salava.page.ui.routes]
                [salava.gallery.ui.routes]
                [salava.file.ui.routes]
                [salava.user.ui.routes]
                [salava.hello.ui.routes] ; <- Our plugin route ns
                [salava.core.ui.routes]])))

```


## Routes

Salava is built around compojure-api.

Every plugin is expected to have ```salava.[plugin].routes``` namespace and it
must define ```route-def``` function. Return values from these plugin functions
are combined to form the salava REST api.

Client side routing is done with Bidi. See existing plugins for examples of
route handlers and navigation definition functions.

## Configuration

Plugins can have their own config files in resources/config/{plugin}.edn

These files are ignored by git so it's recommended to write your default config
to resources/config/{plugin}.edn.base which can be copied over.

Config files are read at startup and they are exposed to route handlers in the
context map.

Remember to add your plugin to the enabled plugin list in core config.


## Translations

Translation strings are stored in  
resources/i18n/[lang]/[plugin]\_[lang].properties

These are regular Java properties files. English is used as the base language.
Add your new translation keys to [plugin]\_en.properties file and run

    lein translate

This will copy new keys to other enabled language files where they can be translated


## Migrations

Db migration files can be found in resources/migrations/[plugin]/sql. A migration has
separate files for adding new features (up) and rolling back (down). They are
identified by unique id numbers in file names, e.g.
201601221421-counter-table.up.sql. Migrations are applied in alphabetical order
so id numbers should be composed from current date and time.

Seed data for testing can be defined in seed resources/migrations/[plugin]/seed/data.edn
