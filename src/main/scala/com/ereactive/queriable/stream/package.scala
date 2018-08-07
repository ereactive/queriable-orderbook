package com.ereactive.queriable

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{Message, WebSocketUpgradeResponse}
import akka.pattern.after
import scala.concurrent.{Future, Promise}
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.{Failure, Success}

package object stream {
  def connectAfterDelay(delay: FiniteDuration,
    f: () => (Future[WebSocketUpgradeResponse], Promise[Option[Message]]))(
    implicit system: ActorSystem): Future[Unit] = {
    val sc = system.scheduler
    import system.dispatcher
    after(delay, sc)(Future {
      val reconnectDelay = Duration(30, "s")
      val (connectionUpgrade, promise) = f()
      promise.future.onComplete { _ =>
        system.log.warning("websocket connection terminated")
        system.log.info(s"reconnecting to stream after delay $reconnectDelay")
        connectAfterDelay(reconnectDelay, f)
      }
      connectionUpgrade.onComplete {
        case Success(validUpgrade) => println(validUpgrade)
        case Failure(e) => println(e.getMessage)
      }
    })
  }
}
