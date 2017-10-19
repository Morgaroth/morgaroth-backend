import sbt.Keys.mappings

name := "MorgarothServer"

version := "1.0.1"

scalaVersion := "2.12.2"

val selenium = "3.3.1"
val akka = "2.4.17"
val akkaHttp = "10.0.6"
val betterFilesVer = "3.0.0"

val AkkaActor = "com.typesafe.akka" %% "akka-actor" % akka
val AkkaStream = "com.typesafe.akka" %% "akka-stream" % akka
val AkkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttp
val AkkaTestKit = "com.typesafe.akka" %% "akka-testkit" % akka % "test"
val Json4s = "org.json4s" %% "json4s-native" % "3.5.1"
val ScalaTest = "org.scalatest" %% "scalatest" % "3.0.1" % "test"
val MongoDriver = "org.reactivemongo" %% "reactivemongo" % "0.12.3"
val BetterFiles = "com.github.pathikrit" %% "better-files" % betterFilesVer

lazy val macros = project.settings(
  libraryDependencies ++= Seq(
    "com.propensive" %% "contextual" % "1.0.0",
    ScalaTest
  ),
  scalaVersion := "2.12.2",
  version := "1.0",
  publish := {},
  publishLocal := {}
)

//val base = project.settings(commonSettings: _*)
//  .dependsOn(macros % "compile")
//  .settings(
//    libraryDependencies ++= Seq(
//      Json4s, ScalaTest
//    ),
//    mappings in(Compile, packageBin) ++= mappings.in(macros, Compile, packageBin).value,
//    mappings in(Compile, packageSrc) ++= mappings.in(macros, Compile, packageSrc).value
//  )
//
//val misc = project.settings(commonSettings: _*).dependsOn(base % "compile")
//  .settings(
//    libraryDependencies ++= Seq(
//      ScalaTest, AkkaTestKit,
//      "com.github.alonsodomin.cron4s" %% "cron4s-joda" % "0.4.0"
//    )
//  )
//
//lazy val PhotoManager = project.settings(commonSettings: _*)
//  .dependsOn(base % "compile")
//  .settings(
//    libraryDependencies ++= Seq(
//      AkkaStream, BetterFiles, ScalaTest
//    )
//  )
//
//lazy val SpotifyManager = project.settings(commonSettings: _*)
//  .dependsOn(base % "compile")
//  .settings(
//    libraryDependencies += BetterFiles
//  )
//
//lazy val mongo = project.settings(commonSettings: _*)
//  .dependsOn(base % "compile")
//  .settings(
//    libraryDependencies ++= Seq(
//      MongoDriver, ScalaTest,
//      "com.whisk" %% "docker-testkit-scalatest" % "0.9.0" % "test",
//      "com.whisk" %% "docker-testkit-impl-docker-java" % "0.9.0" % "test",
//      "org.mongodb.scala" %% "mongo-scala-driver" % "2.1.0"
//    )
//  )
//
//lazy val GPBettingLeague = project.settings(commonSettings: _*)
//  .dependsOn(macros, base % "compile")
//  .settings(
//    libraryDependencies ++= Seq(
//      Ficus.ficusNewLibrary("ficus", "1.4.0"),
//      "org.seleniumhq.selenium" % "selenium-java" % selenium,
//      "org.seleniumhq.selenium" % "selenium-chrome-driver" % selenium,
//      "org.seleniumhq.selenium" % "selenium-support" % selenium,
//      "ru.yandex.qatools.ashot" % "ashot" % "1.5.3",
//      "commons-io" % "commons-io" % "2.5",
//      ScalaTest
//    ),
//    mappings in(Compile, packageBin) ++= mappings.in(macros, Compile, packageBin).value,
//    mappings in(Compile, packageSrc) ++= mappings.in(macros, Compile, packageSrc).value,
//    name := "GP Betting League",
//    version := "1.0",
//    initialCommands +=
//      """
//        |import org.openqa.selenium.WebDriver
//        |import org.openqa.selenium.chrome.ChromeDriver
//        |import org.openqa.selenium.Cookie
//        |import io.github.morgaroth.gpbettingleague.Driver
//        |import io.github.morgaroth.base.UserCredentials
//        |import org.joda.time.{DateTime, LocalTime}
//        |
//        |import io.github.morgaroth.gpbettingleague._
//        |import xpath._
//        |
//        |System.setProperty("webdriver.chrome.driver","/mnt/prv-projects/chromedriver")
//        |implicit val driver: WebDriver = new ChromeDriver()
//        |
//        |val seleniumHelpers = new Selenium {}
//        |import seleniumHelpers._
//      """.stripMargin
//  )

