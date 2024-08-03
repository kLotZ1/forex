package forex.services.rates

import forex.domain.Rate
import forex.services.rates.ServiceErrors._

trait ServiceAlgebra[F[_]] {
  def get(pair: Rate.Pair, token: String): F[ServiceError Either Rate]
}
