package com.flipkart.uie.stream.drivers

import com.flipkart.uie.stream.producer.{OrderEvent, Producer}
import com.typesafe.config.ConfigFactory
import org.scalacheck.Gen
import play.api.libs.json.Json

/**
 * Created by rakesh.h on 13/07/15.
 */
object OrderEventsProducer {

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()

    // batch size is size of order event objects that will be written to kafka in one go
    val batchSize = config.getInt("producer.batch.size")

     //time in millisecons after which each batch is to be published
    val batchTime = config.getInt("producer.batch.timing")

     // number of different user ids that we want to simulate
    val userCount = config.getInt("producer.user.count")

    val producer = new Producer[String]("orderEvent")
    val orderGen = orderEventGenerator(userCount)
    val sizedGen = Gen.listOfN(batchSize, orderGen)

    while(true) {
      val orderItems  = sizedGen.sample.get
      orderItems.foreach(send(producer, _))
      Thread.sleep(batchTime)
    }

  }

  private def send(producer: Producer[String], orderEvent: OrderEvent): Unit = {
    implicit  val writes = Json.writes[OrderEvent]
    producer.send(Json.toJson(orderEvent).toString())
  }

  /**
   * generator for order events. Generator is for producing random things
   * @param usersCount
   * @return OrderEvent object
   */
  private def orderEventGenerator(usersCount: Int) : Gen[OrderEvent] = for {
    id <- Gen.oneOf(createUserIds(usersCount).toSeq) //create alphanumeric user id
    orderAmount <- Gen.choose(10.00, 1000.00) //choose order amount
    orderCount <- Gen.choose(1, 30) //choose order count
    orderId <-orderIdGen //choose order id
  } yield OrderEvent(id, orderId, orderCount, roundToTwo(orderAmount))

  /**
   * user id that starts with "AC" and of size 32 alphanumeric characters
   * @param count
   * @return
   */
  private def createUserIds(count: Int): Set[String] = {
    val alphaNumGen = Gen.listOfN(30, Gen.alphaNumChar).map(_.mkString)
    (1 to count).map { _ =>
      "AC" + alphaNumGen.sample.get.toUpperCase
    }.toSet
  }

  /**
   * order id generator of length 6 (only numbers like 342378)
   * @return
   */
  private def orderIdGen: Gen[Long] = for {
    firstChar <- Gen.choose(1, 9)
    rest <- Gen.listOfN(5, Gen.choose(0, 9))
  } yield (firstChar + rest.mkString("")).toLong

  /**
   * rounds a big decimal like 10.234433 to 10.23
   * @param input
   * @return
   */
  private def roundToTwo(input: Double): Double =
    BigDecimal(input).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble

}
