package forex.programs.rates

import cats.Functor
import cats.data.EitherT
import forex.domain._
import forex.programs.rates.ProgramErrors._
import forex.services.RatesService

class Program[F[_]: Functor](
    ratesService: RatesService[F]
) extends ProgramAlgebra[F] {

  override def get(request: Protocol.GetRatesRequest): F[ProgramError Either Rate] =
    EitherT(ratesService.get(Rate.Pair(request.from, request.to))).leftMap(toProgramError).value

}

object Program {

  def apply[F[_]: Functor](
      ratesService: RatesService[F]
  ): ProgramAlgebra[F] = new Program[F](ratesService)

}
