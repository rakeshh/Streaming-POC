package com.flipkart.uie.stream.util

import kafka.admin.AdminUtils
import org.I0Itec.zkclient.ZkClient

/**
 * Created by rakesh.h on 13/07/15.
 */
case class TopicAdmin(zkClient: ZkClient) {

  def createTopic(name: String, partitionCount: Int = 1, replication: Int = 1): Unit = {
    AdminUtils.createTopic(zkClient, name, partitionCount, replication)
  }

}
