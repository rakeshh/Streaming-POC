package com.flipkart.uie.stream.util

import java.util.Properties
import KafkaConfig._
import com.typesafe.config.ConfigFactory

/**
 * Created by rakesh.h on 13/07/15.
 */
trait KafkaConfig extends Properties{
  private val allKeys = Seq(brokers, serializer, zookeeper)
  lazy val config = ConfigFactory.load()
  //by default it will load application.conf in resource directory, check that file

  allKeys.map{key =>
    if (config.hasPath(key))
      put(key.replace("producer.", ""), config.getString(key))
  }


}

object KafkaConfig {
  val producerPrefix = "producer"

  val brokers = s"$producerPrefix.metadata.broker.list"
  val serializer = s"$producerPrefix.serializer.class"
  val zookeeper  = s"$producerPrefix.zookeeper.connect"

  def apply() = new KafkaConfig {}
}
