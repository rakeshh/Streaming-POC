package com.flipkart.uie.stream.producer

import java.util.Properties

import com.flipkart.uie.stream.util.KafkaConfig
import kafka.producer.{Producer => KafkaProducer, KeyedMessage, ProducerConfig}
import play.api.libs.json.Json

/**
 * Created by rakesh.h on 13/07/15.
 */
case class Producer[A](topic : String) {
  protected val config = new ProducerConfig(KafkaConfig())
  private lazy val producer = new KafkaProducer[A, A](config)

  def send(message: A) = sendMessage(producer, keyedMessage(topic, message))

  def sendStream(stream : Stream[A]) = stream.foreach(send)

  private def keyedMessage(topic: String, message: A) = new KeyedMessage[A, A](topic, message)

  private def sendMessage(producer: KafkaProducer[A,A], message: KeyedMessage[A,A]) = producer.send(message)


}

object Producer{
  def apply[A](topic: String, props: Properties) = new Producer[A](topic) {
    override val config = new ProducerConfig(props)
  }
}