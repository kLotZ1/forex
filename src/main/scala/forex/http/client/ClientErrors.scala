package forex.http.client

object ClientErrors {

  sealed trait ClientError extends Exception
  object ClientError {
    final case class InvalidCurrencyPair(msg: String) extends ClientError
    final case class TokenExpired(msg: String) extends ClientError
    final case class ResponseBodyIsEmpty(msg: String) extends ClientError
    final case class UnexpectedError(msg: String) extends ClientError
  }
}
