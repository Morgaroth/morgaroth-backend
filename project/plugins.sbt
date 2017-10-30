resolvers += Resolver.bintrayIvyRepo("morgaroth", "sbt-plugins")

addSbtPlugin("io.github.morgaroth" % "sbt-commons" % "0.17")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.0")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.0")

addSbtPlugin("com.softwaremill.clippy" % "plugin-sbt" % "0.5.3")

addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-RC12")