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
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.dockerAlias
import com.typesafe.sbt.packager.Keys.packageName
import com.typesafe.config._
import scalaj.http._

import MediativeDeployPlugin.autoImport._

/**
 * Deploy jobs to Chronos.
 *
 * To use add the following lines to the project definition:
 * {{{
 * enablePlugins(MediativeChronosPlugin)
 * // Configure credentials for each environment
 * credentials in DeployEnvironment.QA += Credentials(baseDirectory.value / "...")
 * credentials in DeployEnvironment.Production += Credentials(baseDirectory.value / "...")
 * }}}
 *
 * Example credentials file:
 * <pre>
 * realm=DC/OS
 * host=https://dcos.example.com/service/chronos/v1
 * user=<...>
 * password=<...>
 * </pre>
 *
 * @see [[MediativeDeployPlugin]] for how to customize publishing of artifacts.
 *
 * This plugin must be enabled.
 */
object MediativeChronosPlugin extends AutoPlugin {

  override def requires = MediativeDeployPlugin
  override def projectSettings: Seq[Setting[_]] = Def.settings(
    chronosSettings(DeployEnvironment.Production),
    chronosSettings(DeployEnvironment.QA),
    chronosSettings(DeployEnvironment.Staging)
  )

  def chronosSettings(env: Configuration) =
    inConfig(env)(Seq(
      deployTemplate := ConfigFactory.parseResources(getClass, "chronos-template.conf"),
      deployConfig := {
        val dockerImage = dockerAlias.value.copy(tag = Some((version in deploy).value)).versioned

        ConfigFactory.parseString(s"""
          docker.image = "${dockerImage}"
        """)
      },
      version in deploy := {
        env match {
          case DeployEnvironment.Production => version.value
          case _ => s"$env-latest"
        }
      },
      publish in deploy := publishDockerImage.value,
      deploy := {
        val _ = (publish in deploy).value
        val chronos = Credentials.toDirect(credentials.value.head)

        def http(server: DirectCredentials, path: String, transform: HttpRequest => HttpRequest = identity): String = {
          def auth(request: HttpRequest): HttpRequest = {
            if (chronos.userName.isEmpty) request.header("Authorization", s"token=${server.passwd}")
            else request.auth(server.userName, server.passwd)
          }

          val request = Http(server.host + path)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")

          try transform(auth(request)).asString.throwError.body catch {
            case e: Throwable =>
              streams.value.log.error(s"Request failed for $request")
              throw e
          }
        }

        def toJson(config: ConfigObject): String = {
          val opts = ConfigRenderOptions.defaults.setJson(true).setOriginComments(false).setComments(false)
          config.render(opts)
        }

        val jobConfig = ConfigFactory.parseFile(baseDirectory.value / "src/main/resources/application.conf")
          .withFallback(ConfigFactory.parseFile(baseDirectory.value / s"src/main/resources/$env.conf"))
          .withFallback(ConfigFactory.parseString("job {}"))
          .getConfig("job")

        val buildConf = jobConfig
          .withFallback(deployConfig.value)
          .withFallback(ConfigFactory.parseString(s"""
            name = "${name.value}"
            version = "${version.value}"
            deploy.environment = "$env"
            developer.email = "${developers.value.headOption.map(_.email).getOrElse("")}"
            developer.name = "${developers.value.headOption.map(_.name).getOrElse("")}"
          """))

        val currentJson = http(chronos, s"/scheduler/jobs/search?name=${buildConf.getString("name")}")
        val currentConf = ConfigFactory.parseString(s"{ current = $currentJson }").getObjectList("current") match {
          case list if list.isEmpty => ConfigFactory.empty.root
          case list => list.get(0)
        }

        val template =
          if (buildConf.hasPath("docker.image"))
            deployTemplate.value
          else
            deployTemplate.value.withoutPath("container")

        val nextConf = template
          .resolveWith(buildConf)
          .withFallback(currentConf)
        val json = toJson(nextConf.root)

        if (json == toJson(currentConf)) {
          streams.value.log.info(s"Job ${name.value} is up-to-date in (${chronos.realm}) ${chronos.host} ...")
          streams.value.log.info(json)
        } else {
          streams.value.log.info(s"Deploying ${name.value} to (${chronos.realm}) ${chronos.host} ...")
          streams.value.log.info(json)
          http(chronos, "/scheduler/iso8601", _.postData(json))
          streams.value.log.info("Job deployed")
        }
      }
    ))
}
