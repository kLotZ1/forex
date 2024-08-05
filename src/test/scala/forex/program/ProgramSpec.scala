package forex.program

import cats.Id
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.programs.rates.{Program, ProgramErrors, Protocol}
import forex.services.RatesService
import forex.services.rates.ServiceErrors
import org.mockito.scalatest.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ProgramSpec extends AnyFlatSpec with Matchers with MockitoSugar {

  "Program" should "return a rate successfully" in {
    val mockRatesService = mock[RatesService[Id]]
    val program = new Program[Id](mockRatesService)
    val pair = Rate.Pair(Currency.USD, Currency.JPY)
    val rate = Rate(pair, Price(BigDecimal(100)), Timestamp.now)
    val request = Protocol.GetRatesRequest(Currency.USD, Currency.JPY, "some-token")

    when(mockRatesService.get(pair, "some-token")).thenReturn(Id(Right(rate)))

    val result = program.get(request)

    result should be (Right(rate))
  }

  it should "handle service errors" in {
    val mockRatesService = mock[RatesService[Id]]
    val program = new Program[Id](mockRatesService)
    val pair = Rate.Pair(Currency.USD, Currency.JPY)
    val serviceError = ServiceErrors.ServiceError.NoCurrencyPairProvided("No currency pair")
    val expected = ProgramErrors.ProgramError.RateLookupFailed(serviceError.msg);
    val request = Protocol.GetRatesRequest(Currency.USD, Currency.JPY, "some-token")

    when(mockRatesService.get(pair, "some-token")).thenReturn(Id(Left(serviceError)))

    val result = program.get(request)

    result should be (Left(expected))
  }
}
