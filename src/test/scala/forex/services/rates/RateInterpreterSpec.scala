package forex.services.rates

import cats.effect.IO
import forex.config.ClientConfig
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.http.client.OneFrameClient
import forex.services.cache.{Cache, RedisCache}
import forex.services.rates.interpreters.RateInterpreter
import org.http4s.client.Client
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.OffsetDateTime

class RateInterpreterSpec extends AnyFlatSpec with Matchers with MockFactory {

  "RateInterpreter" should "return cached rate when available" in {
    val clientMock: Client[IO] = mock[Client[IO]]
    val config                     = ClientConfig("localhost", 8080)
    val oneFrameClient             = new OneFrameClient[IO](clientMock, config)
    val mockCache                  = mock[Cache[IO, String, String]]
    val interpreter                = new RateInterpreter[IO](oneFrameClient, mockCache)

    val pair             = Rate.Pair(Currency.USD, Currency.EUR)
    val token            = "test-token"
    val cacheKey         = "rate:USD:EUR"
    val cachedRate       = Rate(pair, Price(1.5), Timestamp(OffsetDateTime.now()))
    val cachedRateString = RedisCache.rateToString(cachedRate)

    (mockCache.get _).expects(cacheKey).returning(IO.pure(Some(cachedRateString)))

    val result = interpreter.get(pair, token).unsafeRunSync()

    result shouldBe Right(cachedRate)
  }

}
