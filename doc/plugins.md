Plugin design notes
===================

Plugins in Salava are self-contained software modules which are responsible
for all of the client-facing features of the service. Salava core is a thin
layer which ties everything together but has very little business logic of its
own.

We prefer convention over configuration. Enabled plugins are defined in core
config and are automatically discoverable by their names.

Dependencies between plugins should be limited. When writing plugins,
modifications to shared code must be avoided unless absolutely
necessary. One exception is general-purpose code which can be common to any
web app. These kinds of functions can be moved to core.

Plugins can have their own:

- config
- clj/cljc/cljs namespaces
- routes
- navigation
- i18n
- migrations
- css

