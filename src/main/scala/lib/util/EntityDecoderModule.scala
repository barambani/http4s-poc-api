package lib.util

import io.circe.Decoder
import lib.ErrorInvariantMap
import lib.instances.SyncInstances._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

object EntityDecoderModule {

  def eitherEntityDecoder[E : ErrorInvariantMap[Throwable, ?], A : Decoder]: EntityDecoder[Either[E, ?], A] =
    jsonOf[Either[E, ?], A]
}