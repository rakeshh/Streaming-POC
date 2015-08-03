package com.flipkart.uie.stream.util

import org.joda.time.{DateTimeConstants, DateTime}
import com.github.nscala_time.time.Imports._

/**
 * Created by rakesh.h on 02/08/15.
 */
object TimeUtils {

  def timeStream(start: DateTime, period: Period = 1.day):Stream[DateTime] =
        start #:: timeStream(start + period, period)

  def countedTimeStream(monthBefore: Int, dailyCount: Int): Stream[DateTime] = {
    val startTime = DateTime.now - monthBefore.months
    val milliSecs = (DateTimeConstants.MILLIS_PER_DAY / dailyCount)
    val milliSecPeriod = milliSecs.milli
    timeStream(startTime, milliSecPeriod)
  }

}
