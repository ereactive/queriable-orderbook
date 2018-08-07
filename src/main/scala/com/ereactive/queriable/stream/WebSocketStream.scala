package com.ereactive.queriable.stream

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.Message
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}
import akka.util.ByteString

import scala.concurrent.duration.Duration

class WebSocketStream(
  socketUrl: String, subscriptions: Seq[String],
  streamFlow: Flow[Message, (String, ByteString), NotUsed],
  sinkConnector: Sink[(String, ByteString), NotUsed])(
  implicit actorSystem: ActorSystem, actorMaterializer: ActorMaterializer) {
  def socketConnector() = WebSocketConnector(socketUrl, subscriptions)
  def connect() = socketConnector().start(
    sinkConnector, streamFlow
  )
  connectAfterDelay(Duration.apply(1, "s"), () => connect())
}

object WebSocketStream {
  def apply(socketUrl: String, sinkConnector: Sink[(String, ByteString), NotUsed],
    streamFlow: Flow[Message, (String, ByteString), NotUsed], subscriptions: Seq[String] = Seq.empty)(
    implicit actorSystem: ActorSystem, actorMaterializer: ActorMaterializer)
  : WebSocketStream = new WebSocketStream(socketUrl, subscriptions, streamFlow, sinkConnector)
}
