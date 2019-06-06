package external
package library

package object syntax {
  object parallelEffect extends ParallelEffectSyntax with ParallelEffectAritySyntax
  object errorAdapt     extends ErrorAdaptSyntax
  object ioAdapt        extends IoAdaptSyntax
}
