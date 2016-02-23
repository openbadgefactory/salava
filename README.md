# salava

## Open Badge Passport community edition

[Open Badges](http://openbadges.org/) is an open standard developed by the
Mozilla Foundation to recognize, validate and demonstrate learning that
happens anywhere. Open Badges are digital credentials, created and issued by
organizations such as schools, vocational organizations, companies and
employers for their students, members, staff, clients or partners.

[Open Badge Passport](https://openbadgepassport.com/) is a platform for badge
earners to easily receive, save and organize their Open Badges and share them
on social media such as LinkedIn, Twitter and Facebook. Salava (this project)
is the open source implementation of the currently running
proof-of-concept. Our first goal is to have the same feature set and then
build on that.

Quickest way to see what this project is about is to
[create an account](https://openbadgepassport.com/en/user/register)
in Open badge Passport (it's free) and play around with that.


## Quick start

The project is still in its early stages and not really suitable for any real
use. Still, you can try it out.

The code is known to work with Ubuntu Linux, Oracle Java 8 and MariaDb 10. We use
[Leiningen](http://leiningen.org/) as dependency manager. For building scss files you need a sass
compiler, such as [sassc](https://github.com/sass/sassc).

Start the installation with creating your config file:

    $ cp resources/config/core.edn.base resources/config/core.edn

Edit the file and add your db settings etc. (Don't forget to create the
database as well).

Create a directory to store files that are uploaded or created by Salava. Add
the directory to the config file (keyword :data-dir).

After that:

    # Initialize your db and insert some sample data
    $ lein migrator-reset

    # Build translation files
    $ lein translate

    # Start figwheel, cljsbuild and scss compiler
    $ lein develop

    # (in another terminal)

    # Start application server
    $ lein repl
    # ...
    user=> (go)



## TODO

- OAuth
- Full text search
- User groups
- Admin tools
- More tests



## License

Copyright (c) 2015-2016 Discendum Oy and contributors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
