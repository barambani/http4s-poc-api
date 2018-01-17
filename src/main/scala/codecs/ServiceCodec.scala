package codecs

import java.time.Instant

import cats.syntax.either._
import io.circe.{Decoder, Encoder}
import shapeless.tag
import shapeless.tag.@@


object ServiceCodec {

  implicit val instantEncoder: Encoder[Instant] =
    Encoder.encodeString.contramap[Instant](_.toString)

  implicit val instantDecoder: Decoder[Instant] =
    Decoder.decodeString emap {
      str => Either.catchNonFatal(Instant.parse(str)) leftMap (_ => s"Cannot parse $str to DateTime")
    }

  implicit def taggedStringEncoder[T]: Encoder[String @@ T] =
    Encoder.encodeString.contramap[String @@ T](identity)

  implicit def taggedBigDecimalEncoder[T]: Encoder[BigDecimal @@ T] =
    Encoder.encodeString.contramap[BigDecimal @@ T](_.toString)

  implicit def taggedLongDecoder[T]: Decoder[Long @@ T] =
    Decoder.decodeString emap {
      str => Either.catchNonFatal(str.toLong) leftMap (_ => s"Cannot parse $str to Long") map tag[T].apply
    }
}