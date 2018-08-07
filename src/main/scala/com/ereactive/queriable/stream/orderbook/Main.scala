package com.ereactive.queriable.stream.orderbook

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.stream.Supervision.{Decider, Restart}
import akka.stream.scaladsl.{Flow, Sink}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import com.ereactive.queriable.stream.WebSocketStream
import com.typesafe.config.ConfigFactory

object Main {

  def main(args: Array[String]): Unit = {

    import SystemSettings._

    startStream("wss://api.bitfinex.com/ws", "bitfinex-orderbook", List(
      """
        |{
        |  "event": "subscribe",
        |  "channel": "book",
        |  "pair": "BTCUSD",
        |  "prec": "P0",
        |  "freq": "F0"
        |}
      """.stripMargin)
    )

  }

  def startStream(socketUrl: String, firehoseName: String, subscriptions: Seq[String])(
    implicit actorSystem: ActorSystem, actorMaterializer: ActorMaterializer): Unit =
    WebSocketStream(
      socketUrl,
      Flow[(String, ByteString)].map(tup => println(tup._2.utf8String)).to(Sink.ignore),
      bitfinex.statefulBitfinexFlow,
      subscriptions
    )
}

object SystemSettings extends MaterializerSettings {
  val config = ConfigFactory.load()
  implicit val system: ActorSystem = ActorSystem("trade-stream", config)
  implicit val materializer: ActorMaterializer = ActorMaterializer(actorSettings)
}

trait MaterializerSettings {

  /** A decider that logs error messages, and resumes (Restart's) */
  private def loggingResumeDecider(log: LoggingAdapter): Decider = e => {
    log.error(e, e.getMessage)
    Restart
  }

  def actorSettings(implicit system: ActorSystem): ActorMaterializerSettings =
    ActorMaterializerSettings(system)
      .withSupervisionStrategy(loggingResumeDecider(system.log))
}
