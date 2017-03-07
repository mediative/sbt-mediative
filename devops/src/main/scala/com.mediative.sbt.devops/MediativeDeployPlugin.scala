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

package com.mediative.sbt.devops

import sbt._
import sbt.Keys._
import com.typesafe.sbt.packager.docker.DockerPlugin
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import com.typesafe.sbt.packager.Keys.packageName
import com.typesafe.config._
import scalaj.http._

/**
 * Utilities for deploying apps and jobs.
 *
 * This plugin is automatically enabled.
 */
object MediativeDeployPlugin extends AutoPlugin {

  def docker(log: Logger, args: String*): Unit = {
    Process("docker" +: args.toSeq) ! log
    ()
  }

  object autoImport {
    val deploy = taskKey[Unit]("Deploy a job or an application")
    val deployTemplate = settingKey[Config]("Template for deploying job or application")
    val deployConfig = taskKey[Config]("Configuration for generating the job or application template")

    object DeployEnvironment {
      val Production = config("prod")
      val QA = config("qa")
      val Staging = config("stg")
    }

    /**
     * Promote a Docker image from one environment to another.
     *
     * To promote from QA (using the `qa-latest` Docker tag) to production:
     * {{{
     * .settings(
     *   deployPreparation in DeployEnvironment.Production := promoteDockerImage(DeployEnvironment.QA)
     * )
     * }}}
     *
     * The Docker tag of the promoted image is read from the `BUILD_NUMBER`
     * environment variable if it exists. Else the project version is used.
     *
     * @param latest the Docker tag from which to promote, e.g. `qa-latest`
     */
    def promoteDockerImage(env: Configuration) =
      Def.task {
        val latest = env match {
          case DeployEnvironment.Production => sys.error(s"Cannot promote from $env")
          case _ => s"$env-latest"
        }
        val tag = (version in deploy).value
        val dockerImage = s"${dockerRepository.value.get}/${packageName.in(Docker).value}"
        val fromImage = s"$dockerImage:$latest"
        val toImage = s"$dockerImage:$tag"

        streams.value.log.info(s"Promoting $latest to $tag")
        docker(streams.value.log, "pull", fromImage)
        docker(streams.value.log, "tag", fromImage, toImage)
        docker(streams.value.log, "push", toImage)
      }

    /**
     * Build and publish a Docker image.
     *
     * To deploy a newly build Docker image to QA:
     * {{{
     * .settings(
     *   deployPreparation in DeployEnvironment.QA := publishDockerImage.value
     * )
     * }}}
     */
   val publishDockerImage =
     Def.task {
       val _ = (publishLocal in Docker).value
       val tag = (version in deploy).value
       val dockerImage = s"${dockerRepository.value.get}/${packageName.in(Docker).value}:$tag"

       streams.value.log.info(s"Publishing $dockerImage")
       docker(streams.value.log, "rmi", dockerImage)
       docker(streams.value.log, "tag", (dockerTarget in Docker).value, dockerImage)
       docker(streams.value.log, "push", dockerImage)
     }
  }

  override def requires = MediativeDockerPlugin

}
