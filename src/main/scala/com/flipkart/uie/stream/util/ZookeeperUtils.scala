package com.flipkart.uie.stream.util

import java.util.Properties

import kafka.utils.ZKStringSerializer
import org.I0Itec.zkclient.ZkClient
import org.I0Itec.zkclient.serialize.ZkSerializer

/**
 * Created by rakesh.h on 13/07/15.
 */
object ZookeeperUtils {
    def createClient(config: Properties = KafkaConfig(),
                     sessionTimeout: Int = 10000,
                     connTimeOut: Int = 10000,
                     serializer: ZkSerializer = ZKStringSerializer): ZkClient = {
      val host = config.getProperty("zookeeper.connect")
      new ZkClient(host, sessionTimeout, connTimeOut, serializer)
    }
}
