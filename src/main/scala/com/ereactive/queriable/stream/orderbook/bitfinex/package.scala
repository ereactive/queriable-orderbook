package com.ereactive.queriable.stream.orderbook

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import com.briefml.trade.orderbook.bitfinex.BitfinexOrderBook
import com.ereactive.queriable.stream.MessageStream

package object bitfinex {
  import com.ereactive.queriable.stream.orderbook.SystemSettings._
  val statefulBitfinexFlow: Flow[Message, (String, ByteString), NotUsed] =
    MessageStream.connectionFlow
      .statefulMapConcat { () =>
        var orderBookState: OB = Empty
        msg =>
          val updatedOrderBook = BitfinexOrderBook.parseOrderBook(msg, orderBookState)
          orderBookState = updatedOrderBook
          List(orderBookState)
      }
      .via(MessageStream.orderBookPrettifier)
}
