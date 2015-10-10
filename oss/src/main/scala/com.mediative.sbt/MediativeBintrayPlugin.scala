package com.mediative.sbt

import scala.language.postfixOps
import sbt._
import sbt.Keys._
import bintray.BintrayPlugin.autoImport._
import MediativeProjectPlugin.autoImport._

/**
 * Configures publishing to Bintray.
 */
object MediativeBintrayPlugin extends AutoPlugin {

  val BintrayMavenRepository = "maven"

  override def requires = plugins.JvmPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    bintrayRepository := BintrayMavenRepository,
    bintrayOrganization := Some(repoOrganization.value),
    publishArtifact in Test := false,
    publishMavenStyle := bintrayRepository.value == BintrayMavenRepository
  )

}
