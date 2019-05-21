package BoilerplateGeneration

import BlockSyntax._
import sbt._

private[BoilerplateGeneration] object ParallelEffectArityFunctions extends Template {

  def moduleFile: File => File =
    _ / "ParallelEffectfulArityFunctions.scala"

  def expandTo: Int => String =
    maxArity => {

      val top =
        static"""package external
          |package library
          |
          |import scala.concurrent.duration.FiniteDuration
          |
          |private[library] trait ParallelEffectArityFunctions {
          |
          |  def parallelMap2[F[_], A1, A2, R](fa1: =>F[A1], fa2: =>F[A2])(t: FiniteDuration)(f: (A1, A2) => R)(implicit ev: ParallelEffect[F]): F[R] =
          |    ev.parallelMap2(fa1, fa2)(t)(f)
          |
          |  def parallelRun2[F[_], A1, A2](fa1: =>F[A1], fa2: =>F[A2])(t: FiniteDuration)(implicit ev: ParallelEffect[F]): F[(A1, A2)] =
          |    ev.parallelMap2(fa1, fa2)(t)(Tuple2.apply)"""

      def arityBlock: Int => String =
        arity => {

          val expansion = BlockMembersExpansions(arity)
          import expansion._

          lazy val `(a0..(a1..(an-2, an-1)` =
            rightAssociativeExpansionOf(`sym a0..an-1`)("")

          lazy val `(a0..ParallelEffect.parallelMap2(a1..ParallelEffect.parallelRun2(an-2, an-1))` =
            rightAssociativeWithSuffixExpansionOf(`sym fa0..fan-1`)("ParallelEffect.parallelRun2")(")(t")

          static"""
             |  def parallelMap$arityS[F[_] : ParallelEffect, ${`A0..An-1`}, R](${`fa0: =>F[A0]..fan-1: =>F[An-1]`})(t: FiniteDuration)(f: (${`A0..An-1`}) => R): F[R] =
             |    ParallelEffect.parallelMap2${`(a0..ParallelEffect.parallelMap2(a1..ParallelEffect.parallelRun2(an-2, an-1))`} { case ${`(a0..(a1..(an-2, an-1)`} => f(${`a0..an-1`}) }
             |
             |  def parallelRun$arityS[F[_] : ParallelEffect, ${`A0..An-1`}](${`fa0: =>F[A0]..fan-1: =>F[An-1]`})(t: FiniteDuration): F[(${`A0..An-1`})] =
             |    ParallelEffect.parallelMap2${`(a0..ParallelEffect.parallelMap2(a1..ParallelEffect.parallelRun2(an-2, an-1))`} { case ${`(a0..(a1..(an-2, an-1)`} => Tuple$arityS.apply(${`a0..an-1`}) }""".stripMargin
        }

      val bottom = static"""}"""

      s"""$top
         |${arityBlock.expandedTo(maxArity, skip = 2)}
         |$bottom""".stripMargin
    }
}