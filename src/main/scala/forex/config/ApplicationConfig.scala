package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig,
    client: ClientConfig,
    redisConfig: RedisConfig
)

case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)

case class ClientConfig(
    host: String,
    port: Int
)

case class RedisConfig(
    host: String,
    port: Int,
    password: Option[String],
    database: Option[Int],
    maxConnections: Int,
    connectionTimeout: FiniteDuration,
    socketTimeout: FiniteDuration,
    retryAttempts: Int,
    retryDelay: FiniteDuration,
    keyPrefix: Option[String],
    defaultTtl: FiniteDuration
)
