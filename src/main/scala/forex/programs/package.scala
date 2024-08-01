package forex

package object programs {
  type RatesProgram[F[_]] = rates.ProgramAlgebra[F]
  final val RatesProgram = rates.Program
}
