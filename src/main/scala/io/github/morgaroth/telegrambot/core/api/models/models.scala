package io.github.morgaroth.telegrambot.core.api.models

import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpEntity.Strict
import akka.http.scaladsl.model.Multipart.FormData
import akka.http.scaladsl.model.Multipart.FormData.BodyPart
import akka.util.ByteString
import io.github.morgaroth.base.MMarshalling
import io.github.morgaroth.telegrambot.core.api.models.formats.Keyboard

import scala.language.implicitConversions

sealed trait Command


object formats extends MMarshalling {
  type Keyboard = Either[Either[ReplyKeyboardMarkup, ReplyKeyboardHide], ForceReply]

  implicit def convertToKeyboard(rkm: ReplyKeyboardMarkup): Option[Keyboard] = Some(Left(Left(rkm)))

  implicit def convertToKeyboard(rkm: ReplyKeyboardHide): Option[Keyboard] = Some(Left(Right(rkm)))

  implicit def convertToKeyboard(rkm: ForceReply): Option[Keyboard] = Some(Right(rkm))

  val Markdown = Some("Markdown")

  type DI = DummyImplicit

  def convBP(t: (String, java.io.File))(implicit di: DI): BodyPart = BodyPart.fromPath(t._1, ContentTypes.`application/octet-stream`, t._2.toPath)

  def convBP(t: (String, Int))(implicit di: DI, di2: DI): BodyPart = BodyPart(t._1, Strict(ContentTypes.`text/plain(UTF-8)`, ByteString(t._2.toString)))

  def convBP(t: (String, String))(implicit di: DI, di2: DI, di3: DI): BodyPart = BodyPart(t._1, Strict(ContentTypes.`text/plain(UTF-8)`, ByteString(t._2)))

  def convBP(t: (String, Keyboard))(implicit di: DI, di2: DI, di3: DI, di4: DI): BodyPart = BodyPart(t._1, Strict(ContentTypes.`application/json`, ByteString(MJson.write(t._2))))

}

import io.github.morgaroth.telegrambot.core.api.models.formats._

/**
  * https://core.telegram.org/bots/api#audio
  */
case class Audio(
                  file_id: String,
                  duration: Int,
                  mime_type: String,
                  file_size: Int,
                  title: String,
                  performer: String
                )

/**
  * https://core.telegram.org/bots/api#contact
  */
case class Contact(
                    phone_number: String,
                    first_name: String,
                    last_name: Option[String],
                    user_id: Option[Int]
                  )

/**
  * https://core.telegram.org/bots/api#document
  */
case class Document(
                     file_id: String,
                     thumb: Option[PhotoSize],
                     file_name: Option[String],
                     mime_type: Option[String],
                     file_size: Option[Int]
                   )

/**
  * https://core.telegram.org/bots/api#file
  */
case class File(
                 file_id: String,
                 file_size: Option[Int],
                 file_path: Option[String]
               )


/**
  * https://core.telegram.org/bots/api#location
  */
case class Location(
                     longitude: Double,
                     latitude: Double
                   )

/**
  * https://core.telegram.org/bots/api#message
  */
