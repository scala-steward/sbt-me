ThisBuild / scalaVersion := "2.12.10"
ThisBuild / name         := "sbt-me"

enablePlugins(SbtPlugin, MdocPlugin)

mdocVariables := Map("VERSION" -> version.value.replaceAll("\\+.*", ""))

addSbtPlugin("com.geirsson"              % "sbt-ci-release" % "1.4.31")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat"   % "0.1.8")

libraryDependencies += "io.circe"   %% "circe-generic" % "0.12.3"
libraryDependencies += "io.circe"   %% "circe-parser"  % "0.12.3"
libraryDependencies += "org.scalaj" %% "scalaj-http"   % "2.4.2"
