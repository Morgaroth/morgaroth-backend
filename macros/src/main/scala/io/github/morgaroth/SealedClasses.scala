package io.github.morgaroth

import scala.language.experimental.macros
import scala.reflect.macros.whitebox

object SealedClasses {
  def values[A]: Map[String, Manifest[_ <: A]] = macro values_impl[A]

  def values_impl[A: c.WeakTypeTag](c: whitebox.Context) = {

    import c.universe._

    val symbol = weakTypeOf[A].typeSymbol

    if (!symbol.isClass) c.abort(
      c.enclosingPosition,
      s"${symbol.name} is a Class! Can only enumerate values of a sealed trait or class."
    ) else if (!symbol.asClass.isSealed) c.abort(
      c.enclosingPosition,
      s"${symbol.name} isn't sealed! Can only enumerate values of a sealed trait or class.${symbol.asClass}"
    ) else {
      c.Expr[Map[String, Manifest[_ <: A]]] {
        def toEntity(s: Symbol): List[c.Tree] = {
          if (s.asClass.isSealed) {
            s.asClass.knownDirectSubclasses.toList.flatMap(toEntity(_))
          } else {
            val typeName = if (s.isModuleClass) s.asType.asType.fullName + ".type" else s.asType.asType.fullName
            List(c.parse(s""""${s.name.toString}" -> implicitly[Manifest[$typeName]]"""))
          }
        }

        Apply(
          Select(
            reify(Map).tree,
            TermName("apply")
          ),
          symbol.asClass.knownDirectSubclasses.toList.flatMap(x => toEntity(x))
        )
      }
    }
  }
}