package forex

package object services {
  type RatesService[F[_]]          = rates.ServiceAlgebra[F]
  type RatesRedisCache[F[_], K, V] = cache.Cache[F, K, V]
  final val RatesServices   = rates.Interpreters
  final val RatesRedisCache = cache.Caches
}
