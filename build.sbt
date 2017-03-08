organization in ThisBuild := "com.mediative.sbt"
name in ThisBuild         := "sbt-mediative"

lazy val root = project.in(file("."))
  .enablePlugins(MediativeGitHubPlugin, MediativeReleasePlugin)
  .settings(noPublishSettings)
  .aggregate(core, oss, devops, teamcity)

lazy val core = project.configure(plugin)
  .enablePlugins(MediativeBintrayPlugin)
  .settings(
    addSbtPlugin("com.typesafe.sbt"  % "sbt-scalariform" % "1.3.0"),
    addSbtPlugin("com.typesafe.sbt"  % "sbt-git"         % "0.8.5"),
    addSbtPlugin("com.github.gseitz" % "sbt-release"     % "1.0.4")
  )

lazy val oss = project.configure(plugin)
  .enablePlugins(MediativeBintrayPlugin)
  .settings(
    addSbtPlugin("com.typesafe.sbt"  % "sbt-site"    % "1.2.0"),
    addSbtPlugin("com.typesafe.sbt"  % "sbt-ghpages" % "0.6.0"),
    addSbtPlugin("com.eed3si9n"      % "sbt-unidoc"  % "0.4.0"),
    addSbtPlugin("me.lessis"         % "bintray-sbt" % "0.3.0"),
    addSbtPlugin("de.heikoseeberger" % "sbt-header"  % "1.5.0")
  )
  .dependsOn(core)

lazy val devops = project.configure(plugin)
  .enablePlugins(MediativeBintrayPlugin)
  .settings(
    libraryDependencies += "org.scalaj" %% "scalaj-http" % "2.3.0",
    libraryDependencies += "com.typesafe" % "config" % "1.3.1",
    addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.0-M8"),
    resolvers += Resolver.url("fonseca-sbt-plugins", url("https://dl.bintray.com/fonseca/sbt-plugins"))(Resolver.ivyStylePatterns),
    addSbtPlugin("io.github.jonas" % "sbt-dcos" % "0.1.1")
  )

lazy val teamcity = project.configure(plugin)
  .enablePlugins(MediativeBintrayPlugin)
  .settings(
    addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.6.1")
  )

def plugin(project: Project) =
  project.settings(
    name := s"sbt-mediative-${project.id}",
    sbtPlugin := true,
    bintrayRepository := "sbt-plugins"
  )