case class Message(
                    message_id: Int,
                    from: User,
                    date: Long,
                    chat: Either[User, GroupChat],
                    forward_from: Option[User],
                    forward_date: Option[Long],
                    text: Option[String],
                    audio: Option[Audio],
                    document: Option[Document],
                    photo: Option[List[PhotoSize]],
                    sticker: Option[Sticker],
                    video: Option[Video],
                    contact: Option[Contact],
                    location: Option[Location],
                    new_chat_participant: Option[User],
                    left_chat_participant: Option[User],
                    new_chat_title: Option[String],
                    new_chat_photo: Option[List[PhotoSize]],
                    delete_chat_photo: Option[Boolean],
                    group_chat_created: Option[Boolean],
                    reply_to_message: Option[Message]
                  ) {
  def chatId = chat.chatId

  override def toString: String = {
    var separator: Option[String] = None

    def sep = {
      val res = separator.getOrElse("")
      separator = separator orElse Some(", ")
      res
    }

    val sb = StringBuilder.newBuilder
    sb.append("Message(")
    sb.append(s"${sep}message_id=$message_id")
    sb.append(s"${sep}from=$from")
    sb.append(s"${sep}date=$date")
    sb.append(s"${sep}chat=$chat")
    sb.append(forward_from.map(x => s"${sep}forward_from=${x.toString}").getOrElse(""))
    sb.append(forward_date.map(x => s"${sep}forward_date=${x.toString}").getOrElse(""))
    sb.append(text.map(x => s"${sep}text=${x.toString}").getOrElse(""))
    sb.append(audio.map(x => s"${sep}audio=${x.toString}").getOrElse(""))
    sb.append(document.map(x => s"${sep}document=${x.toString}").getOrElse(""))
    sb.append(photo.map(x => s"${sep}photo=${x.mkString}").getOrElse(""))
    sb.append(sticker.map(x => s"${sep}sticker=${x.toString}").getOrElse(""))
    sb.append(video.map(x => s"${sep}video=${x.toString}").getOrElse(""))
    sb.append(contact.map(x => s"${sep}contact=${x.toString}").getOrElse(""))
    sb.append(location.map(x => s"${sep}location=${x.toString}").getOrElse(""))
    sb.append(new_chat_participant.map(x => s"${sep}new_chat_participant=${x.toString}").getOrElse(""))
    sb.append(left_chat_participant.map(x => s"${sep}left_chat_participant=${x.toString}").getOrElse(""))
    sb.append(new_chat_title.map(x => s"${sep}new_chat_title=${x.toString}").getOrElse(""))
    sb.append(new_chat_photo.map(x => s"${sep}new_chat_photo=${x.mkString}").getOrElse(""))
    sb.append(delete_chat_photo.map(x => s"${sep}delete_chat_photo=${x.toString}").getOrElse(""))
    sb.append(group_chat_created.map(x => s"${sep}group_chat_created=${x.toString}").getOrElse(""))
    sb.append(reply_to_message.map(x => s"${sep}reply_to_message=${x.toString}").getOrElse(""))
    sb.append(")")
    sb.mkString
  }
}

/**
  * https://core.telegram.org/bots/api#photosize
  */
case class PhotoSize(
                      file_id: String,
                      width: Int,
                      height: Int,
                      file_size: Option[Int]
                    )

/**
  * https://core.telegram.org/bots/api#replykeyboardmarkup
  */
case class ReplyKeyboardMarkup(
                                keyboard: List[List[String]],
                                resize_keyboard: Option[Boolean] = None,
                                one_time_keyboard: Option[Boolean] = None,
                                selective: Option[Boolean] = None
                              )

object ReplyKeyboardMarkup {
  def once(keyboard: List[List[String]], resize_keyboard: Boolean = true, selective: Boolean = false) =
    apply(keyboard, Option(resize_keyboard).filter(identity), Some(true), Option(selective).filter(identity))

  def long(keyboard: List[List[String]], resize_keyboard: Boolean = true, selective: Boolean = false) =
    apply(keyboard, Option(resize_keyboard).filter(identity), Some(false), Option(selective).filter(identity))
}

/**
  * https://core.telegram.org/bots/api#replykeyboardhide
  */
case class ReplyKeyboardHide(
                              hide_keyboard: Boolean = true,
                              selective: Option[Boolean] = None
                            )

/**
  * https://core.telegram.org/bots/api#forcereply
  */
case class ForceReply(
                       selective: Option[Boolean] = None,
                       force_reply: Boolean = true
                     )

object ForceReply {
  def selective = apply(Some(true))
}

/**
  * https://core.telegram.org/bots/api#sticker
  */
case class Sticker(
                    file_id: String,
                    width: Int,
                    height: Int,
                    file_size: Option[Int]
                  )

/**
  * https://core.telegram.org/bots/api#user
  */
case class User(
                 id: Int,
                 first_name: String,
                 last_name: Option[String],
                 username: Option[String]
               ) {
  def getAnyUserName = {
    username.map(nick => s"@$nick").getOrElse {
      last_name.map(x => s"$first_name $x").getOrElse(first_name)
    }
  }

  def uber = UberUser(id, first_name, "user", last_name, username)
}

/**
  * https://core.telegram.org/bots/api#groupchat
  */
case class GroupChat(
                      id: Int,
                      title: String
                    )

/**
  * https://core.telegram.org/bots/api#userprofilephotos
  */
case class UserProfilePhotos(
                              total_count: Int,
                              photos: List[PhotoSize]
                            )

/**
  * https://core.telegram.org/bots/api#update
  */
case class Update(
                   update_id: Int,
                   message: Message
                 )

/**
  * https://core.telegram.org/bots/api#video
  */
case class Video(
                  file_id: String,
                  width: Int,
                  height: Int,
                  duration: Int,
                  thumb: PhotoSize,
                  mime_type: Option[String],
                  file_size: Option[Int]
                )

/**
  * https://core.telegram.org/bots/api#voice
  */
case class Voice(
                  file_id: String,
                  duration: Int,
                  mime_type: Option[String],
                  file_size: Option[Int]
                )

