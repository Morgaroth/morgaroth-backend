import sbt.Keys.{clean, mappings}

name := "MorgarothServer"

version := "1.0"

scalaVersion := "2.12.2"

val selenium = "3.3.1"
val akka = "2.4.17"
val akkaHttp = "10.0.6"
val betterFilesVer = "3.0.0"

val AkkaActor = "com.typesafe.akka" %% "akka-actor" % akka
val AkkaStream = "com.typesafe.akka" %% "akka-stream" % akka
val Json4s = "org.json4s" %% "json4s-native" % "3.5.1"
val ScalaTest = "org.scalatest" %% "scalatest" % "3.0.1" % "test"
val MongoDriver = "org.reactivemongo" %% "reactivemongo" % "0.12.3"
val BetterFiles = "com.github.pathikrit" %% "better-files" % betterFilesVer

def dep(p: Project) = p % "compile"

val commonSettings = Seq(
  scalaVersion := "2.12.2",
  clippyColorsEnabled := true,
  libraryDependencies ++= Seq(
    AkkaActor, Joda.Time.last, Joda.Convert.last
  )
)

lazy val macros = project.settings(commonSettings: _*).settings(
  libraryDependencies ++= Seq(
    "com.propensive" %% "contextual" % "1.0.0",
    ScalaTest
  ),
  version := "1.0",
  publish := {},
  publishLocal := {}
)

val base = project.settings(commonSettings: _*)
  .dependsOn(macros % "compile")
  .settings(
    libraryDependencies ++= Seq(
      Json4s
    ),
    mappings in(Compile, packageBin) ++= mappings.in(macros, Compile, packageBin).value,
    mappings in(Compile, packageSrc) ++= mappings.in(macros, Compile, packageSrc).value
  )

val misc = project.settings(commonSettings: _*).dependsOn(base % "compile")
  .settings(
    libraryDependencies ++= Seq(
      "com.github.alonsodomin.cron4s" %% "cron4s-joda" % "0.4.0"
    )
  )

lazy val PhotoManager = project.settings(commonSettings: _*)
  .dependsOn(base % "compile")
  .settings(
    libraryDependencies ++= Seq(
      AkkaStream, BetterFiles
    )
  )

lazy val SpotifyManager = project.settings(commonSettings: _*)
  .dependsOn(base % "compile")
  .settings(
    libraryDependencies += BetterFiles
  )

lazy val mongo = project.settings(commonSettings: _*)
  .dependsOn(base % "compile")
  .settings(
    libraryDependencies ++= Seq(
      MongoDriver
    )
  )

lazy val GPBettingLeague = project.settings(commonSettings: _*)
  .dependsOn(macros, base % "compile")
  .settings(
    libraryDependencies ++= Seq(
      "org.seleniumhq.selenium" % "selenium-java" % selenium,
      "org.seleniumhq.selenium" % "selenium-chrome-driver" % selenium,
      "org.seleniumhq.selenium" % "selenium-support" % selenium,
      "ru.yandex.qatools.ashot" % "ashot" % "1.5.3",
      "commons-io" % "commons-io" % "2.5"
    ),
    mappings in(Compile, packageBin) ++= mappings.in(macros, Compile, packageBin).value,
    mappings in(Compile, packageSrc) ++= mappings.in(macros, Compile, packageSrc).value,
    name := "GP Betting League",
    version := "1.0",
    initialCommands +=
      """
        |import org.openqa.selenium.WebDriver
        |import org.openqa.selenium.chrome.ChromeDriver
        |import org.openqa.selenium.Cookie
        |import io.github.morgaroth.gpbettingleague.Driver
        |import org.joda.time.{DateTime, LocalTime}
        |
        |import io.github.morgaroth.gpbettingleague._
        |import xpath._
        |
        |implicit val driver: WebDriver = new ChromeDriver()
        |
        |val seleniumHelpers = new Selenium {}
        |import seleniumHelpers._
      """.stripMargin
  )

val HTTPRpcServer = project.settings(commonSettings: _*)
  .dependsOn(macros % "compile", base % "compile")
  .settings(
    resolvers += Resolver.bintrayRepo("hseeberger", "maven"),
    libraryDependencies ++= Seq(
      Json4s,
      "com.typesafe.akka" %% "akka-http" % akkaHttp,
      "de.heikoseeberger" %% "akka-http-json4s" % "1.15.0",
      "ch.megard" %% "akka-http-cors" % "0.1.11"
    )
  )

val app = project.settings(commonSettings: _*)
  .dependsOn(List(HTTPRpcServer, misc, mongo, SpotifyManager, GPBettingLeague, PhotoManager).map(dep): _*)
  .settings(
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.1.6",
      "com.typesafe.akka" %% "akka-slf4j" % akka,
      "org.reflections" % "reflections" % "0.9.11"
    )
  )

val root = (project in file(".")).settings(commonSettings: _*)
  .dependsOn(app % "compile").aggregate(app)
  .enablePlugins(JavaAppPackaging, WindowsPlugin, DebianPlugin).settings(
  maintainer := "Mateusz Jaje <mateuszjaje@gmail.com",
  mainClass in Compile := Some("io.github.morgaroth.app.App"),
  packageSummary := "GPBettingLeague",
  packageDescription := "Automate app to bets on GP Betting League",
  debianPackageDependencies in Debian ++= Seq("java-runtime-headless (>= 1.8)"),
  clean := {
    (clean in app).value
    (clean in base).value
    (clean in macros).value
    (clean in misc).value
    (clean in mongo).value
    (clean in SpotifyManager).value
    (clean in PhotoManager).value
    (clean in GPBettingLeague).value
    (clean in HTTPRpcServer).value
  }
)