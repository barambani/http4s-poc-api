import sbt.librarymanagement.syntax.ExclusionRule
import sbt.{ModuleID, compilerPlugin, _}

object Dependencies {

  /*
   * Versions
   */
  private object versionOf {
    val cats         = "1.1.0"
    val catsEffect   = "0.10"
    val monix        = "3.0.0-RC1"
    val scalaz       = "7.2.21"
    val http4s       = "0.18.7"
    val circe        = "0.9.2"
    val http4sExtend = "0.0.26"

    val scalaCheck   = "1.13.5"
    val scalaTest    = "3.0.5"

    val kindProjector = "0.9.6"
  }

  /*
   * Transitive dependencies to exclude
   */
  private val transitiveDependencies: Seq[ExclusionRule] = Seq(
    ("org.typelevel", "cats-core_2.12"),
    ("org.typelevel", "cats-effect_2.12"),
    ("io.circe"     , "circe-generic_2.12"),
    ("io.circe"     , "circe-literal_2.12"),
    ("org.scalaz"   , "scalaz-concurrent_2.12")
  ) map (ex => ExclusionRule(ex._1, ex._2))

  /*
   * Dependencies and compiler plugins
   */
  val externalDependencies = Seq(
    "org.typelevel"         %% "cats-core"            % versionOf.cats         withSources(),
    "org.typelevel"         %% "cats-effect"          % versionOf.catsEffect   withSources(),
    "io.monix"              %% "monix"                % versionOf.monix        excludeAll(transitiveDependencies:_*) withSources(),
    "org.scalaz"            %% "scalaz-concurrent"    % versionOf.scalaz       withSources(),
    "org.http4s"            %% "http4s-dsl"           % versionOf.http4s       excludeAll(transitiveDependencies:_*) withSources(),
    "org.http4s"            %% "http4s-blaze-server"  % versionOf.http4s       withSources(),
    "org.http4s"            %% "http4s-blaze-client"  % versionOf.http4s       withSources(),
    "org.http4s"            %% "http4s-circe"         % versionOf.http4s       withSources(),
    "io.circe"              %% "circe-generic"        % versionOf.circe        withSources(),
    "io.circe"              %% "circe-literal"        % versionOf.circe        withSources(),
    "com.github.barambani"  %% "http4s-extend"        % versionOf.http4sExtend excludeAll(transitiveDependencies:_*) withSources()
  )

  val testDependencies = Seq(
    "org.scalacheck"  %% "scalacheck"       % versionOf.scalaCheck % "test" withSources(),
    "org.scalatest"   %% "scalatest"        % versionOf.scalaTest  % "test" withSources(),
    "org.typelevel"   %% "cats-testkit"     % versionOf.cats       % "test" withSources(),
    "org.typelevel"   %% "cats-effect-laws" % versionOf.catsEffect % "test" withSources()
  )

  val compilerPlugins: Seq[ModuleID] = Seq(
    compilerPlugin("org.spire-math" %% "kind-projector" % versionOf.kindProjector)
  )
}