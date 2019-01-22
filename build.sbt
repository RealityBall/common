lazy val commonSettings = Seq(
   organization := "org.bustos",
   version := "0.1.0",
   scalaVersion := "2.11.7"
)

lazy val common = (project in file("."))
   .settings(name := "realityballCommon")
   .settings(commonSettings: _*)
   .settings(libraryDependencies ++= projectLibraries)

val slf4jV = "1.7.6"
val akka_http_version = "10.0.11"

val projectLibraries = Seq(
  "com.typesafe.slick"      %% "slick"                % "3.2.1",
  "com.typesafe.slick"      %% "slick-hikaricp"       % "3.2.1",
  "com.typesafe.akka"       %% "akka-actor"           % "2.4.20",
  "org.seleniumhq.selenium" %  "selenium-java"        % "2.35.0",
  "org.scalatest"           %% "scalatest"            % "3.0.1",
  "com.typesafe.akka"       %% "akka-http-core"       % akka_http_version,
  "com.typesafe.akka"       %% "akka-http"            % akka_http_version,
  "com.typesafe.akka"       %% "akka-http-spray-json" % akka_http_version,
  "mysql"                   %  "mysql-connector-java" % "5.1.23",
  "log4j"                   %  "log4j"                % "1.2.14",
  "org.slf4j"               %  "slf4j-api"            % slf4jV,
  "org.slf4j"               %  "slf4j-log4j12"        % slf4jV,
  "joda-time"               %  "joda-time"            % "2.7",
  "org.joda"                %  "joda-convert"         % "1.2"
)