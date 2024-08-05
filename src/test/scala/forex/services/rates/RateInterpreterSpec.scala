package forex.services.rates

import cats.effect.IO
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.http.client.{OneFrameClient, Protocol}
import forex.services.cache.{Cache, RedisCache}
import forex.services.rates.interpreters.RateInterpreter
import org.mockito.ArgumentMatchersSugar
import org.mockito.scalatest.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class RateInterpreterSpec extends AnyFlatSpec with Matchers with MockitoSugar with ArgumentMatchersSugar {

  "RateInterpreter" should "retrieve rates from cache if available" in {
    val mockClient  = mock[OneFrameClient[IO]]
    val mockCache   = mock[Cache[IO, String, String]]
    val interpreter = new RateInterpreter[IO](mockClient, mockCache)
    val pair        = Rate.Pair(Currency.USD, Currency.JPY)
    val rate        = Rate(pair, Price(BigDecimal(107.50)), Timestamp.now)

    when(mockCache.get(s"rate:${pair.from}:${pair.to}")).thenReturn(IO(Some(RedisCache.rateToString(rate))))

    val result = interpreter.get(pair, "token").unsafeRunSync()

    result shouldBe Right(rate)
    verify(mockCache).get(s"rate:${pair.from}:${pair.to}")
    verifyNoMoreInteractions(mockClient)
  }

  it should "fetch rates from the client and update cache if not in cache" in {
    val mockClient  = mock[OneFrameClient[IO]]
    val mockCache   = mock[Cache[IO, String, String]]
    val interpreter = new RateInterpreter[IO](mockClient, mockCache)
    val pair        = Rate.Pair(Currency.USD, Currency.JPY)
    val rate        = Rate(pair, Price(BigDecimal(107.50)), Timestamp.now)
    val response    = Protocol.RateResponse(pair.from, pair.to, Price(100.0), Price(100.0), rate.price, rate.timestamp)

    when(mockCache.get(s"rate:${pair.from}:${pair.to}")).thenReturn(IO(None))
    when(mockClient.getRates(pair, "token")).thenReturn(IO(Right(response)))
    when(mockCache.setWithTime(any[String], any[String], any[FiniteDuration])).thenReturn(IO.unit)

    val result = interpreter.get(pair, "token").unsafeRunSync()

    result shouldBe Right(rate)
    verify(mockCache).get(s"rate:${pair.from}:${pair.to}")
    verify(mockClient).getRates(pair, "token")
    verify(mockCache).setWithTime(s"rate:${pair.from}:${pair.to}", RedisCache.rateToString(rate), 5.minutes)
  }
}
