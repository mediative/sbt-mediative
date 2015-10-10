package com.mediative.sbt

import scala.language.postfixOps
import sbt._
import sbt.Keys._
import com.typesafe.sbt.GitPlugin
import com.typesafe.sbt.SbtGhPages
import com.typesafe.sbt.SbtSite.site

import sbtrelease.ReleasePlugin.autoImport._
import GitPlugin.autoImport.git
import MediativeProjectPlugin.autoImport._

/**
 * Configures the project's GitHub pages using the SBT site plugin.
 */
object MediativeGitHubPlugin extends AutoPlugin {

  override def requires = sbt.plugins.JvmPlugin

  override def projectSettings: Seq[Setting[_]] =
    site.settings ++
    site.includeScaladoc("api") ++
    SbtGhPages.ghpages.settings ++
    Seq(
      homepage := Some(url(s"https://github.com/${repoOrganization.value}/${name.in(ThisBuild).value}")),
      git.remoteRepo := s"git@github.com:${repoOrganization.value}/${name.in(ThisBuild).value}.git",
      apiURL := Some(url(s"https://${repoOrganization.value}.github.io/${name.in(ThisBuild).value}/api/")),
      autoAPIMappings := true,
      postReleaseSteps := Seq(releaseStepTask(SbtGhPages.GhPagesKeys.pushSite))
    )

}
