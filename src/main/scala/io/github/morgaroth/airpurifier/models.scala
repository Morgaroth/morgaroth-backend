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

case class DeviceStatus(
                         aqi: Int,
                         bright: Option[String],
                         buzzer: String,
                         child_lock: String,
                         f1_hour_used: Int,
                         favorite_level: Int,
                         filter1_life: Int,
                         humidity: Int,
                         led: String,
                         led_b: Int,
                         mode: String,
                         motor1_speed: Int,
                         power: String,
                         temp_dec: Int,
                         use_time: Int,
                       )

case class Device(
                   ip: String,
                   status: DeviceStatus,
                   status_raw: String,
                   kind: String,
                 )

case class DevicesList(devices: List[Device]) extends MiioResponse

case class StrTask(property: String, value: String)

case class IntTask(property: String, value: Int)