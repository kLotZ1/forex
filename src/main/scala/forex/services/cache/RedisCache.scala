package forex.services.cache

import cats.effect.{Concurrent, ContextShift}
import dev.profunktor.redis4cats.connection.RedisClient
import dev.profunktor.redis4cats.data.RedisCodec
import dev.profunktor.redis4cats.effect.Log
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import forex.config.RedisConfig
import forex.domain.{Currency, Price, Rate, Timestamp}
import fs2.Stream
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, HCursor, Json}

import java.net.URLEncoder
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import scala.concurrent.duration.FiniteDuration

class RedisCache[F[_]](cmd: RedisCommands[F, String, String]) extends Cache[F, String, String] {

  def get(key: String): F[Option[String]] = cmd.get(key)

  def set(key: String, value: String): F[Unit] = cmd.set(key, value)

  def del(key: String): F[Long] = cmd.del(key)

  override def setWithTime(key: String, value: String, ttl: FiniteDuration): F[Unit] = cmd.setEx(key, value, ttl)
}

object RedisCache {
  private val dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

  implicit val rateEncoder: Encoder[Rate] = new Encoder[Rate] {
    final def apply(a: Rate): Json = Json.obj(
      ("from", Json.fromString(a.pair.from.toString)),
      ("to", Json.fromString(a.pair.to.toString)),
      ("price", Json.fromBigDecimal(a.price.value)),
      ("timestamp", Json.fromString(a.timestamp.value.format(dateTimeFormatter)))
    )
  }

  implicit val rateDecoder: Decoder[Rate] = new Decoder[Rate] {
    final def apply(c: HCursor): Decoder.Result[Rate] =
      for {
        from <- c.downField("from").as[String].map(Currency.fromString)
        to <- c.downField("to").as[String].map(Currency.fromString)
        price <- c.downField("price").as[BigDecimal]
        timestamp <- c.downField("timestamp").as[String].map(OffsetDateTime.parse(_, dateTimeFormatter))
      } yield
        Rate(
          Rate.Pair(from, to),
          Price(price),
          Timestamp(timestamp)
        )
  }
  def rateToString(rate: Rate): String = rate.asJson.noSpaces

  def parseRate(rateString: String): Either[io.circe.Error, Rate] =
    decode[Rate](rateString)

  def stream[F[_]: Concurrent: ContextShift: Log](
      config: RedisConfig
  ): Stream[F, RedisCommands[F, String, String]] = {
    val redisURI = buildUri(config)

    Stream.resource {
      for {
        client <- RedisClient[F].from(redisURI)
        cmd <- Redis[F].fromClient(client, RedisCodec.Utf8)
      } yield cmd
    }
  }

  // For small proxy it could be overkill, but we can setup it like this
  private def buildUri(config: RedisConfig): String = {
    val baseUri = new StringBuilder(s"redis://")

    config.password.foreach(pw => baseUri.append(URLEncoder.encode(pw, "UTF-8")).append("@"))

    baseUri.append(config.host).append(":").append(config.port)

    val queryParams = new StringBuilder

    config.database.foreach(db => queryParams.append("&db=").append(db))

    queryParams.append("&max_connections=").append(config.maxConnections)
    queryParams.append("&connection_timeout=").append(config.connectionTimeout.toMillis)
    queryParams.append("&socket_timeout=").append(config.socketTimeout.toMillis)
    queryParams.append("&retry_attempts=").append(config.retryAttempts)
    queryParams.append("&retry_delay=").append(config.retryDelay.toMillis)

    config.keyPrefix.foreach(prefix => queryParams.append("&key_prefix=").append(URLEncoder.encode(prefix, "UTF-8")))
    queryParams.append("&default_ttl=").append(config.defaultTtl.toSeconds)

    if (queryParams.nonEmpty) {
      baseUri.append("/?").append(queryParams.substring(1))
    }

    baseUri.result()
  }
}
