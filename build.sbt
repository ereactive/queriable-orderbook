name := "queriable_orderbook"

val akkaVersion = "2.5.13"

val circeVersion = "0.9.3"

val logbackV = "1.1.3"

val akkaV = "2.5.13"

val akkaHttpV = "10.1.3"

val enumeratumVersion = "1.5.13"

val root = project
  .in(file("."))
  .settings(
    organization := "com.ereactive",
    scalaVersion := "2.12.6",
    scalacOptions in Compile ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint"),
    javacOptions in Compile ++= Seq("-Xlint:unchecked", "-Xlint:deprecation"),
    javaOptions in run ++= Seq("-Xms128m", "-Xmx1024m"),
    libraryDependencies ++= Seq(
       "org.scalatest" %% "scalatest" % "3.0.1" % Test
      ,"com.typesafe.akka" %% "akka-stream" % akkaVersion
      ,"com.typesafe.akka" %% "akka-http"   % akkaHttpV
      ,"ch.qos.logback" % "logback-classic" % logbackV
      ,"com.typesafe.akka" %% "akka-slf4j" % akkaV
      ,"com.beachape" %% "enumeratum" % enumeratumVersion)
      ++ Seq(
        "io.circe" %% "circe-core",
        "io.circe" %% "circe-generic",
        "io.circe" %% "circe-optics",
        "io.circe" %% "circe-parser"
      ).map(_ % circeVersion)
      ,fork in run := true
      // disable parallel tests
      ,parallelExecution in Test := false
      ,licenses := Seq(("CC0", url("http://creativecommons.org/publicdomain/zero/1.0")))
  )