import java.time.Instant

import BoilerplateGeneration.{ ArityFunctionsGenerator, ArityTestsGenerator }
import ScalacOptions._
import sbt.Keys.javaOptions

lazy val versionOf = new {
  val cats              = "2.0.0-M4"
  val catsEffect        = "2.0.0-M4"
  val circe             = "0.11.1"
  val fs2               = "1.0.5"
  val http4s            = "0.20.3"
  val kindProjector     = "0.9.10"
  val `log-effect`      = "0.8.0"
  val `logback-classic` = "1.2.3"
  val monix             = "3.0.0-RC1"
  val scalaCheck        = "1.14.0"
  val scalaTest         = "3.0.8"
  val scalaz            = "7.3.0-M27"
  val shapeless         = "2.3.3"
  val silencer          = "1.4.1"
}

/*
 * Transitive dependencies to exclude
 */
lazy val transitiveDependencies: Seq[ExclusionRule] = Seq(
  ("org.typelevel", "cats-core_2.12"),
  ("org.typelevel", "cats-effect_2.12"),
  ("io.circe", "circe-generic_2.12"),
  ("io.circe", "circe-literal_2.12"),
  ("org.scalaz", "scalaz-concurrent_2.12")
) map (ex => ExclusionRule(ex._1, ex._2))

/*
 * Dependencies
 */
val externalDependencies = Seq(
  "org.typelevel"   %% "cats-core"           % versionOf.cats,
  "org.typelevel"   %% "cats-kernel"         % versionOf.cats,
  "org.typelevel"   %% "cats-effect"         % versionOf.catsEffect,
  "co.fs2"          %% "fs2-core"            % versionOf.fs2,
  "io.monix"        %% "monix-eval"          % versionOf.monix,
  "io.monix"        %% "monix-execution"     % versionOf.monix,
  "org.scalaz"      %% "scalaz-concurrent"   % versionOf.scalaz,
  "org.scalaz"      %% "scalaz-core"         % versionOf.scalaz,
  "org.http4s"      %% "http4s-core"         % versionOf.http4s,
  "org.http4s"      %% "http4s-server"       % versionOf.http4s,
  "org.http4s"      %% "http4s-dsl"          % versionOf.http4s excludeAll (transitiveDependencies: _*),
  "org.http4s"      %% "http4s-blaze-server" % versionOf.http4s,
  "org.http4s"      %% "http4s-circe"        % versionOf.http4s,
  "io.circe"        %% "circe-core"          % versionOf.circe,
  "io.circe"        %% "circe-generic"       % versionOf.circe,
  "com.chuusai"     %% "shapeless"           % versionOf.shapeless,
  "io.laserdisc"    %% "log-effect-core"     % versionOf.`log-effect`,
  "io.laserdisc"    %% "log-effect-fs2"      % versionOf.`log-effect`,
  "ch.qos.logback"  % "logback-classic"      % versionOf.`logback-classic`,
  "com.github.ghik" %% "silencer-lib"        % versionOf.silencer
) map (_.withSources)

/*
 * Test dependencies
 */
val testDependencies = Seq(
  "org.scalacheck" %% "scalacheck"          % versionOf.scalaCheck % "test",
  "org.scalatest"  %% "scalatest"           % versionOf.scalaTest  % "test",
  "org.typelevel"  %% "cats-testkit"        % versionOf.cats       % "test",
  "org.typelevel"  %% "cats-effect-laws"    % versionOf.catsEffect % "test",
  "org.http4s"     %% "http4s-blaze-client" % versionOf.http4s     % "test"
) map (_.withSources)

/*
 * Compiler plugins
 */
val compilerPlugins: Seq[ModuleID] = Seq(
  compilerPlugin("org.spire-math"  %% "kind-projector"  % versionOf.kindProjector),
  compilerPlugin("com.github.ghik" %% "silencer-plugin" % versionOf.silencer)
)

val buildInfoSettings = Seq(
  buildInfoPackage := "server",
  buildInfoKeys    := Seq[BuildInfoKey](name, version, scalaVersion, scalaOrganization),
  buildInfoKeys    += BuildInfoKey.action("buildTime") { Instant.now }
)

val dockerSettings = Seq(
  maintainer      in Docker := "barambani -> https://github.com/barambani",
  dockerBaseImage in Docker := "hirokimatsumoto/alpine-openjdk-11"
)

lazy val jreRuntimeOptions = Seq(
  "-J-Xmx512m",
  "-J-Xms512m",
  "-J-XX:+UseG1GC",
  "-J-XX:MaxGCPauseMillis=300",
  "-J-XX:InitiatingHeapOccupancyPercent=35",
  "-J-verbosegc",
  "-J-Xloggc:/catalog-streams/gc/log/catalog-streams-gc.log",
  "-J-XX:+PrintGCDetails",
  "-J-XX:+PrintTenuringDistribution",
  "-J-XX:+PrintGCDateStamps",
  "-J-XX:+UseGCLogFileRotation",
  "-J-XX:GCLogFileSize=10m",
  "-J-XX:NumberOfGCLogFiles=10"
)

val root = project
  .in(file("."))
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .settings(buildInfoSettings)
  .settings(
    sourceGenerators in Compile += ((sourceManaged in Compile) map ArityFunctionsGenerator.run(22)).taskValue,
    sourceGenerators in Test    += ((sourceManaged in Test) map ArityTestsGenerator.run(8)).taskValue
  )
  .settings(
    name                := "http4s-poc-api",
    organization        := "com.github.barambani",
    scalaVersion        := "2.12.8",
    libraryDependencies ++= externalDependencies ++ testDependencies ++ compilerPlugins,
    addCommandAlias("format", ";scalafmt;test:scalafmt;scalafmtSbt"),
    addCommandAlias(
      "checkFormat",
      ";scalafmtCheck;test:scalafmtCheck;scalafmtSbtCheck"
    ),
    addCommandAlias(
      "fullBuild",
      ";checkFormat;unusedCompileDependenciesTest;undeclaredCompileDependenciesTest;clean;coverage;test;coverageReport;coverageAggregate"
    ),
    addCommandAlias(
      "fullCiBuild",
      ";set scalacOptions in ThisBuild ++= Seq(\"-opt:l:inline\", \"-opt-inline-from:**\");fullBuild"
    ),
    scalacOptions ++= generalOptions,
    scalacOptions in Test ++= testOnlyOptions,
    scalacOptions in (Compile, console) --= nonTestExceptions,
    javaOptions   in Universal := jreRuntimeOptions,
    cancelable    in Global := true
  )
