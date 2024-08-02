package forex.http
package rates

import cats.effect.Sync
import cats.syntax.all._
import forex.domain.Currency
import forex.programs.RatesProgram
import forex.programs.rates.ProgramErrors.ProgramError
import forex.programs.rates.{ Protocol => RatesProgramProtocol }
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.{ HttpRoutes, Request, Response }
import org.typelevel.ci.CIString

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._
  import Protocol._
  import QueryParams._

  private[http] val prefixPath = "/rates"
  private[http] val authHeader = CIString("token")

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ GET -> Root :? FromQueryParam(from) +& ToQueryParam(to) =>
      (for {
        headerValue <- extractHeader(req, authHeader)
        response <- handleRatesRequest(from, to, headerValue)
      } yield response).handleErrorWith {
        case error: ProgramError => handleProgramError(error)
        case other               => InternalServerError(s"Unexpected error: ${other.getMessage}")
      }
  }

  private def extractHeader(req: Request[F], headerName: CIString): F[String] =
    req.headers
      .get(headerName)
      .map(_.head.value)
      .liftTo[F](ProgramError.HeaderMissing(s"Missing required header: $headerName"))

  private def handleRatesRequest(from: Currency, to: Currency, headerValue: String): F[Response[F]] =
    rates.get(RatesProgramProtocol.GetRatesRequest(from, to, headerValue)).flatMap {
      case Right(rate) => Ok(rate.asGetApiResponse)
      case Left(error) => handleProgramError(error)
    }

  private def handleProgramError(error: ProgramError): F[Response[F]] = error match {
    case ProgramError.RateLookupFailed(msg)    => InternalServerError(s"Rate lookup failed: $msg")
    case ProgramError.InvalidCurrencyPair(msg) => BadRequest(s"Invalid currency pair: $msg")
    case ProgramError.UnexpectedError(msg)     => InternalServerError(s"Unexpected error: $msg")
    case ProgramError.NotFound(msg)            => NotFound(s"Resource not found: $msg")
    case ProgramError.HeaderMissing(msg)       => BadRequest(msg)
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
