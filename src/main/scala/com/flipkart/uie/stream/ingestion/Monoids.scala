package com.flipkart.uie.stream.ingestion

import play.api.libs.functional.Monoid

/**
 * Created by rakesh.h on 02/08/15.
 */
object Monoids {
  implicit val maxMonoid = new Monoid[Double] {
     def append(a1: Double, a2: Double): Double = a1 max a2

     def identity: Double = 0.00
  }

  implicit val sumMonoid = new Monoid[Double] {
     def append(a1: Double, a2: Double): Double = a1 + a2

     def identity: Double = 0.00
  }

  implicit val avgOrder = new Monoid[(Double, Int)] {
     def append(a1: (Double, Int), a2: (Double, Int)): (Double, Int) =
      (a1._1 + a2._1, a1._2 + a2._2)
     def identity: (Double, Int) = (0.00, 0)
  }


}
