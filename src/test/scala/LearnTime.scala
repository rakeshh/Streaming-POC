import com.flipkart.uie.stream.util.TimeUtils
import com.github.nscala_time.time.Imports._
import org.joda.time.DateTime

/**
 * Created by rakesh.h on 02/08/15.
 */
object LearnTime extends App{
  println(DateTime.now + 2.months)
 // print((DateTime.now - 6.months) to DateTime.now)


  TimeUtils.countedTimeStream(1, 10).filter(t => t <= DateTime.now).foreach(println)

}

