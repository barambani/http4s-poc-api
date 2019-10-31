package external

import cats.instances.int._
import cats.instances.list._
import cats.instances.option._
import cats.syntax.eq._
import external.library.newtype
import org.scalacheck.Prop.forAll
import org.scalacheck.Properties

final class NewtypeTests extends Properties("newtype") {
  property("unMk gives the original value") = forAll { i: Int =>
    newtype[Int](i).unMk === i
  }

  property("unMkF gives the original values in F[_] for List") = forAll { xs: List[Int] =>
    val nt = newtype[Int]
    nt.unMkF(nt.mkF(xs)) === xs
  }

  property("unMkF gives the original values in F[_] for Option") = forAll { os: Option[Int] =>
    val nt = newtype[Int]
    nt.unMkF(nt.mkF(os)) === os
  }
}
