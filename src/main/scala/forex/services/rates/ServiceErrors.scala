package forex.services.rates

import forex.http.client.ClientErrors.ClientError
import forex.http.client.ClientErrors.ClientError._

object ServiceErrors {
  sealed trait ServiceError extends Exception

  object ServiceError {
    final case class InvalidCurrencyPair(msg: String) extends ServiceError
    final case class UnexpectedError(msg: String) extends ServiceError
    final case class ClientTokenExpired(msg: String) extends ServiceError
    final case class NotFound(msg: String) extends ServiceError
    final case class CacheDecodingError(msg: String) extends ServiceError
    final case class NoCurrencyPairProvided(msg: String) extends ServiceError
  }

  def toServiceError(error: ClientError): ServiceError = error match {
    case InvalidCurrencyPair(msg)    => ServiceError.InvalidCurrencyPair(msg)
    case NoCurrencyPairProvided(msg) => ServiceError.NoCurrencyPairProvided(msg)
    case Forbidden(msg)              => ServiceError.ClientTokenExpired(msg)
    case ResponseBodyIsEmpty(msg)    => ServiceError.UnexpectedError(msg)
    case InternalServerError(msg)    => ServiceError.UnexpectedError(msg)
    case NotFound(msg)               => ServiceError.NotFound(msg)
    case Unexpected(msg)             => ServiceError.UnexpectedError(msg)
  }

}
