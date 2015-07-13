package com.flipkart.uie.stream.drivers

import com.flipkart.uie.stream.util.TopicAdmin
import com.flipkart.uie.stream.util.ZookeeperUtils._

/**
 * Created by rakesh.h on 13/07/15.
 */
object CreateOrderEventTopic {

  def main(args: Array[String]): Unit = {
    println("Creating order event topic")
    val zkClient = createClient()
    TopicAdmin(zkClient).createTopic("orderEvent")

  }


}
