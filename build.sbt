lazy val commonSettings = Seq(
   organization := "org.bustos",
   version := "0.1.0",
   scalaVersion := "2.11.4"
)

lazy val common = (project in file("."))
   .settings(name := "realityballCommon")
   .settings(commonSettings: _*)
   .settings(libraryDependencies ++= projectLibraries)


val slf4jV = "1.7.6"
val sprayV = "1.3.1"

val projectLibraries = Seq(
  "com.typesafe.akka"       %% "akka-actor"           % "2.3.6",
  "com.typesafe.slick"      %% "slick"                % "2.1.0",
  "org.seleniumhq.selenium" %  "selenium-java"        % "2.35.0",
  "org.scalatest"           %% "scalatest"            % "2.1.6",
  "io.spray"                %% "spray-can"            % sprayV,
  "io.spray"                %% "spray-routing"        % sprayV,
  "io.spray"                %% "spray-json"           % sprayV,
  "mysql"                   %  "mysql-connector-java" % "latest.release",
  "log4j"                   %  "log4j"                % "1.2.14",
  "org.slf4j"               %  "slf4j-api"            % slf4jV,
  "org.slf4j"               %  "slf4j-log4j12"        % slf4jV,
  "joda-time"               %  "joda-time"            % "2.7",
  "org.joda"                %  "joda-convert"         % "1.2"
)