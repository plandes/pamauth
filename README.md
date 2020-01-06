# XWiki PAM Authentication Module

This is a password authentication module ([PAM]) bridge from UNIX/Linux systems
to [XWiki].  This module was inspired by, modeled and written after the [LDAP
module].  The use case and mechanism is very similar.

This is a pure Java implementation that uses [userauth] library, which wraps
[pwauth], which is a command line tool that provides the authentication and
commonly available on UNIX/Linux machines as a package.


## Motivation

This module addresses the need for systems that have a particular [PAM]
configuration and want to use it to authenticate users.  This was written by
the author because the [LDAP module] did not consistently function as described
in this [LDAP over SSL thread].  For those that use the [LDAP NSS] [PAM]
module, which both authenticate users on the OS itself, using this module
allows authentication to [XWiki] to LDAP via this software.


## Obtaining

The [extension] is available via the XWiki Extensions manager.


## Installation

This package requires the [pwauth] program, which is detailed in the [userauth
package].


## Documentation

More [documentation](https://xwiki-contrib.github.io/authenticator-pam/):
* [Javadoc](https://xwiki-contrib.github.io/authenticator-pam/apidocs/index.html)
* [Dependencies](https://xwiki-contrib.github.io/authenticator-pam/dependencies.html)


## Building

To build from source, do the following:

- Install [Maven](https://maven.apache.org)
- Install [GNU make](https://www.gnu.org/software/make/) (optional)
- Build the software: `make`
- Build the distribution binaries: `make dist`

Note that you can also build a single jar file with all the dependencies with: `make package`


## Changelog

An extensive changelog is available [here](CHANGELOG.md).



## License

Copyright © 2019 - 2020 Paul Landes

GNU LESSER GENERAL PUBLIC LICENSE
                       Version 2.1, February 1999

 Copyright (C) 1991, 1999 Free Software Foundation, Inc.
 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 Everyone is permitted to copy and distribute verbatim copies
 of this license document, but changing it is not allowed.

[This is the first released version of the Lesser GPL.  It also counts
 as the successor of the GNU Library Public License, version 2, hence
 the version number 2.1.]


<!-- links -->

[XWiki]: https://www.xwiki.org/xwiki/bin/view/Main/WebHome
[LDAP module]: https://github.com/xwiki-contrib/ldap
[PAM]: https://en.wikipedia.org/wiki/Linux_PAM
[LDAP over SSL thread]: https://forum.xwiki.org/t/need-help-with-ldap-ssl/304/4
[LDAP NSS]: https://wiki.debian.org/LDAP/NSS

[userauth]: https://github.com/plandes/userauth
[pwauth]: https://github.com/phokz/pwauth
[userauth package]: https://github.com/plandes/userauth#installation
[extension]: https://extensions.xwiki.org/xwiki/bin/preview/Extension/PAM%20Authenticator/WebHome
