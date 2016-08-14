organization in ThisBuild := "com.mediative.sbt"
name in ThisBuild         := "sbt-mediative"

lazy val root = project.in(file("."))
  .enablePlugins(MediativeGitHubPlugin, MediativeReleasePlugin)
  .settings(noPublishSettings)
  .aggregate(core, oss)

lazy val core = project.configure(plugin)
  .enablePlugins(MediativeBintrayPlugin)
  .settings(
    addSbtPlugin("com.typesafe.sbt"  % "sbt-scalariform" % "1.3.0"),
    addSbtPlugin("com.typesafe.sbt"  % "sbt-git"         % "0.8.5"),
    addSbtPlugin("com.github.gseitz" % "sbt-release"     % "1.0.0")
  )

lazy val oss = project.configure(plugin)
  .enablePlugins(MediativeBintrayPlugin)
  .settings(
    addSbtPlugin("com.typesafe.sbt"  % "sbt-ghpages" % "0.5.4"),
    addSbtPlugin("me.lessis"         % "bintray-sbt" % "0.3.0"),
    addSbtPlugin("de.heikoseeberger" % "sbt-header"  % "1.5.0")
  )
  .dependsOn(core)

def plugin(project: Project) =
  project.settings(
    name := s"sbt-mediative-${project.id}",
    sbtPlugin := true,
    bintrayRepository := "sbt-plugins"
  )
