organization := "jkvoulgaridis"
name := "crr-lib"
version := "0.1.0"
scalaVersion := "2.12.10"

githubOwner := "jkvoulgaridis"
githubRepository := "crr-library"

import Dependencies._

ThisBuild / scalaVersion     := "2.12.3"
libraryDependencies += scalaTest % Test
libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2"
libraryDependencies += "io.spray" %%  "spray-json" % "1.3.6"
libraryDependencies +=  "org.web3j" % "core" % "4.8.4"
libraryDependencies += "org.scalaj" %% "scalaj-http" % "2.3.0"
libraryDependencies += "org.bouncycastle" % "bcpkix-jdk15on" % "1.58"
libraryDependencies += "com.typesafe.play" %% "play" % "2.8.7"
libraryDependencies +=  "io.reactivex.rxjava3" % "rxjava" % "3.0.11"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"

val json4sJackson = "org.json4s" %% "json4s-jackson" % "{latestVersion}"
