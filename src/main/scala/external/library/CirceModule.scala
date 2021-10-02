package external
package library

import cats.Show
import cats.syntax.either._
import io.circe.{Decoder, Encoder}

object CirceModule {

  /**
   * Gives a Circe Encoder for the type `A` when a `Show` instance is available for it.
   *
   * Example, Shapeless tag:
   *
   * def taggedLongEncoder[T]: Encoder[Long @@ T] = encoderFor[Long @@ T]
   *
   * def taggedStringEncoder[T]: Encoder[String @@ T] = encoderFor[String @@ T]
   *
   * def taggedBigDecimalEncoder[T]: Encoder[BigDecimal @@ T] = encoderFor[BigDecimal @@ T]
   *
   * @return
   *   An encoder for `A`
   */
  def encoderFor[A](implicit ev: Show[A]): Encoder[A] =
    Encoder.encodeString.contramap[A](ev.show)

  /**
   * Gives a Circe Encoder for the type `A` when way to go from A to string `A => String` is given
   *
   * Example, Instant:
   *
   * val instantEncoder: Encoder[Instant] = encoderFor[Instant]
   *
   * @return
   *   An encoder for `A`
   */
  def encoderFor[A]: (A => String) => Encoder[A] =
    f => Encoder.encodeString.contramap[A](f)

  /**
   * Gives a Circe Decoder for the type `A` when a way to go from String to `A` is provided
   *
   * Example, Instant:
   *
   * val instantDecoder: Decoder[Instant] = decoderFor(Instant.parse)
   *
   * @return
   *   A decoder for `A`
   */
  def decoderFor[A]: (String => A) => Decoder[A] =
    f => mappedDecoderFor(f)(identity)

  /**
   * Gives a Circe Decoder for `A` that maps the successful decoded value with `f`
   *
   * Example, Shapeless tag:
   *
   * def taggedLongDecoder[T]: Decoder[Long @@ T] = mappedDecoderFor(_.toLong)(tag[T].apply)
   *
   * def taggedBigDecimalDecoder[T]: Decoder[BigDecimal @@ T] =
   * mappedDecoderFor(BigDecimal.apply)(tag[T].apply)
   *
   * def taggedStringDecoder[T]: Decoder[String @@ T] = mappedDecoderFor(identity)(tag[T].apply)
   *
   * @return
   *   A decoder for `A` that maps the result to `B` in case of successful decoding
   */
  def mappedDecoderFor[A, B]: (String => A) => (A => B) => Decoder[B] =
    ff =>
      f =>
        Decoder.decodeString emap { str =>
          Either.catchNonFatal[A](ff(str)) leftMap (_ => s"Cannot parse $str to Long") map f
        }
}
