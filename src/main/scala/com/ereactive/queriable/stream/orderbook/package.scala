package com.ereactive.queriable.stream

import java.sql.{Date, Timestamp}

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{Message, WebSocketUpgradeResponse}
import akka.pattern.after
import enumeratum._
import io.circe.syntax._
import io.circe.Decoder.Result
import io.circe.{Decoder, Encoder, HCursor, Json}

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.{Failure, Success}

package object orderbook {

  type Price = Double

  sealed trait OB
  case object Empty extends OB
  case class OrderBook(
    channelId: Option[Long],
    bids: Map[Price, OrderGroup],
    asks: Map[Price, OrderGroup],
    timestamp: Option[Timestamp] = None,
    date: Option[Date] = None
  ) extends OB

  final case class OrderGroup(price: Price, amount: Double, count: Option[Int] = None)

  object OrderGroup {
    def fromBitfinex(bits: Seq[Json], startFrom: Int = 0): OrderGroup = {
      val price = bits(startFrom).asNumber.get.toDouble
      val count = bits(startFrom+1).asNumber.flatMap(_.toInt).get
      val amount = bits(startFrom+2).asNumber.get.toDouble
      OrderGroup(price, amount, Some(count))
    }
  }

  implicit final val TimestampFormat : Encoder[Timestamp] with Decoder[Timestamp] = new Encoder[Timestamp] with Decoder[Timestamp] {
    override def apply(a: Timestamp): Json = Encoder.encodeString(a.toString)
    override def apply(c: HCursor): Result[Timestamp] = Decoder.decodeString.map(s => Timestamp.valueOf(s))(c)
  }

  implicit final val encodeDateObject: Encoder[Date] = new Encoder[Date] with Decoder[Date] {
    override def apply(date: Date): Json = Encoder.encodeString(date.toString)
    override def apply(c: HCursor): Result[Date] = Decoder.decodeString.map(s => Date.valueOf(s))(c)
  }

  implicit final val orderGroupEncoder: Encoder[OrderGroup] = Encoder.instance(og =>
    Json.obj(
      ("price", og.price.asJson),
      ("amount", og.amount.asJson),
      ("count", og.count.asJson)
    )
  )

  implicit final val orderBookEncoder: Encoder[OrderBook] = Encoder.instance(ob =>
    Json.obj(
      ("channelId", ob.channelId.asJson),
      ("bids", Json.arr(
        ob.bids.map { case (_, order) =>
          order.asJson
        }.toSeq:_*).asJson
      ),
      ("asks", Json.arr(
        ob.asks.map { case (_, order) =>
          order.asJson
        }.toSeq:_*).asJson
      ),
      ("timestamp", ob.timestamp.asJson),
      ("date", ob.date.asJson)
    )
  )

  final case class Pair(from: Coin, to: Coin)

  sealed abstract private[orderbook] class Coin(override val entryName: String) extends EnumEntry

  object Coin extends Enum[Coin] {
    val values = findValues
    case object USD extends Coin("USD")
    case object BTC extends Coin("BTC")
    case object USDT extends Coin("USDT")
    case object ETH extends Coin("ETH")
  }
}
