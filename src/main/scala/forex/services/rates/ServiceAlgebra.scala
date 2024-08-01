package forex.services.rates

import forex.domain.Rate
import ServiceErrors._

trait ServiceAlgebra[F[_]] {
  def get(pair: Rate.Pair): F[ServiceError Either Rate]
}
