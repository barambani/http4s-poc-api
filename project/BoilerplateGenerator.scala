package BoilerplateGeneration

import sbt._

/**
  * This code is inspired by the Boilerplate generator of typelevel.cats.
  * It can be found at the following link
  *
  * https://github.com/typelevel/cats/blob/master/project/Boilerplate.scala
  */
final case class BoilerplateGenerator(private val templates: Seq[Template]) {

  /**
    * Function to generate the files. It also writes them to disk
    * as a side effect
    *
    * @return a sequence of generated files
    */
  def run: Int => File => Seq[File] =
    maxArity =>
      root =>
        templates map { t =>
          val file = t.moduleFile(root)
          IO.write(file, t.expandTo(maxArity))
          file
    }
}

private[BoilerplateGeneration] trait Template {
  def moduleFile: File => File
  def expandTo: Int => String
}

final private[BoilerplateGeneration] case class BlockMembersExpansions(private val upToArity: Int) {

  lazy val arityS = upToArity.toString

  lazy val `sym A0..An-1`       = arityRange map (n => s"A$n")
  lazy val `sym fa0..fan-1`     = arityRange map (n => s"fa$n")
  lazy val `sym a0..an-1`       = arityRange map (n => s"a$n")
  lazy val `sym F[A0]..F[An-1]` = arityRange map (n => s"F[A$n]")
  lazy val `sym _1.._n`         = arityRange map (n => s"_${n + 1}")
  lazy val `sym _, ... , _`     = List.fill(upToArity)("_")

  lazy val `A0..An-1`       = `sym A0..An-1` mkString ", "
  lazy val `a0..an-1`       = `sym a0..an-1` mkString ", "
  lazy val `fa0..fan-1`     = `sym fa0..fan-1` mkString ", "
  lazy val `F[A0]..F[An-1]` = `sym F[A0]..F[An-1]` mkString ", "

  lazy val `(_, ... , _)` =
    `sym _, ... , _` mkString ("(", ", ", ")")

  lazy val `fa0: =>F[A0]..fan-1: =>F[An-1]` =
    `sym fa0..fan-1` zip `sym F[A0]..F[An-1]` map { case (fa, f) => s"$fa: =>$f" } mkString ", "

  def leftAssociativeExpansionOf: Seq[String] => String => String => String =
    symbols =>
      prefix =>
        separator =>
          if (symbols.size < 2) ""
          else
            symbols
              .drop(2)
              .foldLeft(s"(${symbols.head}$separator${symbols.drop(1).head})")(
                (exp, an) => s"($prefix$exp$separator$an)"
        )

  def rightAssociativeExpansionOf: Seq[String] => String => String =
    symbols =>
      prefix =>
        if (symbols.size <= 2) ""
        else
          symbols
            .dropRight(2)
            .foldRight(s"(${symbols.dropRight(1).last}, ${symbols.last})")((an, exp) => s"($an, $prefix$exp)")

  def rightAssociativeWithSuffixExpansionOf: Seq[String] => String => String => String =
    symbols =>
      prefix =>
        suffix =>
          if (symbols.size <= 2) ""
          else
            symbols
              .dropRight(2)
              .foldRight(s"(${symbols.dropRight(1).last}, ${symbols.last}$suffix)")(
                (an, exp) => s"($an, $prefix$exp$suffix)"
        )

  private[BoilerplateGeneration] def arityRange: Range = 0 until upToArity
}

private[BoilerplateGeneration] object BlockSyntax {

  import scala.StringContext._

  implicit final class BlockOps(private val sc: StringContext) extends AnyVal {

    def static(args: String*): String =
      trimLines(args) mkString "\n"

    private def trimLines(args: Seq[String]): Array[String] = {

      val interpolated = sc.standardInterpolator(treatEscapes, args)
      val rawLines     = interpolated split '\n'

      rawLines map {
        _ dropWhile (_.isWhitespace)
      }
    }
  }

  implicit final class BlockExpansionOps(private val f: Int => String) extends AnyVal {
    def expandedTo(maxArity: Int, skip: Int): String =
      (1 + skip to maxArity) map f mkString "\n"
  }
}
