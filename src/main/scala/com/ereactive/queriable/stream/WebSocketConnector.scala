package com.ereactive.queriable.stream

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest, WebSocketUpgradeResponse}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.concurrent.{Future, Promise}

class WebSocketConnector(
  wsUrl: String,
  subscriptionMessages: Seq[String])(
  implicit system: ActorSystem,
  materializer: ActorMaterializer) {

  private def websocketFlow[T](sink: Sink[T, NotUsed], messageFlow: Flow[Message, T, NotUsed])
  : Flow[Message, Message, Promise[Option[Message]]] =
    Flow.fromSinkAndSourceMat(Flow[Message]
      .via(messageFlow)
      .to(sink),
      Source(subscriptionMessages.map(TextMessage(_)).toList)
        .concatMat(Source.maybe[Message])(Keep.right)
    )(Keep.right)

  def start[T](sink: Sink[T, NotUsed], messageFlow: Flow[Message, T, NotUsed])
  : (Future[WebSocketUpgradeResponse], Promise[Option[Message]]) =
    Http().singleWebSocketRequest(WebSocketRequest(wsUrl), websocketFlow(sink, messageFlow))
}

object WebSocketConnector {
  def apply(wsUrl: String, subscriptionRequest: Seq[String])(
    implicit system: ActorSystem, mat: ActorMaterializer): WebSocketConnector = {
    new WebSocketConnector(wsUrl, subscriptionRequest)
  }
}
