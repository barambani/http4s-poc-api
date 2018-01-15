package codecs

import java.time.Instant

import cats.syntax.either._
import io.circe.{Decoder, Encoder}

object ServiceCodec {

  implicit val instantEncoder: Encoder[Instant] =
    Encoder.encodeString.contramap[Instant](_.toString)

  implicit val instantDecoder: Decoder[Instant] =
    Decoder.decodeString emap {
      str => Either.catchNonFatal(Instant.parse(str)) leftMap (_ => s"Cannot parse $str to DateTime")
    }
}