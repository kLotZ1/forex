package forex.services.rates

import cats.effect.Sync
import forex.config.ClientConfig
import forex.http.client.OneFrameClient
import forex.services.rates.interpreters.RateInterpreter

object Interpreters {
  def RateInterpreter[F[_]: Sync](client: OneFrameClient[F], config: ClientConfig): ServiceAlgebra[F] = new RateInterpreter[F](client, config)
}
