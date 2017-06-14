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
    resolvers += Resolver.bintrayIvyRepo("fonseca", "sbt-plugins"),
    marathonSettings(DeployEnvironment.Production),
    marathonSettings(DeployEnvironment.QA),
    marathonSettings(DeployEnvironment.Staging)
  )

  def marathonSettings(env: Configuration) =
    MediativeDeployPlugin.deploySettings(env, "marathon") ++
    inConfig(env)(Seq(
      deploy := {
        val _ = (publish in deploy).value
        val appId = deployConfig.value.getString("name")
        val json = deployJson.value
        val inputStream = new java.io.ByteArrayInputStream(json.getBytes("UTF-8"))

        def marathon(args: String*): ProcessBuilder = {
          Process(Seq(dcosCli.value.getAbsolutePath, "marathon") ++ args.toSeq, None, (envVars in deploy).value.toSeq: _*)
        }

        streams.value.log.info(s"Deploying ${name.value} as $appId to $env ...")
        streams.value.log.info(json)
        if (marathon("task", "list", appId).!!.contains(name.value))
          marathon("app", "update", "--force", appId) #< inputStream ! streams.value.log
        else
          marathon("app", "add") #< inputStream ! streams.value.log
        ()
      }
    ))
}
