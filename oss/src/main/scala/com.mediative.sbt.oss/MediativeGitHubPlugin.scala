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

package com.mediative.sbt.oss

import scala.language.postfixOps
import sbt._
import sbt.Keys._
import com.typesafe.sbt.site.SitePlugin
import com.typesafe.sbt.sbtghpages.GhpagesPlugin
import sbtunidoc.ScalaUnidocPlugin
import com.mediative.sbt.MediativeReleasePlugin

import sbtunidoc.BaseUnidocPlugin.autoImport.unidoc
import sbtrelease.ReleasePlugin.autoImport.releaseStepTask
import com.typesafe.sbt.GitPlugin.autoImport.git

import ScalaUnidocPlugin.autoImport.ScalaUnidoc
import SitePlugin.autoImport._
import MediativeProjectPlugin.autoImport._
import MediativeReleasePlugin.autoImport.postReleaseSteps

/**
 * Project-wide GitHub related settings, such as SCM and developer info,
 * as well as publishing of site and scaladoc to the project's GitHub Pages.
 *
 * To use add the following lines to the sbt root project:
 * {{{
 * .enablePlugins(MediativeGitHubPlugin)
 * }}}
 *
 * Project-wide settings such as `homepage`, `scmInfo` and `developers` are
 * scoped `in ThisBuild`. By default, it uses the
 * [[MediativeProjectPlugin.autoImport.repoOrganization]] setting as the GitHub
 * organization name.
 *
 * Scaladoc is generated using sbt-unidoc which is published to GitHub Pages
 * using the sbt-site plugin. Thus any additional site content will automatically
 * be generated and published.
 *
 * This plugin must be enabled on the sbt root project **only**.
 */
object MediativeGitHubPlugin extends AutoPlugin {

  override def requires = MediativeReleasePlugin && SitePlugin && GhpagesPlugin && ScalaUnidocPlugin

  override def projectSettings: Seq[Setting[_]] =
    Seq(
      homepage in ThisBuild := Some(url(s"https://github.com/${repoOrganization.value}/${repoName.value}")),
      git.remoteRepo := s"git@github.com:${repoOrganization.value}/${repoName.value}.git",
      siteSubdirName in ScalaUnidoc := "api",
      addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), siteSubdirName in ScalaUnidoc),
      scalacOptions in (ScalaUnidoc, unidoc) ++= Seq(
        "-groups",
        "-implicits",
        "-doc-source-url", (scmInfo in ThisBuild).value.get.browseUrl + "/tree/master€{FILE_PATH}.scala",
        "-sourcepath", baseDirectory.in(LocalRootProject).value.getAbsolutePath
      ),
      apiURL in ThisBuild := Some(url(s"https://${repoOrganization.value}.github.io/${repoName.value}/api/")),
      autoAPIMappings in ThisBuild := true,
      postReleaseSteps += releaseStepTask(GhpagesPlugin.autoImport.ghpagesPushSite),
      scmInfo in ThisBuild := Some(ScmInfo(
        url(s"https://github.com/${repoOrganization.value}/${repoName.value}"),
        s"scm:git:${git.remoteRepo.value}"
      )),
      developers in ThisBuild := List(
        Developer(repoOrganization.value, "Developers", "", url(s"https://github.com/${repoOrganization.value}"))
      )
    )

}
