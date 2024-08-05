package forex.http.client

import cats.effect.{IO, Resource}
import forex.config.ClientConfig
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.http.client.ClientErrors.ClientError
import forex.http.client.Protocol.RateResponse
import org.http4s._
import org.http4s.client.Client
import org.mockito.scalatest.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.OffsetDateTime

class OneFrameClientSpec extends AnyFlatSpec with Matchers with MockitoSugar {

  "OneFrameClient" should "successfully decode rate response" in {
    val mockClient = mock[Client[IO]]
    val config     = ClientConfig(host = "api.forex.com", port = 80)
    val client     = new OneFrameClient[IO](mockClient, config)
    val pair       = Rate.Pair(Currency.USD, Currency.JPY)

    val jsonResponse = """
    [
        {
            "from": "USD",
            "to": "JPY",
            "bid": 0.9,
            "ask": 0.3,
            "price": 0.6,
            "time_stamp": "2024-08-05T06:40:37.613Z"
        }
    ]
    """
    val response     = Response[IO](status = Status.Ok).withEntity(jsonResponse)
    val resource     = Resource.eval(IO.pure(response))

    when(mockClient.run(any[Request[IO]])).thenReturn(resource)

    val result = client.getRates(pair, "token").unsafeRunSync()

    result should be(
      Right(
        RateResponse(
          pair.from,
          pair.to,
          Price(0.9),
          Price(0.3),
          Price(0.6),
          new Timestamp(OffsetDateTime.parse("2024-08-05T06:40:37.613Z"))
        )
      )
    )
  }

  it should "handle error when API returns unexpected status" in {
    val mockClient = mock[Client[IO]]
    val config     = ClientConfig(host = "api.forex.com", port = 80)
    val client     = new OneFrameClient[IO](mockClient, config)
    val pair       = Rate.Pair(Currency.USD, Currency.JPY)

    val response = Response[IO](status = Status.InternalServerError)
    val resource = Resource.eval(IO.pure(response))

    when(mockClient.run(any[Request[IO]])).thenReturn(resource)

    val result = client.getRates(pair, "token").unsafeRunSync()

    result.left.getOrElse(fail("Expected error not found")) shouldBe a[ClientError.InternalServerError]
  }
}
