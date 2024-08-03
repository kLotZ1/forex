package forex.services.cache

import scala.concurrent.duration.FiniteDuration

trait Cache[F[_], K, V] {
  def get(key: K): F[Option[V]]
  def set(key: K, value: V): F[Unit]
  def setWithTime(key: K, value: V, ttl: FiniteDuration): F[Unit]
  def del(key: K): F[Long]
}
