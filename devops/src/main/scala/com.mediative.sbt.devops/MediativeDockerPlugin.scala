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
import com.typesafe.sbt.packager.linux.LinuxPlugin.autoImport.daemonUser
import com.typesafe.sbt.packager.docker.{ Cmd, ExecCmd, DockerPlugin }
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging

/**
 * Package application using the smaller Alpine JDK Docker image.
 *
 * It ensures that the Alpine Docker is compatible with the default base image
 * used by sbt-native-packager. It also filters out default commands which increase
 * the image size.
 */
object MediativeDockerPlugin extends AutoPlugin {

  import DockerPlugin.autoImport._

  val AlpineDockerImage = "openjdk:8-jdk-alpine"
  val AlpineCompatCommands = List(
    "apk add --update bash && rm -rf /var/cache/apk/*",
    "mkdir -p /opt",
    // XXX: Workaround to make Snappy work with musl libc used by Alpine
    "ln /lib/ld-musl-x86_64.so.1 /lib/ld-linux-x86-64.so.2"
  )

  override def requires = plugins.JvmPlugin && DockerPlugin && JavaAppPackaging

  override def projectSettings: Seq[Setting[_]] = Seq(
    dockerBaseImage := AlpineDockerImage,
    dockerCommands := {
      // Filter out `USER` and `chown` RUN commands to reduce image size
      val filteredCommands = dockerCommands.value.filterNot {
        case ExecCmd("RUN", args @ _*) => args.contains("chown")
        case Cmd("USER", args @ _*)    => true
        case cmd                       => false
      }

      (dockerBaseImage.value, filteredCommands) match {
        case (AlpineDockerImage, fromImage :: restOfCommands) =>
          fromImage :: AlpineCompatCommands.map(Cmd("RUN", _)) ++ restOfCommands
        case _ =>
          filteredCommands
      }
    },
    daemonUser in Docker := "root",
    version in Docker := {
      if (version.value matches ".*-g[0-9a-f]{4,}(-SNAPSHOT)?")
        "qa-latest"
      else
        version.value
    }
  )

}
