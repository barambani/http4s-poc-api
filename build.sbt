import java.time.Instant

import Dependencies._
import ScalacOptions._

val buildInfoSettings = Seq(
    buildInfoPackage    :=  "server",
    buildInfoKeys       :=  Seq[BuildInfoKey](name, version, scalaVersion, scalaOrganization),
    buildInfoKeys       +=  BuildInfoKey.action("buildTime") { Instant.now }
)

val root = project.in(file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(buildInfoSettings)
  .settings(
    version                 :=  "0.0.1",
    name                    :=  "http4s-poc-api",
    scalaVersion            :=  "2.12.7",
    libraryDependencies     ++= externalDependencies ++ testDependencies ++ compilerPlugins,
    scalacOptions           ++= generalOptions,
    scalacOptions in Test   ++= testOnlyOptions,
    scalacOptions in (Compile, console) --= nonTestExceptions
  )