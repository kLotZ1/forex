package forex.http.client

import cats.effect.{Async, ConcurrentEffect}
import cats.implicits.toFlatMapOps
import cats.syntax.all._
import forex.config.ClientConfig
import forex.domain.Rate
import forex.http.client.ClientErrors.ClientError
import forex.http.client.Protocol.RateResponse
import fs2.Stream
import io.circe.parser.decode
import org.http4s._
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client
import org.typelevel.ci.CIString

import scala.concurrent.ExecutionContext

class OneFrameClient[F[_]: Async](client: Client[F], config: ClientConfig) {
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

  private def errorResponse(error: ClientError): F[Either[ClientError, RateResponse]] =
    error.asLeft[RateResponse].pure[F]
  private def handleResponse(response: Response[F]): F[Either[ClientError, RateResponse]] =
    response.status match {
      case Status.Ok                  => handleOkResponse(response)
      case Status.NotFound            => errorResponse(ClientError.NotFoundError("Resource not found (404)"))
      case Status.InternalServerError => errorResponse(ClientError.InternalServerError("Internal server error (500)"))
      case status if status.isSuccess => handleUnexpectedSuccessResponse(response)
      case status                     => errorResponse(ClientError.UnexpectedError(s"Unexpected status: ${status.code}"))
    }

  private def handleOkResponse(response: Response[F]): F[Either[ClientError, RateResponse]] =
    response.bodyText.compile.string.flatMap { body =>
      if (body.nonEmpty) {
        decode[List[RateResponse]](body) match {
          case Right(ratesResponse) => Async[F].pure(Right(ratesResponse.head))
          case Left(error) =>
            errorResponse(ClientError.UnexpectedError(s"Failed to parse response: ${error.getMessage}"))
        }
      } else {
        errorResponse(ClientError.ResponseBodyIsEmpty("Empty response body"))
      }
    }

  private def handleUnexpectedSuccessResponse(response: Response[F]): F[Either[ClientError, RateResponse]] =
    response.bodyText.compile.string.map { body =>
      ClientError
        .UnexpectedError(s"Unexpected success status: ${response.status.code}, body: $body")
        .asLeft[RateResponse]
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
}

object OneFrameClient {
  def stream[F[_]: ConcurrentEffect](config: ClientConfig): Stream[F, OneFrameClient[F]] =
    Stream.resource(
      BlazeClientBuilder[F](ExecutionContext.global).resource.map(client => new OneFrameClient[F](client, config))
    )
}
