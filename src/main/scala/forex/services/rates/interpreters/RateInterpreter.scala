package forex.services.rates.interpreters

import cats.Monad
import cats.effect.Sync
import cats.implicits.{toBifunctorOps, toFlatMapOps, toFunctorOps, toTraverseOps}
import forex.domain.Rate
import forex.http.client.{OneFrameClient, Protocol}
import forex.services.cache.{Cache, RedisCache}
import forex.services.rates.ServiceAlgebra
import forex.services.rates.ServiceErrors.{ServiceError, toServiceError}

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class RateInterpreter[F[_]: Sync](
    client: OneFrameClient[F],
    cache: Cache[F, String, String]
) extends ServiceAlgebra[F] {
  private val cacheTTL: FiniteDuration = 5.minutes
  override def get(pair: Rate.Pair, token: String): F[Either[ServiceError, Rate]] = {
    val cacheKey = s"rate:${pair.from}:${pair.to}"

    cache.get(cacheKey).flatMap {
      case Some(rateString) =>
        Monad[F].pure(RedisCache.parseRate(rateString).leftMap(e => ServiceError.CacheDecodingError(e.getMessage)))
      case None =>
        client
          .getRates(pair, token)
          .flatMap { rateResponse =>
            rateResponse.traverse { rate =>
              val rateString = RedisCache.rateToString(Protocol.RateResponse.asRate(rate))
              cache.setWithTime(cacheKey, rateString, cacheTTL).as(Protocol.RateResponse.asRate(rate))
            }
          }
          .map(_.leftMap(toServiceError))
    }
  }
}
