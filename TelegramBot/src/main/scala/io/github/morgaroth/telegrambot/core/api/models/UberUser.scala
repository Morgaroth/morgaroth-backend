package io.github.morgaroth.telegrambot.core.api.models

/**
  * Created by PRV on 24.05.2017.
  */
case class UberUser(id: Int,
                    firstName: String,
                    kind: String,
                    lastName: Option[String] = None,
                    username: Option[String] = None
                     ) {
                       def getAnyUserName = {
                         username.map(nick => s"@$nick").getOrElse {
                           lastName.map(x => s"$firstName $x").getOrElse(firstName)
                         }
                       }
                     }
