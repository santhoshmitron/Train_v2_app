name := "jtrack"

version := "1.0"

organization in ThisBuild := "jtrack"

scalaVersion := "2.12.5"

import Versions._

crossPaths := false
resolvers += Resolver.sonatypeRepo("snapshots")

Revolver.settings

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")
javacOptions in Compile += "-parameters" // This is needed for jackson-module-parameter-names.
javacOptions in(Compile, compile) ++= Seq("-source", "8",
  "-target", "8",
  "-g:lines")

artifactName := {
  (sv: ScalaVersion, module: ModuleID, artifact: Artifact) => {
    import artifact._
    import java.text.SimpleDateFormat
    import java.util.TimeZone
    import java.util.Date
    val dateFormat = { val df = new SimpleDateFormat("yyyyMMdd");
      df.setTimeZone(TimeZone.getTimeZone("IST")); df}
    val timestamp = dateFormat.format(new Date())
    name + "_" + timestamp + "." + artifact.extension
  }
}

libraryDependencies ++= Seq(
  // SQUBS DEPENDENCIES
  "ch.qos.logback" % "logback-classic" % Versions.logbackClassicV,
  "org.squbs" %% "squbs-unicomplex" % Versions.squbsV,
  "org.squbs" %% "squbs-actormonitor" % Versions.squbsV,
  "org.squbs" %% "squbs-actorregistry" % Versions.squbsV,
  "org.squbs" %% "squbs-httpclient" % Versions.squbsV,
  "org.squbs" %% "squbs-pattern" % Versions.squbsV,
  "org.squbs" %% "squbs-admin" % Versions.squbsV,
  "org.squbs" %% "squbs-zkcluster" % Versions.squbsV,
  "mysql" % "mysql-connector-java" % "8.0.33",
 "org.springframework" % "spring-jdbc" % "5.1.3.RELEASE",

// AKKA Dependencies
  "com.typesafe.akka" %% "akka-persistence" % "2.5.13",
  "com.typesafe.akka" %% "akka-cluster-sharding" % "2.5.13",
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.13",
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.13",
  "com.github.levkhomich" %% "akka-tracing-core" % "0.6",
  "com.google.code.gson" % "gson" % "2.8.2",
  "redis.clients" % "jedis" % "4.3.1",

  //"jio.rtrs" %% "pigeon_core" % "1.0",
  "org.apache.commons" % "commons-csv" % "1.5",
  "commons-io" % "commons-io" % "2.5",

  // Mustache
  "com.github.spullara.mustache.java" % "scala-extensions-2.11" % "0.9.5",
  "com.github.spullara.mustache.java" % "compiler" % "0.9.5"
)

mainClass in (Compile, run) := Some("org.squbs.unicomplex.Bootstrap")

// enable scalastyle on compile
lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")

compileScalastyle := scalastyle.in(Compile).toTask("").value

(compile in Compile) := ((compile in Compile) dependsOn compileScalastyle).value

enablePlugins(PackPlugin)

packMain := Map("run" -> "org.squbs.unicomplex.Bootstrap")

enablePlugins(DockerPlugin)

dockerfile in docker := {
  val jarFile: File = sbt.Keys.`package`.in(Compile, packageBin).value
  val classpath = (managedClasspath in Compile).value
  val mainclass = "org.squbs.unicomplex.Bootstrap"
  val jarTarget = s"/app/${jarFile.getName}"
  // Make a colon separated classpath with the JAR file
  val classpathString = classpath.files.map("/app/" + _.getName)
    .mkString(":") + ":" + jarTarget
  new Dockerfile {
    // Base image
    from("java")
    // Add all files on the classpath
    add(classpath.files, "/app/")
    // Add the JAR file
    add(jarFile, jarTarget)
    // On launch run Java with the classpath and the main class
    entryPoint("java", "-cp", classpathString, mainclass)
  }
}
