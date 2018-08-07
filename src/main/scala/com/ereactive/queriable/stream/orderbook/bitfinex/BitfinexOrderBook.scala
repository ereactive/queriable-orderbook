package com.briefml.trade.orderbook.bitfinex

import java.sql.{Date, Timestamp}
import java.time.LocalDateTime

import com.ereactive.queriable.stream.orderbook.{Empty, OB, OrderBook, OrderGroup}
import io.circe.Json
import io.circe.parser.parse

import scala.util.Try

object BitfinexOrderBook {
  def parseOrderBook(msg: String, orderBook: OB): OB = {
    val json: Json = parse(msg).getOrElse(Json.arr())

    orderBook match {
      case Empty =>
        getOrderBook(json)
      case ob: OrderBook =>
        getOrderBook(json) match {
          case Empty =>
            val bits: Seq[Json] = json.asArray.getOrElse(Vector.empty)
            val tryHb = Try(bits(1)).map(!_.asString.contains("hb"))
            if (tryHb.getOrElse(false)) {
              val now = Timestamp.valueOf(LocalDateTime.now())
              Try(OrderGroup.fromBitfinex(bits, 1)) map { update =>
                val updatedOrderBook = if (update.count.contains(0)) ob.copy(bids = ob.bids - update.price).copy(asks = ob.asks - update.price)
                else if (update.amount < 0) ob.copy(asks = ob.asks + (update.price -> update.copy(amount = update.amount.abs)))
                else ob.copy(bids = ob.bids + (update.price -> update))
                updatedOrderBook.copy(timestamp = Some(now), date = Some(new Date(now.getTime)))
              } getOrElse ob
            } else ob
          case fullOderBook => fullOderBook
        }
    }
  }

  private def getOrderBook(json: Json): OB = {
    val inner = json.asArray.getOrElse(Vector.empty).drop(1)
    if (inner.nonEmpty && inner.forall(_.asArray.nonEmpty)) {
      val channelIdVec = json.asArray.getOrElse(Vector.empty).take(1)
        .map(_.asNumber.flatMap(_.toLong).getOrElse(-1l))
      channelIdVec.map { channelId  =>
        val orders: Vector[OrderGroup] = inner.flatMap(
          _.asArray.getOrElse(Vector.empty).map { json =>
            val bits = json.asArray.getOrElse(Vector.empty)
            OrderGroup.fromBitfinex(bits)
          })
        val eventTime = Timestamp.valueOf(LocalDateTime.now())
        val (bids, asks) = orders.partition(_.amount > 0)
        OrderBook(Some(channelId),
          bids.map(b => b.price -> b).toMap,
          asks.map(a => a.price -> a.copy(amount = a.amount.abs)).toMap,
          timestamp = Some(eventTime),
          date = Some(new Date(eventTime.getTime))
        )
      }.headOption.getOrElse(Empty)
    }
    else Empty
  }
}