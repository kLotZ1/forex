package forex.services.rates.interpreters

import cats.Applicative
import cats.data.EitherT
import forex.domain.Rate
import forex.http.client.OneFrameClient
import forex.http.client.Protocol.RateResponse
import forex.services.rates.ServiceAlgebra
import forex.services.rates.ServiceErrors.{ServiceError, toServiceError}

class RateInterpreter[F[_]: Applicative](client: OneFrameClient[F]) extends ServiceAlgebra[F] {
  override def get(pair: Rate.Pair, token: String): F[ServiceError Either Rate] =
    EitherT(client.getRates(pair, token))
      .bimap(toServiceError, RateResponse.toRate)
      .value

}
