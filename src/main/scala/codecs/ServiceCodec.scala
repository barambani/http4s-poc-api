package codecs

import java.time.Instant

import http4s.extend.util.CirceModule._
import io.circe.{Decoder, Encoder}
import shapeless.tag
import shapeless.tag.@@

object ServiceCodec {

  /**
    * Encoders
    */
  implicit val instantEncoder: Encoder[Instant] =
    stringEncoder(_.toString)

  implicit def taggedStringEncoder[T]: Encoder[String @@ T] =
    stringEncoder(identity)

  implicit def taggedBigDecimalEncoder[T]: Encoder[BigDecimal @@ T] =
    stringEncoder(_.toString)

  /**
    * Decoders
    */
  implicit val instantDecoder: Decoder[Instant] =
    stringDecoder(Instant.parse)

  implicit def taggedLongDecoder[T]: Decoder[Long @@ T] =
    stringDecoderMap(_.toLong)(tag[T].apply)
}