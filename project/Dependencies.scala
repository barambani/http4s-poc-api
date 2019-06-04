import sbt.librarymanagement.syntax.ExclusionRule
import sbt.{ ModuleID, compilerPlugin, _ }

object Dependencies {

  /*
   * Versions
   */
  private object versionOf {
    val cats          = "1.6.1"
    val catsEffect    = "1.0.0-RC"
    val circe         = "0.11.1"
    val fs2           = "0.10.7"
    val http4s        = "0.18.23"
    val kindProjector = "0.9.10"
    val `log-effect`  = "0.7.0"
    val monix         = "3.0.0-RC1"
    val scalaCheck    = "1.14.0"
    val scalaTest     = "3.0.7"
    val scalaz        = "7.3.0-M30"
    val shapeless     = "2.3.3"
  }

  /*
   * Transitive dependencies to exclude
   */
  private val transitiveDependencies: Seq[ExclusionRule] = Seq(
    ("org.typelevel", "cats-core_2.12"),
    ("org.typelevel", "cats-effect_2.12"),
    ("io.circe", "circe-generic_2.12"),
    ("io.circe", "circe-literal_2.12"),
    ("org.scalaz", "scalaz-concurrent_2.12")
  ) map (ex => ExclusionRule(ex._1, ex._2))

  /*
   * Dependencies and compiler plugins
   */
  val externalDependencies = Seq(
    "org.typelevel" %% "cats-core"           % versionOf.cats,
    "org.typelevel" %% "cats-kernel"         % versionOf.cats,
    "org.typelevel" %% "cats-effect"         % versionOf.catsEffect,
    "co.fs2"        %% "fs2-core"            % versionOf.fs2,
    "io.monix"      %% "monix-eval"          % versionOf.monix,
    "io.monix"      %% "monix-execution"     % versionOf.monix,
    "org.scalaz"    %% "scalaz-concurrent"   % versionOf.scalaz,
    "org.scalaz"    %% "scalaz-core"         % versionOf.scalaz,
    "org.http4s"    %% "http4s-core"         % versionOf.http4s,
    "org.http4s"    %% "http4s-server"       % versionOf.http4s,
    "org.http4s"    %% "http4s-dsl"          % versionOf.http4s excludeAll (transitiveDependencies: _*),
    "org.http4s"    %% "http4s-blaze-server" % versionOf.http4s,
    "org.http4s"    %% "http4s-circe"        % versionOf.http4s,
    "io.circe"      %% "circe-core"          % versionOf.circe,
    "io.circe"      %% "circe-generic"       % versionOf.circe,
    "com.chuusai"   %% "shapeless"           % versionOf.shapeless,
//    "io.laserdisc"         %% "log-effect-core"     % versionOf.`log-effect`,
//    "io.laserdisc"         %% "log-effect-fs2"      % versionOf.`log-effect`
  ) map (_.withSources)

  val testDependencies = Seq(
    "org.scalacheck" %% "scalacheck"          % versionOf.scalaCheck % "test",
    "org.scalatest"  %% "scalatest"           % versionOf.scalaTest  % "test",
    "org.typelevel"  %% "cats-testkit"        % versionOf.cats       % "test",
    "org.typelevel"  %% "cats-effect-laws"    % versionOf.catsEffect % "test",
    "org.http4s"     %% "http4s-blaze-client" % versionOf.http4s     % "test"
  ) map (_.withSources)

  val compilerPlugins: Seq[ModuleID] = Seq(
    compilerPlugin("org.spire-math" %% "kind-projector" % versionOf.kindProjector)
  )
}
