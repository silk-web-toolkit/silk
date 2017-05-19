# SILK WEB TOOLKIT

[![Clojars Project](https://img.shields.io/clojars/v/org.silkyweb/silk.svg)](https://clojars.org/org.silkyweb/silk)

## About

Silk allows you to build static websites out of re-usable building blocks without the use of server side languages, just use HTML, CSS and Javascript.  Sites built with Silk are clean from semantic pollution, no tag soup.

Improve the security of the web, use static wherever possible.  How many sites are dynamic beyond a contact form really ?  Why generate site content for every visit when you can do it once ?

## Getting Started

Please see the [silkyweb.org site](http://www.silkyweb.org/) for more documentation.

There are binaries for a number of platforms on the releases page.

Alternatively you can run the custom installation script which will put silk in ~/bin.

    bash <(curl -fksSL https://raw.githubusercontent.com/silk-web-toolkit/silk/develop/install/install.sh)

### Development

Run lein uberjar to build a single jar file for the project.

The Silk community is in its infancy and as such is pretty flexible right now.  We work around one principle belief... `'quod severis metes` which translates as 'as you sow, so shall you reap'.  We are all in this to make something useful, and we hope to have fun along the way.

For formatting we prefer two space indentation, this seems to work well for HTML and cross editor/IDE compatibility.  For patch/commits/pull requests we prefer small atomic units of work where the intent is clearly expressed and one topic is submitted at a time.  As we are on Github feature branches are best for pull requests.

To get started with contributing just come and ask about it on the mailing list.  We have plenty of ideas for things that need to be done.  Several are listed below.
* new site prototype's
* new components static and dynamic
* general improvements to the somewhat prototypy code

Our [issues list](https://github.com/organizations/silk-web-toolkit/dashboard/issues) is usually kept up to date over on Github.

### Releases and distribution

See the build instructions in build/README.md to build for various platforms.

## Status

Silk is already used in commercial projects at large multinationals.

## License

Silk is licensed with Apache License v2.0.
