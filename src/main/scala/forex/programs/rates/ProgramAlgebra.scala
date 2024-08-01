package forex.programs.rates

import forex.domain.Rate
import forex.programs.rates.ProgramErrors.ProgramError
trait ProgramAlgebra[F[_]] {
  def get(request: Protocol.GetRatesRequest): F[ProgramError Either Rate]
}
