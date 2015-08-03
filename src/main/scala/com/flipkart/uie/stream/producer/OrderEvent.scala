package com.flipkart.uie.stream.producer

import org.joda.time.DateTime

/**
 * Created by rakesh.h on 13/07/15.
 */
case class OrderEvent(userId: String, orderId: Long, orderCount: Int = 1, orderAmount: Double, time: DateTime = DateTime.now)
