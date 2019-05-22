import cats.data.ValidatedNel

package object syntax {
  type Verified[A] = ValidatedNel[String, A]

  object responseVerification extends ResponseVerificationSyntax
  object http4sService        extends Http4sServiceSyntax
}