/**
  * https://core.telegram.org/bots/api#sendmessage
  */
case class SendMessage(
                        chat_id: Int,
                        text: String,
                        parse_mode: Option[String] = None,
                        disable_web_page_preview: Option[Boolean] = None,
                        reply_to_message_id: Option[Int] = None,
                        reply_markup: Option[Keyboard] = None
                      ) extends Command

/**
  * https://core.telegram.org/bots/api#forwardmessage
  */
case class ForwardMessage(
                           chat_id: Int,
                           from_chat_id: Int,
                           message_id: Int
                         ) extends Command

/**
  * https://core.telegram.org/bots/api#sendphoto
  */
case class SendPhoto(
                      chat_id: Int,
                      photo: Either[java.io.File, String],
                      caption: Option[String] = None,
                      reply_to_message_id: Option[Int] = None,
                      reply_markup: Option[Keyboard] = None
                    ) extends Command {

  def toForm = FormData(
    Seq(convBP("chat_id" -> chat_id), photo.fold(f => convBP("photo", f), id => convBP("photo", id))) ++
      caption.map(x => convBP("caption" -> x)) ++
      reply_to_message_id.map(x => convBP("reply_to_message_id" -> x)) ++
      reply_markup.map(x => convBP("reply_markup" -> x)): _*
  )
}

///**
//  * https://core.telegram.org/bots/api#sendaudio
//  */
//case class SendAudio(
//                      chat_id: Int,
//                      audio: Either[java.io.File, String],
//                      duration: Option[Int] = None,
//                      performer: Option[String] = None,
//                      title: Option[String] = None,
//                      reply_to_message_id: Option[Int] = None,
//                      reply_markup: Option[Keyboard] = None
//                    ) extends Command {
//
//  def toForm =
//    audio.left.map(data =>
//      FormData((
//        Seq(convBP("chat_id" -> chat_id), convBP("audio" -> data)) ++
//          duration.map(x => convBP("duration" -> x)) ++
//          performer.map(x => convBP("performer" -> x)) ++
//          title.map(x => convBP("title" -> x)) ++
//          reply_to_message_id.map(x => convBP("reply_to_message_id" -> x)) ++
//          reply_markup.map(x => convBP("reply_markup" -> x))): _*
//      ).right.map(data_id =>
//        FormData(
//          Seq(convFD("chat_id" -> chat_id), convFD("audio" -> data_id)) ++
//            duration.map(x => convFD("duration" -> x)) ++
//            performer.map(x => convFD("performer" -> x)) ++
//            title.map(x => convFD("title" -> x)) ++
//            reply_to_message_id.map(x => convFD("reply_to_message_id" -> x)) ++
//            reply_markup.map(x => convFD("reply_markup" -> x))
//        ))
//
//}

/**
  * https://core.telegram.org/bots/api#senddocument
  */
case class SendDocument(
                         chat_id: Int,
                         document: Either[java.io.File, String],
                         reply_to_message_id: Option[Int] = None,
                         reply_markup: Option[Keyboard] = None
                       ) extends Command {
  def toForm = FormData(Seq(convBP("chat_id" -> chat_id),
    document.fold(data => convBP("document" -> data), d => convBP("document", d))) ++
    reply_markup.map(x => convBP("reply_markup" -> x)) ++
    reply_to_message_id.map(x => convBP("reply_to_message_id" -> x)): _*
  )
}

/**
  * https://core.telegram.org/bots/api#sendlocation
  */
case class SendLocation(
                         chat_id: Int,
                         latitude: Double,
                         longitude: Double,
                         reply_to_message_id: Option[Int],
                         reply_markup: Option[Keyboard]
                       ) extends Command

/**
  * https://core.telegram.org/bots/api#sendchataction
  */
case class SendChatAction(
                           chat_id: Int,
                           action: String
                         ) extends Command

object Action {
  val `typing...` = "typing"
  val `uploading photo...` = "upload_photo"
  val `uploading document...` = "upload_document"
  val `location...` = "find_location"
  val `recording video...` = "record_video"
  val `uploading video...` = "upload_video"
  val `recording audio...` = "record_audio"
  val `uploading audio...` = "upload_audio"
}

/**
  * https://core.telegram.org/bots/api#getuserprofilephotos
  */
case class GetUserProfilePhotos(
                                 user_id: Int,
                                 offset: Option[Int],
                                 limit: Option[Int]
                               ) extends Command

/**
  * https://core.telegram.org/bots/api#getfile
  */
case class GetFile(
                    file_id: String
                  ) extends Command