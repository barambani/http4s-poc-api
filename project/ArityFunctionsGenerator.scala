package BoilerplateGeneration

import sbt.File

object ArityFunctionsGenerator {

  def run: Int => File => Seq[File] =
    BoilerplateGenerator(templates).run

  private val templates: Seq[Template] = Seq(
    ParallelEffectArityFunctions,
    ParallelEffectAritySyntax
  )
}
