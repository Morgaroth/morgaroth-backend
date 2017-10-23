package io.github.morgaroth.airpurifier

sealed trait MiioResponse


case class Error(
                  errorCode: Int,
                  message: String,
                  errorClass: Option[String],
                ) extends MiioResponse

case class HttpError(
                      code: Int,
                      info: String,
                    ) extends MiioResponse

case class Device(
                   ip: String,
                   status: Map[String, String],
                   statusRaw: String,
                   kind: String,
                 )

case class DevicesList(devices: List[Device]) extends MiioResponse

case class StrTask(property: String, value: String)

case class IntTask(property: String, value: Int)