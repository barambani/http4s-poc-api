package external
package library

package object syntax {
  object parallelEffect extends ParallelEffectSyntax with ParallelEffectAritySyntax
  object errorResponse  extends ErrorResponseSyntax
  object errorAdapt     extends ErrorAdaptSyntax
  object ioAdapt        extends IoAdaptSyntax
}
