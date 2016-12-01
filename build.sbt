name := "akka-typed-tcp-pingpong"

version := "1.0"

scalaVersion := "2.12.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.14",
  "com.typesafe.akka" %% "akka-typed-experimental" % "2.4.14"
)
