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
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport.stage
import com.typesafe.sbt.packager.docker.DockerPlugin
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import com.typesafe.sbt.packager.docker.DockerPlugin.{ publishLocalDocker, publishDocker }
import com.typesafe.sbt.packager.Keys.packageName
import com.typesafe.config._
import scalaj.http._

/**
 * Utilities for deploying apps and jobs.
 *
 * This plugin is automatically enabled.
 */
object MediativeDeployPlugin extends AutoPlugin {

  object autoImport {
    val deploy = taskKey[Unit]("Deploy a job or an application")
    val deployTemplate = taskKey[Config]("Template for deploying job or application")
    val deployConfig = taskKey[Config]("Configuration for generating the job or application template")
    val deployJson = taskKey[String]("Generate a JSON descriptor by applying the deploy config to the template")

    object DeployEnvironment {
      val Production = config("prod") describedAs("Deployment to prod environment.")
      val QA = config("qa") describedAs("Deployment to QA environment.")
      val Staging = config("stg") describedAs("Deployment to staging environment.")
    }

    /**
     * Promote a Docker image from one environment to another.
     *
     * To promote from QA (using the `qa-latest` Docker tag) to production:
     * {{{
     * .settings(
     *   publish in (DeployEnvironment.Production, deploy) := promoteDockerImage(DeployEnvironment.QA)
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
          case _ => (version in (env, deploy)).value
        }
        val tag = (version in deploy).value
        val fromImage = dockerAlias.value.copy(tag = Some(latest)).versioned
        val toImage = dockerAlias.value.copy(tag = Some(tag)).versioned

        def docker(args: String*): Unit = {
          Process(dockerExecCommand.value ++ args.toSeq) ! streams.value.log
          ()
        }

        streams.value.log.info(s"Promoting $latest to $tag")
        docker("pull", fromImage)
        docker("tag", fromImage, toImage)
        publishDocker(dockerExecCommand.value, toImage, streams.value.log)
      }

    /**
     * Build and publish a Docker image.
     *
     * To deploy a newly build Docker image to QA:
     * {{{
     * .settings(
     *   publish in (DeployEnvironment.QA, deploy) := publishDockerImage.value
     * )
     * }}}
     */
   val publishDockerImage =
     Def.task {
       val tag = (version in deploy).value
       val dockerImage = dockerAlias.value.copy(tag = Some(tag)).versioned
       val log = streams.value.log

       streams.value.log.info(s"Building $tag")
       publishLocalDocker(
         (stage in Docker).value,
         dockerExecCommand.value ++ Seq("build", "--force-rm", "-t", dockerImage, "."),
         log
       )

       streams.value.log.info(s"Publishing $tag")
       publishDocker(dockerExecCommand.value, dockerImage, log)
     }
  }
  import autoImport._

  override def requires = MediativeDockerPlugin

  def toJson(config: ConfigObject): String = {
    val opts = ConfigRenderOptions.defaults.setJson(true).setOriginComments(false).setComments(false)
    config.render(opts)
  }

  def deploySettings(env: Configuration, service: String) = inConfig(env)(Seq(
    deployTemplate := ConfigFactory.empty,
    version in deploy := {
      env match {
        case DeployEnvironment.Production => version.value
        case _ => s"$env-latest"
      }
    },
    publish in deploy := publishDockerImage.value,
    envVars in deploy := Map.empty,
    deployConfig := {
      val dockerImage = dockerAlias.value.copy(tag = Some((version in deploy).value)).versioned

      def envVar(name: String) = sys.env.get(name)
      def branch = ("git symbolic-ref -q --short HEAD" #|| "git rev-parse HEAD").!!.trim
      val ciInfo =
        Seq(
          envVar("TEAMCITY_PROJECT_NAME").orElse(envVar("USER")).getOrElse(name.value),
          envVar("TEAMCITY_BUILDCONF_NAME").getOrElse(branch),
          envVar("BUILD_NUMBER").getOrElse(java.time.LocalDateTime.now.toString)
        ).mkString(":")

      ConfigFactory.parseString(s"""
          deploy.environment = "$env"
          job {
            deploy.environment = "$env"
            deploy.info = "${version.value} by $ciInfo"
          }
        """)
        .withFallback(ConfigFactory.parseFile(baseDirectory.value / s"src/main/resources/$env.conf"))
        .withFallback(ConfigFactory.parseFile(baseDirectory.value / "src/main/resources/application.conf"))
        .resolve() // Resolve before extracting the job sub-config
        .getConfig(service)
        .withFallback(ConfigFactory.parseString(s"""
          name = "${name.value}"
          version = "${version.value}"
          docker.image = "${dockerImage}"
          developer.email = "${developers.value.headOption.map(_.email).getOrElse("")}"
          developer.name = "${developers.value.headOption.map(_.name).getOrElse("")}"
        """))
        .resolve()
    },
    deployJson := toJson(deployTemplate.value.resolveWith(deployConfig.value).root)
  ))
}
