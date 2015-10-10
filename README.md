# Mediative SBT Plugins

A collection of opinionated plugins to minimize boilerplate for setting up new
Mediative SBT projects.

## Getting Started

To use the plugins add the following to `project/plugins.sbt`:

    resolvers += Resolver.url("YPG-Data SBT Plugins", url("https://dl.bintray.com/ypg-data/sbt-plugins"))(Resolver.ivyStylePatterns)
    addSbtPlugin("com.mediative.sbt" % "sbt-mediative-core" % "0.1")
    addSbtPlugin("com.mediative.sbt" % "sbt-mediative-oss" % "0.1")

The `sbt-mediative-oss` plugin provides the command `bootstrap-project` to
help quickly setup a new project:

    root> bootstrap-project
    [info] Writing LICENSE file...
    [info] Writing CONTRIBUTING.md file...

## Building

To build the plugins for use locally run the following commands from the project
root:

    $ sbt publishLocal

See [CONTRIBUTING.md](CONTRIBUTING.md) for how to contribute.

## Releasing

To release version `x.y.z` run:

    $ sbt release -Dversion=x.y.z

This will take care of running tests, tagging and publishing JARs and API docs.

## License

Copyright 2015 Mediative

Licensed under the Apache License, Version 2.0. See LICENSE file for terms and
conditions for use, reproduction, and distribution.