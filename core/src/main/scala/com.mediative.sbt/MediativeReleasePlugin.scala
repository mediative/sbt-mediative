/*
 * Copyright 2017 Mediative
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
import sbtrelease.ReleasePlugin

import ReleasePlugin.autoImport._
import ReleaseTransformations._
import ReleaseKeys._

/**
 * Customizable release process which works with Git-based versioning.
 *
 * To use add the following lines to the project definition:
 * {{{
 * .enablePlugins(MediativeReleasePlugin)
 * }}}
 *
 * Configures a release process that reads the release version from
 * the command line via `-Dversion=x.y.z`. Post-release steps to be executed
 * after publishing can be added to
 * [[MediativeReleasePlugin.autoImport.postReleaseSteps]].
 *
 * Compared to the default sbt-release process, all steps that
 * update and commit the version file before and after tagging the
 * release have been removed, since this plugin assumes the project
 * has been configured configured to read the project version from Git.
 *
 * This plugin must be enabled.
 */
object MediativeReleasePlugin extends AutoPlugin {

  object autoImport {
    val postReleaseSteps = settingKey[Seq[ReleaseStep]]("Post release steps")
  }
  import autoImport._

  override def requires = ReleasePlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    releaseCrossBuild := true,
    releaseTagComment := s"Releasing ${(version in ThisBuild).value}",
    releaseTagName := (version in ThisBuild).value,
    releaseVersionFile := target.value / "unused-version.sbt",
    releaseProcess := defaultReleaseSteps ++ postReleaseSteps.value,
    postReleaseSteps := Seq.empty
  )

  def defaultReleaseSteps = Seq[ReleaseStep](
    checkSnapshotDependencies,
    { st: State =>
      val v = sys.props.get("version").getOrElse {
        st.log.error("No version specified, rerun with `-Dversion=x.y.z`")
        sys.exit(1)
      }
      st.put(versions, (v, v))
    },
    runTest,
    setReleaseVersion,
    tagRelease,
    publishArtifacts,
    pushChanges
  )

}
