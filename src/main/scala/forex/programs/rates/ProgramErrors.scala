package forex.programs.rates

import forex.services.rates.ServiceErrors.{ServiceError => RatesServiceError}

object ProgramErrors {
  sealed trait ProgramError extends Exception

  object ProgramError {
    final case class RateLookupFailed(msg: String) extends ProgramError
    final case class InvalidCurrencyPair(msg: String) extends ProgramError
    final case class UnexpectedError(msg: String) extends ProgramError
    final case class NotFound(msg: String) extends ProgramError
    final case class HeaderMissing(msg: String) extends ProgramError
    final case class Forbidden(msg: String) extends ProgramError
  }

  def toProgramError(error: RatesServiceError): ProgramError = error match {
    case RatesServiceError.UnexpectedError(msg)        => ProgramError.RateLookupFailed(msg)
    case RatesServiceError.NoCurrencyPairProvided(msg) => ProgramError.RateLookupFailed(msg)
    case RatesServiceError.InvalidCurrencyPair(msg)    => ProgramError.InvalidCurrencyPair(msg)
    case RatesServiceError.ClientTokenExpired(msg)     => ProgramError.Forbidden(msg)
    case RatesServiceError.NotFound(msg)               => ProgramError.NotFound(msg)
    case RatesServiceError.CacheDecodingError(msg)     => ProgramError.UnexpectedError(msg)
  }
}
