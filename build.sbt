
name := "MorgarothServer"

version := "1.0"

scalaVersion := "2.12.2"

val selenium = "3.3.1"
val akka = "2.4.17"
val akkaHttp = "10.0.6"

val AkkaActor = "com.typesafe.akka" %% "akka-actor" % akka
val AkkaStream = "com.typesafe.akka" %% "akka-stream" % akka

val commonSettings = Seq(
  scalaVersion := "2.12.2",
  clippyColorsEnabled := true,
  libraryDependencies ++= Seq(
    AkkaActor, Joda.Time.last, Joda.Convert.last
  )
)

val base = project.settings(commonSettings: _*)

val misc = project.settings(commonSettings: _*).dependsOn(base % "compile")

lazy val GPBettingLeagueMacros = project.settings(commonSettings: _*).settings(
  libraryDependencies ++= Seq(
    "com.propensive" %% "contextual" % "1.0.0"
  ),
  name := "GP Betting League Macros",
  scalaVersion := "2.12.2",
  version := "1.0",
  publish := {},
  publishLocal := {}
)

lazy val PhotoManager = project.settings(commonSettings: _*)
  .dependsOn(base % "compile")
  .settings(
    libraryDependencies ++= Seq(
      AkkaStream
    )
  )

lazy val SpotifyRipper = project.settings(commonSettings: _*)
  .dependsOn(base % "compile")

lazy val GPBettingLeague = project.settings(commonSettings: _*)
  .dependsOn(GPBettingLeagueMacros, base % "compile")
  .settings(
    libraryDependencies ++= Seq(
      "org.seleniumhq.selenium" % "selenium-java" % selenium,
      "org.seleniumhq.selenium" % "selenium-chrome-driver" % selenium,
      "org.seleniumhq.selenium" % "selenium-support" % selenium,
      "ru.yandex.qatools.ashot" % "ashot" % "1.5.3",
      "commons-io" % "commons-io" % "2.5"
    ),
    mappings in(Compile, packageBin) ++= mappings.in(GPBettingLeagueMacros, Compile, packageBin).value,
    mappings in(Compile, packageSrc) ++= mappings.in(GPBettingLeagueMacros, Compile, packageSrc).value,
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
  .dependsOn(GPBettingLeagueMacros, GPBettingLeague % "compile", PhotoManager % "compile").settings(
  resolvers += Resolver.bintrayRepo("hseeberger", "maven"),
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-http" % akkaHttp,
    "de.heikoseeberger" %% "akka-http-json4s" % "1.15.0",
    "org.json4s" %% "json4s-native" % "3.5.1",
    "ch.megard" %% "akka-http-cors" % "0.1.11"
  )
)

val app = project.settings(commonSettings: _*)
  .dependsOn(HTTPRpcServer % "compile", base % "compile", misc % "compile")
  .settings(
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.1.6",
      "com.typesafe.akka" %% "akka-slf4j" % akka
    )
  )

val root = (project in file(".")).settings(commonSettings: _*)
  .dependsOn(app % "compile").aggregate(app)
  .enablePlugins(JavaAppPackaging, WindowsPlugin).settings(
  maintainer := "Mateusz Jaje <mateuszjaje@gmail.com",
  mainClass in Compile := Some("io.github.morgaroth.app.App"),
  packageSummary := "GPBettingLeague",
  packageDescription := "Automate app to bets on GP Betting League"
)