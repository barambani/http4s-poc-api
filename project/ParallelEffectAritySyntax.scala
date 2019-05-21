package BoilerplateGeneration

import BoilerplateGeneration.BlockSyntax._
import sbt._

private[BoilerplateGeneration] object ParallelEffectAritySyntax extends Template {
  def moduleFile: File => File =
    _ / "syntax" / "ParallelEffectAritySyntax.scala"

  def expandTo: Int => String =
    maxArity => {

      val syntaxTraitTop =
        static"""package external
          |package library
          |package syntax
          |
          |import scala.concurrent.duration.FiniteDuration
          |import scala.language.implicitConversions
          |
          |private[syntax] trait ParallelEffectAritySyntax {"""
      
      def syntaxArityBlock: Int => String =
        arity => {

          val expansion = BlockMembersExpansions(arity)
          import expansion._

          static"""|  implicit def parEffectfulSyntax$arityS[F[_], ${`A0..An-1`}](t$arityS: (${`F[A0]..F[An-1]`})) = new Tuple${arityS}ParallelEffectOps(t$arityS)""".stripMargin
        }

      val syntaxTraitBottom = static"""}"""

      def opsArityBlock: Int => String =
        arity => {

          val expansion = BlockMembersExpansions(arity)
          import expansion._

          lazy val `t._1..t._n` =
            List.fill(arity)(s"t$arityS") zip `sym _1.._n` map { case (t, n) => s"$t.$n" } mkString ", "

          static"""
            |private[syntax] final class Tuple${arityS}ParallelEffectOps[F[_], ${`A0..An-1`}](t$arityS: (${`F[A0]..F[An-1]`})) {
            |  def parallelMap[R](t: FiniteDuration)(f: (${`A0..An-1`}) => R)(implicit F: ParallelEffect[F]): F[R] = ParallelEffect.parallelMap$arityS(${`t._1..t._n`})(t)(f)
            |  def parallelRun(t: FiniteDuration)(implicit F: ParallelEffect[F]): F[(${`A0..An-1`})] = ParallelEffect.parallelRun$arityS(${`t._1..t._n`})(t)
            |}""".stripMargin
        }

      s"""$syntaxTraitTop
         |${syntaxArityBlock.expandedTo(maxArity, skip = 1)}
         |$syntaxTraitBottom
         |${opsArityBlock.expandedTo(maxArity, skip = 1)}""".stripMargin
    }
}
