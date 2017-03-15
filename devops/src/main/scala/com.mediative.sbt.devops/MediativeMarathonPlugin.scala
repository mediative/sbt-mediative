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
import io.github.jonas.sbt.dcos.DCOSPlugin
import io.github.jonas.sbt.dcos.DCOSPlugin.autoImport._

import MediativeDeployPlugin.autoImport._

/**
 * Deploy apps to Marathon.
 *
 * To use add the following lines to the project definition:
 * {{{
 * enablePlugins(MediativeMarathonPlugin)
 * }}}
 *
 * @see [[MediativeDeployPlugin]] for how to customize publishing of artifacts.
 *
 * This plugin must be enabled.
 */
object MediativeMarathonPlugin extends AutoPlugin {

  override def requires = MediativeDeployPlugin && DCOSPlugin
  override def projectSettings: Seq[Setting[_]] = Def.settings(
    // To bring in sbt-dcos
    resolvers += Resolver.url("fonseca:sbt-plugins", url("https://dl.bintray.com/fonseca/sbt-plugins"))(Resolver.ivyStylePatterns),
    marathonSettings(DeployEnvironment.Production),
    marathonSettings(DeployEnvironment.QA),
    marathonSettings(DeployEnvironment.Staging)
  )

  def marathonSettings(env: Configuration) =
    inConfig(env)(Seq(
      deployTemplate := ConfigFactory.empty,
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
      envVars in deploy := Map.empty,
      deploy := {
        val _ = (publish in deploy).value

        def marathon(args: String*): ProcessBuilder = {
          Process(Seq(dcosCli.value.getAbsolutePath, "marathon") ++ args.toSeq, None, (envVars in deploy).value.toSeq: _*)
        }

        def toJson(config: ConfigObject): String = {
          val opts = ConfigRenderOptions.defaults.setJson(true).setOriginComments(false).setComments(false)
          config.render(opts)
        }

        val appConfig = ConfigFactory.parseFile(baseDirectory.value / "src/main/resources/application.conf")
          .withFallback(ConfigFactory.parseFile(baseDirectory.value / s"src/main/resources/$env.conf"))
          .withFallback(ConfigFactory.parseString("marathon {}"))
          .getConfig("marathon")

        val buildConf = appConfig
          .withFallback(deployConfig.value)
          .withFallback(ConfigFactory.parseString(s"""
            name = "${name.value}"
            version = "${version.value}"
            deploy.environment = "$env"
            developer.email = "${developers.value.headOption.map(_.email).getOrElse("")}"
            developer.name = "${developers.value.headOption.map(_.name).getOrElse("")}"
          """))

        val appId = buildConf.getString("name")
        val json = toJson(deployTemplate.value.resolveWith(buildConf).root)
        val inputStream = new java.io.ByteArrayInputStream(json.getBytes("UTF-8"))

        streams.value.log.info(s"Deploying ${name.value} to $env ...")
        streams.value.log.info(json)
        if (marathon("task", "list", appId).!!.contains(name.value))
          marathon("app", "update", "--force", appId) #< inputStream ! streams.value.log
        else
          marathon("app", "add") #< inputStream ! streams.value.log
        ()
      }
    ))
}
