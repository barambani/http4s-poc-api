import sbt._

object Dependencies {

  private val catsVersion       = "1.0.1"
  private val catsEffectVersion = "0.8"
  private val http4sVersion     = "0.18.0-M8"
  private val circeVersion      = "0.9.0"
  private val scalaCheckVersion = "1.13.5"

  val externalDependencies = Seq(
    "org.typelevel"       %% "cats-core"            % catsVersion withSources(),
    "org.typelevel"       %% "cats-effect"          % catsEffectVersion withSources(),
    "org.http4s"          %% "http4s-dsl"           % http4sVersion withSources(),
    "org.http4s"          %% "http4s-blaze-server"  % http4sVersion withSources(),
    "org.http4s"          %% "http4s-blaze-client"  % http4sVersion withSources(),
    "org.http4s"          %% "http4s-circe"         % http4sVersion withSources(),
    "io.circe"            %% "circe-generic"        % circeVersion withSources(),
    "io.circe"            %% "circe-literal"        % circeVersion withSources(),
    "org.scalacheck"      %% "scalacheck"           % scalaCheckVersion % "test" withSources()
  )
}