package model

import java.time.Instant

import http4s.extend.util.CirceModule._
import io.circe.{Decoder, Encoder}
import shapeless.tag
import shapeless.tag.@@

object DomainModelCodecs {

  /**
    * Encoders
    */
  implicit val instantEncoder: Encoder[Instant] =
    encoderFor(_.toString)

  implicit def taggedStringEncoder[T]: Encoder[String @@ T] =
    encoderFor(identity)

  implicit def taggedBigDecimalEncoder[T]: Encoder[BigDecimal @@ T] =
    encoderFor(_.toString)

  implicit def taggedLongEncoder[T]: Encoder[Long @@ T] =
    encoderFor(_.toString)

  /**
    * Decoders
    */
  implicit val instantDecoder: Decoder[Instant] =
    decoderFor(Instant.parse)

  implicit def taggedLongDecoder[T]: Decoder[Long @@ T] =
    decoderMapFor(_.toLong)(tag[T].apply)

  implicit def taggedBigDecimalDecoder[T]: Decoder[BigDecimal @@ T] =
    decoderMapFor(BigDecimal.apply)(tag[T].apply)

  implicit def taggedStringDecoder[T]: Decoder[String @@ T] =
    decoderMapFor(identity)(tag[T].apply)
}