package forex.services.cache

import cats.effect.IO
import dev.profunktor.redis4cats.RedisCommands
import org.mockito.ArgumentMatchersSugar
import org.mockito.scalatest.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class RedisCacheSpec extends AnyFlatSpec with Matchers with MockitoSugar with ArgumentMatchersSugar {

  "RedisCache" should "retrieve a value correctly" in {
    val mockCmd = mock[RedisCommands[IO, String, String]]
    val redisCache = new RedisCache[IO](mockCmd)

    when(mockCmd.get("testKey")).thenReturn(IO(Some("testValue")))

    val result = redisCache.get("testKey").unsafeRunSync()

    result shouldBe Some("testValue")
  }

  it should "set a value correctly" in {
    val mockCmd = mock[RedisCommands[IO, String, String]]
    val redisCache = new RedisCache[IO](mockCmd)

    when(mockCmd.set("testKey", "testValue")).thenReturn(IO.unit)

    redisCache.set("testKey", "testValue").unsafeRunSync()

    verify(mockCmd).set("testKey", "testValue")
  }

  it should "delete a key correctly" in {
    val mockCmd = mock[RedisCommands[IO, String, String]]
    val redisCache = new RedisCache[IO](mockCmd)

    when(mockCmd.del("testKey")).thenReturn(IO(1L))

    val result = redisCache.del("testKey").unsafeRunSync()

    result shouldBe 1L
    verify(mockCmd).del("testKey")
  }

  it should "set a value with expiration correctly" in {
    val mockCmd = mock[RedisCommands[IO, String, String]]
    val redisCache = new RedisCache[IO](mockCmd)
    val ttl = 10.seconds

    when(mockCmd.setEx("testKey", "testValue", ttl)).thenReturn(IO.unit)

    redisCache.setWithTime("testKey", "testValue", ttl).unsafeRunSync()

    verify(mockCmd).setEx("testKey", "testValue", ttl)
  }
}
