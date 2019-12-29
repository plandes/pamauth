# PAM Authentication Module

This is a password authentication module ([PAM]) bridge from UNIX/Linux systems
to [XWiki].  This module was inspired by, modeled and written after the [LDAP
module].  The use case and mechanism is very similar.


## Motivation

This module addresses the need for systems that have a particular [PAM]
configuration and want to use it to authenticate users.  This was written by
the author because the [LDAP module] did not consistently function as described
in this [LDAP over SSL thread].  For those that use the [LDAP NSS] [PAM]
module, which both authenticate users on the OS itself, using this module
allows authentication to [XWiki] to LDAP via this software.



## Obtaining

In your `pom.xml` file add
the
[dependency XML element](https://plandes.github.io/pamauth/dependency-info.html) below:
```xml
<dependency>
    <groupId>com.zensols.xwiki</groupId>
    <artifactId>pamauth</artifactId>
    <version>0.0.1</version>
</dependency>
```


## Documentation

More [documentation](https://plandes.github.io/pamauth/):
* [Javadoc](https://plandes.github.io/pamauth/apidocs/index.html)
* [Dependencies](https://plandes.github.io/pamauth/dependencies.html)


## Building

To build from source, do the folling:

- Install [Maven](https://maven.apache.org)
- Install [GNU make](https://www.gnu.org/software/make/) (optional)
- Build the software: `make`
- Build the distribution binaries: `make dist`

Note that you can also build a single jar file with all the dependencies with: `make package`


## Changelog

An extensive changelog is available [here](CHANGELOG.md).



## License

Copyright © 2019 Paul Landes

Apache License version 2.0

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


<!-- links -->

[XWiki]: https://www.xwiki.org/xwiki/bin/view/Main/WebHome
[LDAP module]: https://github.com/xwiki-contrib/ldap
[PAM]: https://en.wikipedia.org/wiki/Linux_PAM
[LDAP over SSL thread]: https://forum.xwiki.org/t/need-help-with-ldap-ssl/304/4
[LDAP NSS]: https://wiki.debian.org/LDAP/NSS
