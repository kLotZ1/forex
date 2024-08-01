package forex.services.rates.interpreters

import cats.Applicative
import cats.data.EitherT
import forex.config.ClientConfig
import forex.domain.Rate
import forex.http.client.OneFrameClient
import forex.http.client.Protocol.RateResponse
import forex.services.rates.ServiceAlgebra
import forex.services.rates.ServiceErrors.{ServiceError, toServiceError}
import org.http4s._

class RateInterpreter[F[_]: Applicative](client: OneFrameClient[F], config: ClientConfig) extends ServiceAlgebra[F] {
  private val prefixPath = "/rates"
  override def get(pair: Rate.Pair): F[ServiceError Either Rate] = {
    val baseUri = createBaseUri(pair)
    EitherT(client.getRates(baseUri))
      .bimap(toServiceError, RateResponse.toRate)
      .value
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
      path = Uri.Path.unsafeFromString(prefixPath),
      query = Query.fromPairs("pair" -> pair.pairString())
    )
}
