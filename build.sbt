import java.time.Instant

import BoilerplateGeneration.{ ArityFunctionsGenerator, ArityTestsGenerator }
import Dependencies._
import ScalacOptions._

val buildInfoSettings = Seq(
  buildInfoPackage := "server",
  buildInfoKeys    := Seq[BuildInfoKey](name, version, scalaVersion, scalaOrganization),
  buildInfoKeys    += BuildInfoKey.action("buildTime") { Instant.now }
)

val root = project
  .in(file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(buildInfoSettings)
  .settings(
    sourceGenerators in Compile += ((sourceManaged in Compile) map ArityFunctionsGenerator.run(22)).taskValue,
    sourceGenerators in Test    += ((sourceManaged in Test) map ArityTestsGenerator.run(8)).taskValue
  )
  .settings(
    version             := "0.1.0",
    name                := "http4s-poc-api",
    scalaVersion        := "2.12.8",
    libraryDependencies ++= externalDependencies ++ testDependencies ++ compilerPlugins,
    scalacOptions       ++= generalOptions,
    scalacOptions       in Test ++= testOnlyOptions,
    scalacOptions       in (Compile, console) --= nonTestExceptions,
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
