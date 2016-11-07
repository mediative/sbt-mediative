resolvers += Resolver.url("YPG-Data SBT Plugins", url("https://dl.bintray.com/ypg-data/sbt-plugins"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.mediative.sbt" % "sbt-mediative-core" % "0.2.0")
addSbtPlugin("com.mediative.sbt" % "sbt-mediative-oss"  % "0.2.0")
