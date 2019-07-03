import java.time.Instant

import BoilerplateGeneration.{ ArityFunctionsGenerator, ArityTestsGenerator }
import Dependencies._
import ScalacOptions._

val buildInfoSettings = Seq(
  buildInfoPackage := "server",
  buildInfoKeys    := Seq[BuildInfoKey](name, version, scalaVersion, scalaOrganization),
  buildInfoKeys    += BuildInfoKey.action("buildTime") { Instant.now }
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
  .settings(
    sourceGenerators in Compile += ((sourceManaged in Compile) map ArityFunctionsGenerator.run(22)).taskValue,
    sourceGenerators in Test    += ((sourceManaged in Test) map ArityTestsGenerator.run(8)).taskValue
  )
  .settings(
    name                := "http4s-poc-api",
    organization        := "com.github.barambani",
    scalaVersion        := "2.12.8",
    libraryDependencies ++= externalDependencies ++ testDependencies ++ compilerPlugins,
    scalacOptions       ++= generalOptions,
    scalacOptions       in Test ++= testOnlyOptions,
    scalacOptions       in (Compile, console) --= nonTestExceptions,
    javaOptions         in Universal ++= Seq(
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
    ),
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
    )
  )
