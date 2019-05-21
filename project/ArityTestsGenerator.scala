package BoilerplateGeneration

import sbt.File

object ArityTestsGenerator {

  def run: Int => File => Seq[File] =
    BoilerplateGenerator(templates).run

  private val templates: Seq[Template] = Seq(
    ParallelEffectSyntaxAccumulateErrorTests
  )
}
