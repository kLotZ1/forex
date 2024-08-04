package forex.http.client

import cats.effect.{Async, ConcurrentEffect}
import cats.implicits.toFlatMapOps
import cats.syntax.all._
import forex.config.ClientConfig
import forex.domain.Rate
import forex.http.client.ClientErrors.ClientError
import forex.http.client.Protocol.{ErrorResponse, RateResponse}
import fs2.Stream
import io.circe.parser.decode
import org.http4s._
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client
import org.typelevel.ci.CIString

import scala.concurrent.ExecutionContext

class OneFrameClient[F[_]: Async](client: Client[F], config: ClientConfig) {
  // Could be moved to config, or taken as a const in Obj.
  private val ratePrefixPath = "/rates"
  def getRates(pair: Rate.Pair, token: String): F[ClientError Either RateResponse] =
    makeRequest(pair, token).flatMap(handleResponse)

  private def makeRequest(pair: Rate.Pair, token: String): F[Response[F]] = {
    val uri = createBaseUri(pair)
    client
      .run(
        Request[F](uri = uri)
          .putHeaders(Header.Raw(CIString("token"), token))
      )
      .use(Async[F].pure)
  }

  private def createBaseUri(pair: Rate.Pair): Uri =
    Uri(
      scheme = Some(Uri.Scheme.http),
      authority = Some(
        Uri.Authority(
          host = Uri.RegName(config.host),
          port = Some(config.port)
        )
      ),
      path = Uri.Path.unsafeFromString(ratePrefixPath),
      query = Query.fromPairs("pair" -> pair.pairString())
    )

  private def handleResponse(response: Response[F]): F[Either[ClientError, RateResponse]] =
    response.status match {
      case Status.Ok                  => handleOkResponse(response)
      case Status.NotFound            => errorResponse(ClientError.NotFound("Resource not found (404)"))
      case Status.InternalServerError => errorResponse(ClientError.InternalServerError("Internal server error (500)"))
      case status if status.isSuccess => handleUnexpectedSuccessResponse(response)
      case status                     => errorResponse(ClientError.Unexpected(s"Unexpected status: ${status.code}"))
    }

  private def handleOkResponse(response: Response[F]): F[Either[ClientError, RateResponse]] =
    response.bodyText.compile.string.flatMap { body =>
      if (body.nonEmpty) {
        decode[List[RateResponse]](body) match {
          case Right(ratesResponse) => Async[F].pure(Right(ratesResponse.head))
          case Left(_)              => extractError(body)
        }
      } else {
        errorResponse(ClientError.ResponseBodyIsEmpty("Empty response body"))
      }
    }

  private def extractError(body: String) =
    decode[ErrorResponse](body) match {
      case Right(error) => errorResponse(mapErrorToClientError(error.error))
      case Left(s)      => errorResponse(ClientError.Unexpected(s.getMessage))
    }

  private def errorResponse(error: ClientError): F[Either[ClientError, RateResponse]] =
    error.asLeft[RateResponse].pure[F]

  private def handleUnexpectedSuccessResponse(response: Response[F]): F[Either[ClientError, RateResponse]] =
    response.bodyText.compile.string.map { body =>
      ClientError
        .Unexpected(s"Unexpected success status: ${response.status.code}, body: $body")
        .asLeft[RateResponse]
    }

  private def mapErrorToClientError(error: String): ClientError = error match {
    case ErrorMessages.Forbidden              => ClientError.Forbidden(error)
    case ErrorMessages.InvalidCurrencyPair    => ClientError.InvalidCurrencyPair(error)
    case ErrorMessages.NoCurrencyPairProvided => ClientError.NoCurrencyPairProvided(error)
    case _                                    => ClientError.Unexpected(error)
  }
}

object OneFrameClient {
  def stream[F[_]: ConcurrentEffect](config: ClientConfig): Stream[F, OneFrameClient[F]] =
    Stream.resource(
      BlazeClientBuilder[F](ExecutionContext.global).resource.map(client => new OneFrameClient[F](client, config))
    )
}
