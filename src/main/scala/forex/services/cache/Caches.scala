package forex.services.cache

import dev.profunktor.redis4cats.RedisCommands

object Caches {
  def RateCache[F[_]](cmd: RedisCommands[F, String, String]) = new RedisCache[F](cmd)
}
