package lib.util

import io.circe.Encoder
import lib.ErrorInvariantMap
import lib.instances.SyncInstances._
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

object EntityEncoderModule {

  def eitherEntityEncoder[E : ErrorInvariantMap[Throwable, ?], A : Encoder]: EntityEncoder[Either[E, ?], A] =
    jsonEncoderOf[Either[E, ?], A]
}