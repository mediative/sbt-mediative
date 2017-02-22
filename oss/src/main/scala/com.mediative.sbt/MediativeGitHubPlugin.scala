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
import com.typesafe.sbt.GitPlugin
import com.typesafe.sbt.site.SitePlugin
import com.typesafe.sbt.sbtghpages.GhpagesPlugin
import sbtunidoc.ScalaUnidocPlugin

import sbtrelease.ReleasePlugin.autoImport._
import GitPlugin.autoImport.git
import ScalaUnidocPlugin.autoImport._
import SitePlugin.autoImport._
import MediativeProjectPlugin.autoImport._
import MediativeReleasePlugin.autoImport._

/**
 * Configures the project's GitHub pages using the SBT site plugin.
 *
 * Must be enabled on the sbt root project.
 */
object MediativeGitHubPlugin extends AutoPlugin {

  override def requires = MediativeReleasePlugin && SitePlugin && GhpagesPlugin && ScalaUnidocPlugin

  override def projectSettings: Seq[Setting[_]] =
    Seq(
      homepage in ThisBuild := Some(url(s"https://github.com/${repoOrganization.value}/${repoName.value}")),
      git.remoteRepo := s"git@github.com:${repoOrganization.value}/${repoName.value}.git",
      siteSubdirName in ScalaUnidoc := "api",
      addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), siteSubdirName in ScalaUnidoc),
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
