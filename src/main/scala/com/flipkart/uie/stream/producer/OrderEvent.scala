package com.flipkart.uie.stream.producer

/**
 * Created by rakesh.h on 13/07/15.
 */
case class OrderEvent(userId: String, orderId: Long, orderCount: Int = 1, orderAmount: Double)
