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
  }

  def toServiceError(error: ClientError): ServiceError = error match {
    case InvalidCurrencyPair(msg) => ServiceError.InvalidCurrencyPair(msg)
    case TokenExpired(msg)        => ServiceError.ClientTokenExpired(msg)
    case ResponseBodyIsEmpty(msg) => ServiceError.UnexpectedError(msg)
    case InternalServerError(msg) => ServiceError.UnexpectedError(msg)
    case NotFoundError(msg)       => ServiceError.NotFound(msg)
    case UnexpectedError(msg)     => ServiceError.UnexpectedError(msg)
  }

}
