/*
 * Copyright 2016 Mediative
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediative.sbt

import scala.language.postfixOps
import sbt._
import sbt.Keys._
import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform._
import com.typesafe.sbt.GitPlugin

object JvmCompatibility extends VersionNumberCompatibility {
  def name = "Java specification compatibility"
  def isCompatible(current: VersionNumber, required: VersionNumber) =
    current.numbers.zip(required.numbers).forall(n => n._1 >= n._2)

  sealed trait Version {
    def version = VersionNumber(toString)
    def required = Seq(
      scalacOptions += s"-target:jvm-${version}",
      initialize := {
        val _ = initialize.value // run previous initialization
        val curr = VersionNumber(sys.props("java.specification.version"))
        require(isCompatible(curr, version),
          s"Java ${version} or above is required for this project.")
      }
    )
  }

  case object `1.7` extends Version
  case object `1.8` extends Version
}

/**
 * Provides the core settings used by Mediative SBT projects.
 *
 * This plugin is automatically enabled.
 */
object MediativeCorePlugin extends AutoPlugin {

  object autoImport {
    /**
     * Restrict compilation target to a specific JVM version.
     *
     * Usage:
     * ```scala
     * Jvm.`1.8`.required
     * ```
     */
    val Jvm = JvmCompatibility

    /**
     * Disable all publishing of a project.
     */
    lazy val noPublishSettings = Seq(
      publish := (()),
      publishLocal := (()),
      publishArtifact := false
    )
  }

  override def requires: Plugins = plugins.JvmPlugin && GitPlugin

  override def trigger = allRequirements

  override def buildSettings: Seq[Setting[_]] =
    // Version project based on git-describe
    GitPlugin.autoImport.versionWithGit ++
    Seq(GitPlugin.autoImport.git.useGitDescribe := true)

  /**
   * Add the IntegrationTest config to the project. The `extend(Test)`
   * part makes it so classes in src/it have a classpath dependency on
   * classes in src/test. This makes it simple to share common test
   * helper code.
   *
   * See http://www.scala-sbt.org/release/docs/Testing.html#Custom+test+configuration
   */
  override val projectConfigurations = Seq(Configurations.IntegrationTest extend (Test))

  /** Settings added automatically to all projects. */
  override def projectSettings: Seq[Setting[_]] =
    Defaults.itSettings ++
    Seq(
      scalacOptions ++= Seq(
        "-deprecation",
        "-encoding", "UTF-8",
        "-feature",
        "-language:existentials",
        "-language:higherKinds",
        "-language:implicitConversions",
        "-language:experimental.macros",
        "-unchecked",
        "-Xexperimental",
        "-Xfatal-warnings",
        "-Xlint",
        "-Xfuture",
        "-Yno-adapted-args",
        "-Ywarn-dead-code",
        "-Ywarn-value-discard"
        // "-Ywarn-numeric-widen" Requires Scala 2.11: https://issues.scala-lang.org/browse/SI-8340
      ),
      scalacOptions in (Compile, console) ~= { _.filterNot(Set("-Ywarn-unused-import", "-Xfatal-warnings")) },
      scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value,
      resolvers += Resolver.bintrayRepo("ypg-data", "maven"),
      fork in Test := true,
      // Show current project name in the SBT prompt, e.g. `ReadWrite>`
      shellPrompt in ThisBuild := { state => Project.extract(state).currentRef.project + "> " }
    ) ++
    codeFormatSettings

  /**
   * Configure Scalariform according to the [Scala style
   * guide](https://github.com/daniel-trinh/scalariform#scala-style-guide)
   * and provide a couple of handy aliases for reformatting the code.
   */
  def codeFormatSettings: Seq[Setting[_]] =
    Seq(
      ScalariformKeys.preferences := ScalariformKeys.preferences.value
        .setPreference(DoubleIndentClassDeclaration, true)
        .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true),
      commands += Command.command("reformat-code-check-unchanged") { state =>
        if (("git diff --exit-code" !) != 0) {
          state.log.error("Run `sbt reformat-code` to fix Scala style formatting issues")
          sys.exit(1)
        }
        state
      }
    ) ++
    defaultScalariformSettingsWithIt ++
    addCommandAlias("reformat-code",       ";compile:scalariformFormat;test:scalariformFormat;it:scalariformFormat") ++
    addCommandAlias("reformat-code-check", ";reformat-code;reformat-code-check-unchanged")

}
