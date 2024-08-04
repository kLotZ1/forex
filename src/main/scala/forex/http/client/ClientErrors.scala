package forex.http.client

object ClientErrors {

  sealed trait ClientError extends Exception
  object ClientError {
    final case class InvalidCurrencyPair(msg: String) extends ClientError
    final case class NoCurrencyPairProvided(msg: String) extends ClientError
    final case class Forbidden(msg: String) extends ClientError
    final case class ResponseBodyIsEmpty(msg: String) extends ClientError
    final case class Unexpected(msg: String) extends ClientError
    final case class NotFound(msg: String) extends ClientError
    final case class InternalServerError(msg: String) extends ClientError
  }
}
