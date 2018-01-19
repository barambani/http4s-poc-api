import Dependencies._
import ScalacOptions._

val typelevelOrganization = "org.typelevel"
val scala_typelevel_212   = "2.12.4-bin-typelevel-4"

val root = project.in(file("."))
  .settings(
    version                 :=  "0.0.1",
    name                    :=  "http4s-poc-api",
    scalaOrganization       :=  typelevelOrganization,
    scalaVersion            :=  scala_typelevel_212,
    libraryDependencies     ++= externalDependencies,
    libraryDependencies     ++= compilerPlugins,
    scalacOptions           ++= generalOptions ++ typeLevelScalaOptions,
    scalacOptions in Test   ++= testOnlyOptions,
    scalacOptions in (Compile, console) --= nonTestExceptions,
    resolvers               +=  Resolver.sonatypeRepo("releases")
  )