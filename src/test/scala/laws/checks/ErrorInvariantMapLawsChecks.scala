package laws.checks

import cats.Eq
import cats.laws.discipline._
import http4s.extend.ErrorInvariantMap
import http4s.extend.laws.ErrorInvariantMapLaws
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Prop}
import org.typelevel.discipline.Laws

trait ErrorInvariantMapLawsChecks[E1, E2] extends Laws {

  def laws: ErrorInvariantMapLaws[E1, E2]

  def errorInvariantMap(
    implicit
      AE1:  Arbitrary[E1],
      AE2:  Arbitrary[E2],
      EqE1: Eq[E1],
      EqE2: Eq[E2]): RuleSet =
    new RuleSet {

      def name: String = "errorInvariantMap"
      def bases: Seq[(String, RuleSet)] = Nil
      def parents: Seq[RuleSet] = Nil

      def props: Seq[(String, Prop)] = Seq(
        "directIdentity"  -> forAll(laws.directIdentity _),
        "reverseIdentity" -> forAll(laws.reverseIdentity _)
      )
    }
}

object ErrorInvariantMapLawsChecks {

  @inline def apply[E1, E2](implicit FE: ErrorInvariantMap[E1, E2]): ErrorInvariantMapLawsChecks[E1, E2] =
    new ErrorInvariantMapLawsChecks[E1, E2] {
      def laws: ErrorInvariantMapLaws[E1, E2] = ErrorInvariantMapLaws[E1, E2]
    }
}
