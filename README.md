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
is the open source implementation of Open Badge Passport.

Quickest way to see what this project is about is to
[create an account](https://openbadgepassport.com/en/user/register)
in Open badge Passport (it's free) and play around with that.


## Quick start

The code is known to work with Ubuntu Linux, Oracle Java 8 and MariaDb 10. We use
[Leiningen](http://leiningen.org/) as dependency manager. For building scss files you need a sass
compiler:

    $ gem install sass

Other sass install options can be found here: [https://sass-lang.com/install](https://sass-lang.com/install)


Install MariaDB server:

    #MacOs
      $ brew install mariadb

    #Linux
      $ sudo apt-get install mariadb-server

Create the database:

    $ sudo mysql
    > create database salava;
    > create database salava_test;
    > create user 'salava'@'127.0.0.1' identified by 'salava';
    > grant all privileges on salava.* to 'salava'@'127.0.0.1';
    > grant all privileges on salava_test.* to 'salava'@'127.0.0.1';
    > quit

Create your config files for development and testing (make a copy of all \*.base files):

    $ cp resources/config/core.edn.base resources/config/core.edn
    $ cp resources/test_config/core.edn.base resources/test_config/core.edn


Edit the files and add your db settings etc.

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

    # Load test config and run all tests
    user=> (toggle-test-mode)
    user=> (run-tests)

    # Switch back to development mode
    user=> (toggle-test-mode)

## TODO

- Full text search
- More tests


## License

Copyright (c) 2015-2019 Discendum Oy and contributors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
