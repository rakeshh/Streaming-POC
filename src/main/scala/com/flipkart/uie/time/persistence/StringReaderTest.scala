package com.flipkart.uie.time.persistence

import scala.reflect.internal.util.StringOps

/**
 * Created by rakesh.h on 02/08/15.
 */
object StringReaderTest extends App{
  val input =
    """   { "2014": {
      |              "1" : {
      |                     "0" : "23.34",
      |                     "4" : "10.15"
      |                    },
      |               "4" : {
      |                     "1" : "12.22"
      |                   }
      |               },
      |
      |       "2015" : {
      |                  "3" : "30.44"
      |                  }
      |
      |   }
      |
      |   """.stripMargin
  import Reader._
  import Writer._
  val life = input.read[Double](_.toDouble)
  println(life)

  println(life.get.write(_.toString))

}
