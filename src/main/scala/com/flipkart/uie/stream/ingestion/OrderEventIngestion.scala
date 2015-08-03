package com.flipkart.uie.stream.ingestion

import _root_.redis.RedisClient
import com.flipkart.uie.stream.producer.OrderEvent
import com.flipkart.uie.stream.util.KafkaConfig
import com.flipkart.uie.time.persistence.{Writer, Reader}
import com.flipkart.uie.time.tree.{Life, TimedEvent, TimeTree}
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}
import kafka.serializer.StringDecoder
import org.joda.time.DateTime
import play.api.libs.functional.Monoid
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.nscala_time.time.Imports._
import Monoids._

/**
 * Created by rakesh.h on 13/07/15.
 */
object OrderEventIngestion {
  def turnOffLogging() = {
    import org.apache.log4j.Logger
    import org.apache.log4j.Level

    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)
  }

  def main(args: Array[String]) {
    val conf = new SparkConf()
      .setMaster("local[2]")
      .setAppName("Test Spark")
      .set("spark.executor.memory", "4g")
      .set("spark.rdd.compress", "true")
      .set("spark.storage.memoryFraction", "1")
    turnOffLogging()

    val ssc = new StreamingContext(conf,  Seconds(1))

    /**
     * important, checkpoint is the directory where spark saves the state,
     * without this directory it wont let compute you state, it is required in case of failure
     * without it, it will have to remember whole history of a user in order to compute state in event of failure
     */
    ssc.checkpoint("/Users/rakesh.h/checkpoint/ordercount")


    val kafkaConfig = KafkaConfig()
    val key = "metadata.broker.list"
    val props = Map[String, String](key -> kafkaConfig.get(key).asInstanceOf[String])
    val orderTopic = Set("orderEvent")
    val stream = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, props,
                                                                                              orderTopic)

    /**
     * we are mapping a raw stream of json to string to a stream of OrderEvent objects, bad values
     * are filtered for time being.
     */
    val orderEvents = stream.map{
      case (key, value) => { //key is null, need to check with bigfoot their message structure
        parse(value)
      }
    }.filter(opt => !opt.isEmpty).map(_.get) //we have list orders now, bad orders have been filtered

    /**
     *
     * create a key value RDD, key is user_id value is order amount, orderAmount is the state
     * that we will be tracking
     * a RDD of type (Key, Value) is an important concept in RDD(it lets us join and many other key based operations)
     */
    val orderByUserId = orderEvents.map(oe => (oe.userId, TimedEvent(oe.time, oe.orderAmount)))


    /**
     * we have a RDD with key as the userId and value as the total orderAmount for that user,
     * do anything with it now(write to DB etc)
     */
   // val totalOrderRDD = orderByUserId.updateStateByKey(updateOrderCount _)
    import Reader._
    import Writer._
    import TimeTree._

    orderByUserId.foreachRDD{rdd =>
      rdd.foreachPartition {itr =>
        implicit val akkaSystem = akka.actor.ActorSystem()
        val interval = DateTime.now - 2.month to DateTime.now

        val redis = RedisClient()
        itr.foreach {
          case (account, timedEvent) => {
            for {
              max <- redis.get(account + "-max")
              sum <- redis.get(account + "-sum")
              avg <- redis.get(account + "-avg")
            } {
              val maxStr = max.map(_.utf8String).getOrElse("{}")
              val maxLife = maxStr.read(_.toDouble)

              maxLife.foreach{ life =>
                val newLife = TimeTree.insert(life, timedEvent)(0)(_ max _)
                val ss = newLife.write(_.toString)
                val max = TimeTree.flattenLife(newLife, interval)(maxMonoid)
                println(max)
                redis.set(account + "-max", ss)
              }

              val sumStr = sum.map(_.utf8String).getOrElse("{}")
              val sumLife = sumStr.read(_.toDouble)

              sumLife.foreach{ life =>
                val newLife = TimeTree.insert(life, timedEvent)(0)(_ + _)
                val ss = newLife.write(_.toString)
                val sum = TimeTree.flattenLife(newLife, interval)(sumMonoid)
                println(sum)
                redis.set(account + "-sum", ss)
              }

              val avgStr = avg.map(_.utf8String).getOrElse("{}")
              val avgLife = avgStr.read{str =>
                val splits = str.split("%")
                (splits(0).toDouble, splits(1).toInt)
              }

              avgLife.foreach{ life =>
                val newLife = TimeTree.insert(life, timedEvent)((0.00, 0))((a,b) =>(a._1 + b, a._2 + 1))
                val ss = newLife.write(a => a._1 + "%" + a._2)
                val avg = TimeTree.flattenLife(newLife, interval)(avgOrder)
                println(avg)
                redis.set(account + "-avg", ss)
              }

            }
          }
        }

      }

    }


   // totalOrderRDD.print()
    ssc.start()
    ssc.awaitTermination()
  }

  /**
   *
   * @param orderAmounts all the order amount for a SINGLE user(this is our key), that are present in each batch
   * @param totalOrderState total order amount state for the user
   * @return
   */
  def updateOrderCount(orderAmounts: Seq[Double], totalOrderState: Option[Double]): Option[Double] = {
    val totalOrder = totalOrderState.getOrElse(0.0) //first time, state will not be present, return 0 in that case
    Some(totalOrder + orderAmounts.sum)
  }

  /**
   * parses a string of type json to object of type OrderEvent,
   * bad json string are ignored, should be handled properly
   * @param jsonOrder
   * @return
   */
  def parse(jsonOrder: String): Option[OrderEvent] = {
    implicit  val reads = Json.reads[OrderEvent]
    try {
      val parsedOrder= Json.parse(jsonOrder)
      Json.fromJson(parsedOrder).asOpt
    }
    catch {
      case e: Exception => {
        e.printStackTrace
        None
      }
    }
  }

}
