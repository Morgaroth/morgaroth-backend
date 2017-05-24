package io.github.morgaroth.base

import org.json4s.{DefaultFormats, native}
import org.json4s.native.Serialization

/**
  * Created by PRV on 24.05.2017.
  */
trait MMarshalling {

  implicit val formats = DefaultFormats

  implicit val serializationLibrary: org.json4s.Serialization = native.Serialization

  val MJson = Serialization
}
