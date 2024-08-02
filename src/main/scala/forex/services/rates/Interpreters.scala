package forex.services.rates

import cats.effect.Sync
import forex.http.client.OneFrameClient
import forex.services.rates.interpreters.RateInterpreter

object Interpreters {
  def RateInterpreter[F[_]: Sync](client: OneFrameClient[F]): ServiceAlgebra[F] = new RateInterpreter[F](client)
}
