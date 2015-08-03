package com.flipkart.uie.time.persistence

import com.flipkart.uie.time.tree.{Week, Month, Year, Life}
import play.api.libs.json.{JsString, JsValue, JsObject, Json}

/**
 * Created by rakesh.h on 02/08/15.
 */
trait Reader[A, B] {

  def read(value: B) : Option[Life[A]]

}

object Reader {

  implicit class ReadString(input: String)  {
    def read[A](implicit f: String => A): Option[Life[A]] = stringReader[A](f).read(input)
  }

  implicit def stringReader[A](implicit f: String => A): Reader[A, String] = new Reader[A, String] {
    def read(value: String): Option[Life[A]] = {
      try {
        val parsed = Json.parse(value)
        parsed match {
          case JsObject(fields) => {
            val years = fields.map{
              case(year, value) => createYear(year, value)(f)
            }
            Some(Life(years.flatten.toList))
          }
          case _ => None
        }
      }
      catch {
        case e: Exception => None
      }
    }
  }


  private def createYear[A](year: String, jsVal: JsValue)(f: String => A): Option[Year[A]] = jsVal match {
    case JsString(value) => Some(Year(year.toInt, Nil, Some(f(value))))
    case JsObject(fields) => {
      val months = fields map {
        case (month, jsVal) =>  createMonth(year, month, jsVal)(f)
      }
      Some(Year(year.toInt, months.flatten.toList, None))
    }
    case _ => None
  }

  private def createMonth[A](year: String, month: String, jsVal: JsValue)(f: String => A): Option[Month[A]] = jsVal match{
    case JsString(value) =>Some(Month(year.toInt, month.toInt, Nil, Some(f(value))))
    case JsObject(fields) => {
      val weeks = fields map {
        case (week, value) => createWeek(year, month, week, value)(f)
      }
      Some(Month(year.toInt, month.toInt, weeks.flatten.toList, None))
    }
    case _ => None
  }

  private def createWeek[A](year: String, month: String, week: String, jsVal: JsValue)(f: String => A): Option[Week[A]] =
      jsVal match {
        case JsString(value) => Some(Week(year.toInt, month.toInt, week.toInt, f(value)))
        case _ => None
      }
}

trait Writer[A, B] {
  def write(life: Life[A]): B
}

object Writer {

  implicit class WriteLife[A](life: Life[A])  {
      def write(implicit f: A => String): String = life match {
        case Life(Nil) => "{}"
        case Life (years) => {
          val yearsString = years.map {year =>
            writeYear(year)(f)
          }
          yearsString.mkString("{", ",", "}")
        }
      }

  }

  def writeYear[A](year: Year[A])(implicit f: A => String): String = year match {
    case Year(year, _, Some(x)) => {
      val value = f(x)
      s""" "$year" : "$value"  """.trim
    }
    case Year(year, months, _) => {
      val monthsString = months map {month =>
          writeMonth(month)(f)
      }
      val monthStr = monthsString.mkString("{", ",", "}")
      s""" "$year" : $monthStr  """.trim
    }
  }

  def writeMonth[A](month: Month[A])(implicit  f: A => String) = month match {
    case Month(_, month, _, Some(x)) => {
      val value = f(x)
      s""" "$month" : "$value"  """.trim
    }

    case Month(_, month, weeks, _) => {
      val weeksString = weeks map { week =>
        writeWeek(week)
      }
      val weekStr = weeksString.mkString("{", ",", "}")
      s""" "$month" : $weekStr  """.trim
    }
  }

  def writeWeek[A](week: Week[A])(implicit f : A => String) = week match {
    case Week(_, _, week, a) => s""" "$week" : "${f(a)}"  """.trim
  }

}