//val HTTPRpcServer = project.settings(commonSettings: _*)
//  .dependsOn(macros % "compile", base % "compile")
//  .settings(
//    resolvers += Resolver.bintrayRepo("hseeberger", "maven"),
//    libraryDependencies ++= Seq(
//      Json4s, AkkaHttp,
//      "de.heikoseeberger" %% "akka-http-json4s" % "1.15.0",
//      "ch.megard" %% "akka-http-cors" % "0.1.11"
//    )
//  )
//
//val TelegramBot = project.settings(commonSettings: _*)
//  .dependsOn(base % "compile")
//  .settings(
//    resolvers += Resolver.bintrayRepo("hseeberger", "maven"),
//    libraryDependencies ++= Seq(
//      Json4s, AkkaHttp, Ficus.ficusNewLibrary("ficus", "1.4.0"),
//      "de.heikoseeberger" %% "akka-http-json4s" % "1.15.0"
//    )
//  )
//
//val app = project.settings(commonSettings: _*)
//  .dependsOn(List(HTTPRpcServer, misc, mongo, SpotifyManager, GPBettingLeague, PhotoManager, TelegramBot).map(dep): _*)
//  .settings(
//    libraryDependencies ++= Seq(
//      "ch.qos.logback" % "logback-classic" % "1.1.6",
//      "com.typesafe.akka" %% "akka-slf4j" % akka,
//      "org.reflections" % "reflections" % "0.9.11"
//    )
//  )


lazy val deploy = taskKey[Unit]("Deploy to docker.")

val root = (project in file("."))
  .dependsOn(macros % "compile")
  .enablePlugins(JavaServerAppPackaging, DockerPlugin)
  .settings(
    scalaVersion := "2.12.2",
    clippyColorsEnabled := true,
    resolvers += Resolver.bintrayRepo("hseeberger", "maven"),
    libraryDependencies ++= Seq(
      Json4s,
      AkkaActor,
      Joda.Time.last,
      Joda.Convert.last,
      AkkaHttp,
      BetterFiles,
      MongoDriver,
      Ficus.ficusNewLibrary("ficus", "1.4.0"),
      "ch.qos.logback" % "logback-classic" % "1.1.6",
      "com.typesafe.akka" %% "akka-slf4j" % akka,
      "de.heikoseeberger" %% "akka-http-json4s" % "1.15.0",
      "ch.megard" %% "akka-http-cors" % "0.1.11",
      "org.reflections" % "reflections" % "0.9.11",
      "de.heikoseeberger" %% "akka-http-json4s" % "1.15.0",
      "org.seleniumhq.selenium" % "selenium-java" % selenium,
      "org.seleniumhq.selenium" % "selenium-chrome-driver" % selenium,
      "org.seleniumhq.selenium" % "selenium-support" % selenium,
      "ru.yandex.qatools.ashot" % "ashot" % "1.5.3",
      "commons-io" % "commons-io" % "2.5",
      "com.whisk" %% "docker-testkit-scalatest" % "0.9.0" % "test",
      "com.whisk" %% "docker-testkit-impl-docker-java" % "0.9.0" % "test",
      "org.mongodb.scala" %% "mongo-scala-driver" % "2.1.0",
      "com.github.alonsodomin.cron4s" %% "cron4s-joda" % "0.4.0"
    ),
    deploy := {
      (publishLocal in Docker).toTask.value
      Seq("./deploy_docker.sh", version.value) !
    },
    maintainer := "Mateusz Jaje <mateuszjaje@gmail.com",
    mainClass in Compile := Some("io.github.morgaroth.app.App"),
    mappings in(Compile, packageBin) ++= mappings.in(macros, Compile, packageBin).value,
    mappings in(Compile, packageSrc) ++= mappings.in(macros, Compile, packageSrc).value
  ).settings(DockerConfig.settings: _*)