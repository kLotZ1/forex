package forex.http.client

import forex.domain.{Currency, Price, Rate, Timestamp}
import io.circe.generic.extras.Configuration
import io.circe.{Decoder, HCursor}

import java.time.OffsetDateTime

object Protocol {
  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  final case class RateResponse(
      from: Currency,
      to: Currency,
      bid: Price,
      ask: Price,
      price: Price,
      timestamp: Timestamp
  )

  object RateResponse{
    def toRate(response: RateResponse): Rate = {
      Rate(
        pair = Rate.Pair(from = response.from, to = response.to),
        price = response.price,
        timestamp = response.timestamp
      )
    }
  }

  implicit val currencyDecoder: Decoder[Currency] = Decoder.decodeString.map(Currency.fromString)

  implicit val priceDecoder: Decoder[Price] = Decoder.decodeBigDecimal.map(Price.apply)

  implicit val timestampDecoder: Decoder[Timestamp] = Decoder.decodeString.emap { str =>
    try {
      Right(Timestamp(OffsetDateTime.parse(str)))
    } catch {
      case e: Exception => Left(s"Invalid timestamp format: ${e.getMessage}")
    }
  }

  implicit val rateResponseDecoder: Decoder[RateResponse] = (c: HCursor) => for {
    from <- c.downField("from").as[Currency]
    to <- c.downField("to").as[Currency]
    bid <- c.downField("bid").as[Price]
    ask <- c.downField("ask").as[Price]
    price <- c.downField("price").as[Price]
    timestamp <- c.downField("time_stamp").as[Timestamp]
  } yield RateResponse(from, to, bid, ask, price, timestamp)

  implicit val rateResponseListDecoder: Decoder[List[RateResponse]] = Decoder.decodeList[RateResponse]
}
