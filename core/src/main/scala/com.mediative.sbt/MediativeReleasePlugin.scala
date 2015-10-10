package com.mediative.sbt

import scala.language.postfixOps
import sbt._
import sbt.Keys._
import sbtrelease.ReleasePlugin

import ReleasePlugin.autoImport._
import ReleaseTransformations._
import ReleaseKeys._

/*
 * Customized release process that reads the release version from
 * the command line via `-Dversion=x.y.z` and extends the publishing
 * step to also update of API docs hosted on the project's GitHub
 * pages.
 *
 * Compared to the default sbt-release process, all steps that
 * update and commit the version file before and after tagging the
 * release have been removed, since for this project SBT is
 * configured to read the project version from Git.
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
