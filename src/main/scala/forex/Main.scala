package forex

import cats.effect._
import dev.profunktor.redis4cats.effect.Log
import forex.config._
import forex.http.client.OneFrameClient
import forex.services.cache.RedisCache
import fs2.Stream
import org.http4s.blaze.server.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  // Could be added a logger, that will sink to Splunk, ELK, etc. Here is just a template
  implicit val log: Log[IO] = Log.NoOp.instance
  override def run(args: List[String]): IO[ExitCode] =
    new Application[IO].stream(executionContext).compile.drain.as(ExitCode.Success)

}

class Application[F[_]: ConcurrentEffect: Timer: ContextShift: Log] {

  def stream(ec: ExecutionContext): Stream[F, Unit] =
    for {
      config <- Config.stream("app")
      client <- OneFrameClient.stream(config.client)
      redis <- RedisCache.stream(config.redisConfig)
      module = new Module[F](config, client, redis)
      _ <- BlazeServerBuilder[F](ec)
            .bindHttp(config.http.port, config.http.host)
            .withHttpApp(module.httpApp)
            .serve
    } yield ()

}
