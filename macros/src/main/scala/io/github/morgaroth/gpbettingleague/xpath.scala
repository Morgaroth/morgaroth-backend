package io.github.morgaroth.gpbettingleague

import javax.xml.xpath.XPathFactory

import contextual._

import scala.language.implicitConversions
import scala.util.Try

object xpath {

  object XpathParser extends Interpolator {

    type Output = String

    def contextualize(interpolation: StaticInterpolation) = {
      val lit@Literal(_, xpathExpr) = interpolation.parts.head
      val result = Try(Left(XPathFactory.newInstance.newXPath.compile(xpathExpr))).recover {
        case t: Throwable =>
          Right(t.getMessage)
      }.get
      if (result.isRight) interpolation.abort(lit, 0, s"Invalid XPath: ${result.right.get}")
      Nil
    }

    def evaluate(interpolation: RuntimeInterpolation): String = interpolation.literals.head
  }

  implicit class XpathContext(sc: StringContext) {
    val x = Prefix(XpathParser, sc)
  }

}