package BoilerplateGeneration

import BoilerplateGeneration.BlockSyntax._
import sbt._

private[BoilerplateGeneration] object ParallelEffectSyntaxAccumulateErrorTests extends Template {

  def moduleFile: File => File =
    _ / "syntax" / "ParallelEffectSyntaxAccumulateErrorTests.scala"

  def expandTo: Int => String =
    maxArity => {

      val staticTop =
        static"""package external
          |
          |import cats.Semigroup
          |import cats.effect.IO
          |import cats.effect.laws.util.TestContext
          |import cats.effect.util.CompositeException
          |import cats.syntax.semigroup._
          |import external.library.ParallelEffect
          |import external.library.syntax.parallelEffect._
          |import org.scalacheck.Prop
          |import scalaz.concurrent.{Task => ScalazTask}
          |
          |import scala.concurrent.duration._
          |
          |final class ParallelEffectSyntaxAccumulateErrorTests extends MinimalSuite {
          |
          |  implicit val C = TestContext()
          |
          |  implicit def throwableSemigroup: Semigroup[Throwable] =
          |    new Semigroup[Throwable]{
          |      def combine(x: Throwable, y: Throwable): Throwable =
          |        CompositeException(x, y, Nil)
          |    }
          |    
          |  val timeout = 1.seconds"""

      val staticBottom = static"""}"""

      def testArityBlock: Int => String =
        arity => {

          val expansion = BlockMembersExpansions(arity)
          import expansion._

          lazy val `sym e0..en-1` = arityRange map (n => s"e$n")

          lazy val `e0: Throwable..en-1: Throwable` =
            `sym e0..en-1` map (e => s"$e: Throwable") mkString ", "

          lazy val `ioEff.fail[Int](e0)..ioEff.fail[Int](en-1)` =
            `sym e0..en-1` map (e => s"IO.raiseError($e)") mkString ", "

          lazy val `scalazTaskEff.fail[Int](e0)..scalazTaskEff.fail[Int](en-1)` =
            `sym e0..en-1` map (e => s"ScalazTask.fail[Int]($e)") mkString ", "

          lazy val `(e1 combine e2) ... combine en-1` =
            leftAssociativeExpansionOf(`sym e0..en-1`)("")(" combine ")

          lazy val `(_: Int, ... , _: Int)` =
            `sym _, ... , _` map (s => s"$s: Int") mkString ("(", ", ", ")")

          static"""
            |  test("$arityS io errors are accumulated by parallelMap") {
            |    Prop.forAll { (${`e0: Throwable..en-1: Throwable`}) => {
            |      (${`ioEff.fail[Int](e0)..ioEff.fail[Int](en-1)`}).parallelMap(timeout){ ${`(_, ... , _)`} => () } <-> IO.raiseError[Unit](${`(e1 combine e2) ... combine en-1`})
            |    }}
            |  }
            |
            |  test("$arityS scalaz task errors are accumulated by parallelMap") {
            |    Prop.forAll { (${`e0: Throwable..en-1: Throwable`}) => {
            |      (${`scalazTaskEff.fail[Int](e0)..scalazTaskEff.fail[Int](en-1)`}).parallelMap(timeout){ ${`(_: Int, ... , _: Int)`} => () } <-> ScalazTask.fail(${`(e1 combine e2) ... combine en-1`}).map((_: Int) => ())
            |    }}
            |  }""".stripMargin
        }

      s"""$staticTop
         |${testArityBlock.expandedTo(maxArity, skip = 1)}
         |$staticBottom""".stripMargin
    }
}
