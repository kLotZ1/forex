package forex

package object services {
  type RatesService[F[_]] = rates.ServiceAlgebra[F]
  final val RatesServices = rates.Interpreters
}
