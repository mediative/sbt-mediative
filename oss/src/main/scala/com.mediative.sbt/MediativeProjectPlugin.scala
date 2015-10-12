/*
 * Copyright 2015 Mediative
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
import scala.io.Source
import de.heikoseeberger.sbtheader.HeaderPlugin
import de.heikoseeberger.sbtheader.license.Apache2_0
import sbtrelease.ReleasePlugin

import HeaderPlugin.autoImport.headers
import ReleasePlugin.autoImport._

/**
 * Settings related with licensing and headers.
 *
 * This plugin is automatically enabled.
 */
object MediativeProjectPlugin extends AutoPlugin {

  object autoImport {
    val repoOrganization = settingKey[String]("Organization owning the repository")
    val repoName = settingKey[String]("Repository name for publishing")
    val postReleaseSteps = settingKey[Seq[ReleaseStep]]("Post release steps")
  }
  import autoImport._

  override def requires = sbt.plugins.JvmPlugin && HeaderPlugin

  override def trigger = allRequirements

  override def buildSettings: Seq[Setting[_]] =
    Seq(
      repoOrganization := "ypg-data",
      repoName := (name in ThisBuild).value
    )

  override def projectSettings: Seq[Setting[_]] =
    Seq(
      licenses += (("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),
      headers := Map("scala" -> Apache2_0(year, "Mediative")),
      commands += Command.command("bootstrap-project") { state =>
        state.log.info("Writing LICENSE file...")
        IO.download(url("http://www.apache.org/licenses/LICENSE-2.0.txt"), file("LICENSE"))

        state.log.info("Writing CONTRIBUTING.md file...")
        IO.write(file("CONTRIBUTING.md"), contributing(repoOrganization.value, repoName.value))
        state
      }
    ) ++
    addCommandAlias("update-license", ";compile:createHeaders;test:createHeaders")

  def year = {
    import java.util.Calendar
    Calendar.getInstance.get(Calendar.YEAR).toString
  }

  def contributing(organization: String, project: String) =
    s"""# Contributing
       |
       |Bugs and feature requests should be reported in the [GitHub issue
       |tracker](https://github.com/$organization/$project/issues/new) and
       |answer the following questions:
       |
       | - Motivation: Why should this be addressed? What is the purpose?
       | - Input: What are the pre-conditions?
       | - Output: What is the expected outcome after the issue has been addressed?
       | - Test: How can the results listed in the "Output" be QA'ed?
       |
       |For code contributions, these are the suggested steps:
       |
       | - Identify the change you'd like to make, e.g. fix a bug or add a feature.
       |   Larger contributions should always begin with [first creating an
       |   issue](https://github.com/$organization/$project/issues/new) to ensure
       |   that the change is properly scoped.
       | - Fork the repository on GitHub.
       | - Develop your change on a feature branch.
       | - Write tests to validate your change works as expected.
       | - Create a pull request.
       | - Address any issues raised during the code review.
       | - Once you get a "+1" on the pull request, the change can be merged.
       |""".stripMargin.getBytes("UTF-8")

}
