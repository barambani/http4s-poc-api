import java.time.Instant

import sbt.Keys.javaOptions
import ExplicitDepsPlugin.autoImport.moduleFilterRemoveValue

lazy val versionOf = new {
  val cats               = "2.1.1"
  val catsEffect         = "2.1.2"
  val circe              = "0.13.0"
  val fs2                = "2.2.2"
  val http4s             = "0.21.1"
  val kindProjector      = "0.11.0"
  val `log-effect`       = "0.12.1"
  val `logback-classic`  = "1.2.3"
  val scalaCheck         = "1.14.3"
  val scalaTest          = "3.2.0-M4"
  val zio                = "1.0.0-RC18-2"
  val `zio-interop-cats` = "2.0.0.0-RC11"
  val shapeless          = "2.3.3"
  val silencer           = "1.6.0"
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
  "org.typelevel"   %% "cats-effect"         % versionOf.catsEffect,
  "co.fs2"          %% "fs2-core"            % versionOf.fs2,
  "org.http4s"      %% "http4s-core"         % versionOf.http4s,
  "org.http4s"      %% "http4s-server"       % versionOf.http4s,
  "org.http4s"      %% "http4s-dsl"          % versionOf.http4s excludeAll (transitiveDependencies: _*),
  "org.http4s"      %% "http4s-blaze-server" % versionOf.http4s,
  "org.http4s"      %% "http4s-circe"        % versionOf.http4s,
  "io.circe"        %% "circe-generic"       % versionOf.circe,
  "com.chuusai"     %% "shapeless"           % versionOf.shapeless,
  "io.laserdisc"    %% "log-effect-core"     % versionOf.`log-effect`,
  "io.laserdisc"    %% "log-effect-zio"      % versionOf.`log-effect`,
  "ch.qos.logback"  % "logback-classic"      % versionOf.`logback-classic`,
  "com.github.ghik" %% "silencer-lib"        % versionOf.silencer % Provided cross CrossVersion.full,
  "dev.zio"         %% "zio"                 % versionOf.zio,
  "dev.zio"         %% "zio-interop-cats"    % versionOf.`zio-interop-cats`
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
  compilerPlugin("org.typelevel"   %% "kind-projector"  % versionOf.kindProjector cross CrossVersion.full),
  compilerPlugin("com.github.ghik" %% "silencer-plugin" % versionOf.silencer cross CrossVersion.full)
)

val generalOptions: Seq[String] = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-explaintypes",
  "-Yrangepos",
  "-feature",
  "-language:higherKinds",
  "-language:existentials",
  "-unchecked",
  "-Xlint:_,-type-parameter-shadow",
  "-Xsource:2.13",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfatal-warnings",
  "-Ywarn-unused:imports",
  "-Ywarn-unused:_,imports",
  "-opt-warnings",
  "-Xlint:constant",
  "-Ywarn-extra-implicit"
)

val nonTestExceptions: Seq[String] = Seq(
  "-Ywarn-unused:imports",
  "-Xfatal-warnings"
)

val testOnlyOptions: Seq[String] = Seq("-Yrangepos")

lazy val jreRuntimeOptions = Seq(
  "-J-Xmx512m",
  "-J-Xms512m",
  "-J-XX:+UseG1GC",
  "-J-XX:MaxGCPauseMillis=300",
  "-J-XX:InitiatingHeapOccupancyPercent=35",
  "-J-verbosegc",
  "-J-Xloggc:/var/log/server-gc.log",
  "-J-XX:+PrintGCDetails",
  "-J-XX:+PrintTenuringDistribution",
  "-J-XX:+PrintGCDateStamps",
  "-J-XX:+UseGCLogFileRotation",
  "-J-XX:GCLogFileSize=10m",
  "-J-XX:NumberOfGCLogFiles=10"
)

val buildInfoSettings = Seq(
  buildInfoPackage := "server",
  buildInfoKeys    := Seq[BuildInfoKey](name, version, scalaVersion, scalaOrganization),
  buildInfoKeys    += BuildInfoKey.action("buildTime")(Instant.now)
)

val dockerSettings = Seq(
  maintainer      in Docker := "barambani -> https://github.com/barambani",
  dockerBaseImage in Docker := "hirokimatsumoto/alpine-openjdk-11"
)

val root = project
  .in(file("."))
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .settings(buildInfoSettings)
  .settings(dockerSettings)
  .settings(
    name                := "http4s-poc-api",
    organization        := "com.github.barambani",
    scalaVersion        := "2.13.1",
    libraryDependencies ++= externalDependencies ++ testDependencies ++ compilerPlugins,
    unusedCompileDependenciesFilter -= moduleFilter("ch.qos.logback", "logback-classic"),
    addCommandAlias("format", ";scalafmt;test:scalafmt;scalafmtSbt"),
    addCommandAlias(
      "checkFormat",
      ";scalafmtCheck;test:scalafmtCheck;scalafmtSbtCheck"
    ),
    addCommandAlias(
      "fullBuild",
      ";checkFormat;unusedCompileDependenciesTest;clean;coverage;test;coverageReport;coverageAggregate"
    ),
    addCommandAlias(
      "fullCiBuild",
      ";set scalacOptions in ThisBuild ++= Seq(\"-opt:l:inline\", \"-opt-inline-from:**\");fullBuild"
    ),
    scalacOptions ++= generalOptions,
    scalacOptions in Test ++= testOnlyOptions,
    scalacOptions in (Compile, console) --= nonTestExceptions,
    javaOptions   in Universal := jreRuntimeOptions,
    unusedCompileDependenciesFilter -= moduleFilter("com.github.ghik", "silencer-lib")
  )
