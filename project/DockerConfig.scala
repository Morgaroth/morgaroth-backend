import com.typesafe.sbt.packager.docker
import com.typesafe.sbt.packager.docker.DockerPlugin.UnixSeparatorChar
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}
import sbt.SettingsDefinition

object DockerConfig {

  val ENV_JAVA_VERSION_MAJOR = 8
  val JAVA_VERSION_MINOR = 131
  val JAVA_VERSION_BUILD = 11
  val JAVA_HASH = "d54c1d3a095b4ff2b6607d096fa80163"
  val JAVA_PACKAGE = "server-jre"
  val JAVA_JCE = "standard"
  val JAVA_HOME = "/opt/jdk"
  val PATH = "${PATH}:/opt/jdk/bin"
  val GLIBC_VERSION = "2.25-r0"
  val LANG = "C.UTF-8"

  //  def simpleCmd(str:String)= ExecCmd(str.split(" "): _*)

  val settings: Seq[SettingsDefinition] = Seq.empty
  
  val dockerSettings: Seq[SettingsDefinition] = Seq(
    dockerCommands := Seq(
      Cmd("FROM alpine:latest"),
      Cmd("MAINTAINER mateuszjaje@gmail.com"),

      Cmd("RUN apk add --update bash && rm -rf /var/cache/apk/*"),
      Cmd("RUN apk add --update eudev && rm -rf /var/cache/apk/*"),
      Cmd("WORKDIR /app"),
      Cmd("EXPOSE 8080"),
      ExecCmd("./bin/morgarothserver")
    ),
    dockerBaseImage := "alpine:latest",
    dockerExposedPorts += 8080
  )

}
