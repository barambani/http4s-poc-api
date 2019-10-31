package model

import java.time.Instant

import cats.instances.bigDecimal._
import cats.instances.long._
import cats.instances.string._
import external.library.CirceModule._
import external.library.instances.tagged._
import io.circe.{ Decoder, Encoder }
import shapeless.tag
import shapeless.tag.@@

object DomainModelCodecs {
  /**
    * Encoders
    */
  implicit val instantEncoder: Encoder[Instant] =
    encoderFor(_.toString)

  implicit def taggedStringEncoder[T]: Encoder[String @@ T] =
    encoderFor[String @@ T]

  implicit def taggedBigDecimalEncoder[T]: Encoder[BigDecimal @@ T] =
    encoderFor[BigDecimal @@ T]

  implicit def taggedLongEncoder[T]: Encoder[Long @@ T] =
    encoderFor[Long @@ T]

  /**
    * Decoders
    */
  implicit val instantDecoder: Decoder[Instant] =
    decoderFor(Instant.parse)

  implicit def taggedLongDecoder[T]: Decoder[Long @@ T] =
    mappedDecoderFor(_.toLong)(tag[T].apply)

  implicit def taggedBigDecimalDecoder[T]: Decoder[BigDecimal @@ T] =
    mappedDecoderFor(BigDecimal.apply)(tag[T].apply)

  implicit def taggedStringDecoder[T]: Decoder[String @@ T] =
    mappedDecoderFor(identity)(tag[T].apply)
}
