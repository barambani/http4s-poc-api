package codecs

import java.time.Instant

import cats.syntax.either._
import io.circe.{Decoder, Encoder}
import shapeless.tag
import shapeless.tag.@@

object ServiceCodec {

  implicit val instantEncoder: Encoder[Instant] =
    stringEncoder(_.toString)

  implicit val instantDecoder: Decoder[Instant] =
    stringDecoder(Instant.parse)

  implicit def taggedStringEncoder[T]: Encoder[String @@ T] =
    stringEncoder(identity)

  implicit def taggedBigDecimalEncoder[T]: Encoder[BigDecimal @@ T] =
    stringEncoder(_.toString)

  implicit def taggedLongDecoder[T]: Decoder[Long @@ T] =
    stringDecoderMap(_.toLong)(tag[T].apply)


  private def stringEncoder[A](f: A => String): Encoder[A] =
    Encoder.encodeString.contramap[A](f)

  private def stringDecoderMap[A, B](f: String => A)(g: A => B): Decoder[B] =
    Decoder.decodeString emap {
      str => Either.catchNonFatal(f(str)) leftMap (_ => s"Cannot parse $str to Long") map g
    }

  private def stringDecoder[A, B](f: String => A): Decoder[A] =
    stringDecoderMap(f)(identity)
}