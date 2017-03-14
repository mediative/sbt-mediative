resolvers += Resolver.url("mediative:sbt-plugins", url("https://dl.bintray.com/mediative/sbt-plugins"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.mediative.sbt" % "sbt-mediative-core" % "0.5.0")
addSbtPlugin("com.mediative.sbt" % "sbt-mediative-oss"  % "0.5.0")
