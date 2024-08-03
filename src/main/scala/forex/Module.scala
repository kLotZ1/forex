package forex

import cats.effect.{Concurrent, Timer}
import dev.profunktor.redis4cats.RedisCommands
import forex.config.ApplicationConfig
import forex.http.client.OneFrameClient
import forex.http.rates.RatesHttpRoutes
import forex.programs._
import forex.services._
import forex.services.cache.Caches
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.middleware.{AutoSlash, Timeout}

class Module[F[_]: Concurrent: Timer](config: ApplicationConfig,
                                      client: OneFrameClient[F],
                                      redisCmds: RedisCommands[F, String, String]) {

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]
  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)
  private val rateCache: RatesRedisCache[F, String, String] = Caches.RateCache(redisCmds)
  private val ratesService: RatesService[F] = RatesServices.RateInterpreter[F](client, rateCache)
  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService)
  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes
  private val routesMiddleware: PartialMiddleware = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    }
  }
  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    Timeout(config.http.timeout)(http)
  }
  private val http: HttpRoutes[F] = ratesHttpRoutes

}
