package forex.http.client

import cats.effect.{Async, ConcurrentEffect}
import cats.implicits.toFlatMapOps
import forex.config.ClientConfig
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

  def getRates(uri: Uri): F[Either[ClientError, RateResponse]] = {
    val request = Request[F](uri = uri)
      .putHeaders(Header.Raw(CIString("token"), config.token))

    client.run(request).use { response =>
      response.status match {
        case Status.Ok =>
          response.bodyText.compile.string.flatMap { body =>
            if (body.nonEmpty) {
              decode[List[RateResponse]](body) match {
                case Right(ratesResponse) => Async[F].pure(Right(ratesResponse.head))
                case Left(error) => Async[F].pure(Left(ClientError.UnexpectedError(s"Failed to parse response: ${error.getMessage}")))
              }
            } else {
              Async[F].pure(Left(ClientError.ResponseBodyIsEmpty("Empty response body")))
            }
          }
        case Status.NotFound =>
          Async[F].pure(Left(ClientError.UnexpectedError("Resource not found (404)")))
        case Status.InternalServerError =>
          Async[F].pure(Left(ClientError.UnexpectedError("Internal server error (500)")))
        case status if status.isSuccess =>
          response.bodyText.compile.string.flatMap { body =>
            Async[F].pure(Left(ClientError.UnexpectedError(s"Unexpected success status: ${status.code}, body: $body")))
          }
        case status =>
          Async[F].pure(Left(ClientError.UnexpectedError(s"Unexpected status: ${status.code}")))
      }
    }
  }
}

object OneFrameClient {
  def stream[F[_]: ConcurrentEffect](config: ClientConfig): Stream[F, OneFrameClient[F]] =
    Stream.resource(
      BlazeClientBuilder[F](ExecutionContext.global).resource.map(client => new OneFrameClient[F](client, config))
    )
}
