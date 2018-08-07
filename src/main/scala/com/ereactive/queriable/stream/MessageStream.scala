package com.ereactive.queriable.stream

import java.util.UUID

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.model.ws.TextMessage.{Streamed, Strict}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import orderbook._
import io.circe.Printer
import io.circe.syntax._

import scala.concurrent.Future

object MessageStream {
  def connectionFlow(implicit mat: ActorMaterializer): Flow[Message, String, NotUsed] =
    Flow[Message]
      .mapAsync(1) {
        case Streamed(textStream) => textStream.runFold("")(_ ++ _)
        case Strict(text) => Future.successful(text)
      }
  def orderBookPrettifier(implicit mat: ActorMaterializer): Flow[OB, (String, ByteString), NotUsed] =
    Flow[OB]
      .collect {
        case ob: OrderBook =>
          (UUID.randomUUID().toString, ByteString(
            ob.asJson
              .pretty(
                Printer.noSpaces.copy(dropNullValues = true))
          ))
      }
}
