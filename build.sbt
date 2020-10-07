name := "AzureDataPlatform"

version := "0.1"

scalaVersion := "2.12.11"

libraryDependencies ++= Seq(
  "org.scalactic" % "scalactic_2.12" % "3.2.0",
  "org.scalatest" %% "scalatest-funspec" % "3.2.0" % "test",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scalatest" %% "scalatest-shouldmatchers" % "3.2.0" % "test",
  "com.typesafe.play" %% "play-json" % "2.8.1",
  "com.syedatifakhtar.scalaterraform" %% "scalaterraform" %"0.3-SNAPSHOT",
  "com.syedatifakhtar.simplepipelinedsl" %% "simplepipelinedsl" %"0.1-SNAPSHOT",
  "com.typesafe" % "config" % "1.4.0"
)

run := Defaults.runTask(fullClasspath in Runtime, mainClass in run in Compile, runner in run).evaluated