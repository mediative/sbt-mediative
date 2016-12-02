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

import sbt._
import sbt.Keys._
import sbtbuildinfo.BuildInfoPlugin

/**
 * Provides a plugin to configure sbt-buildinfo for TeamCity builds.
 *
 * To use add the following lines to the project definition:
 *
 * {{{
 * .enablePlugins(MediativeTeamCityPlugin)
 * }}}
 *
 * By default it write to the package `<organization>.<name>`. This can be
 * overridden using `buildInfoPackage`:
 * {{{
 * .settings(buildInfoPackage := "com.company.pkg")
 * }}}
 *
 * This plugin must be enabled.
 */
object MediativeTeamCityPlugin extends AutoPlugin {

  import BuildInfoPlugin.autoImport._

  override def requires: Plugins = BuildInfoPlugin
  override def trigger = noTrigger

  override def projectSettings: Seq[Setting[_]] = Seq(
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      version,
      BuildInfoKey(("builtBy", teamcityBuildId.getOrElse(sys.props("user.name"))))
    ),
    buildInfoPackage := s"${organization.value}.${name.value}",
    buildInfoOptions ++= Seq(
      BuildInfoOption.ToMap,
      BuildInfoOption.ToJson,
      BuildInfoOption.BuildTime
    )
  )

  def teamcityBuildId = for {
    projectName <- sys.env.get("TEAMCITY_PROJECT_NAME")
    buildConf <- sys.env.get("TEAMCITY_BUILDCONF_NAME")
    buildNumber <- sys.env.get("BUILD_NUMBER")
  } yield s"$projectName :: $buildConf #$buildNumber"

}
