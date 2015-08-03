package com.flipkart.uie.time.tree

import play.api.libs.functional.Monoid
import com.github.nscala_time.time.Imports._

/**
 * Created by rakesh.h on 02/08/15.
 */

case class TimedEvent[A](time: DateTime, value: A)
case class Life[A](years: List[Year[A]])


 sealed abstract class TimeTree[A](children : List[TimeTree[A]], value: Option[A] = None) {
  require {
    if (value != None)
      children.size == 0
    else true
  }
  def contains(time: DateTime): Boolean
}

case class Year[A](year: Int, months: List[Month[A]], value: Option[A] = None) extends
      TimeTree[A](months, value) {
  require(year >= 0)

   def contains(time: DateTime): Boolean = {
     return time.getYear == year
   }
}
case class Month[A](year: Int, month: Int, weeks: List[Week[A]], value: Option[A] = None) extends
    TimeTree[A](weeks, value) {
  require(month >=1 && month <= 12)

  def contains(time: DateTime): Boolean = {
    return (time.getYear == year && time.getMonthOfYear == month)
  }
}
case class Week[A](year: Int, month: Int, week: Int, value: A) extends
    TimeTree[A](Nil, Some(value)) {
  require(week >= 0 && week <= 5)

   def contains(time: DateTime): Boolean = {
     return (time.getYear == year && time.getMonthOfYear == month &&
            weekNo(time) == week)
   }

  def weekNo(date: DateTime): Int =  date.dayOfMonth.get() / 7

}

object TimeTree {

  def flattenLife[A: Monoid](life: Life[A], interval: Interval): A = {
    val monoid = implicitly[Monoid[A]]
    life match {
      case Life(Nil) => monoid.identity
      case Life(years) => years.foldRight(monoid.identity){
        case (year, a) => monoid.append(flatten(year, interval), a)
      }
    }

  }

  def overlaps(year: Int, interval: Interval): Boolean = {
    val start = DateTime.parse(year + "-01-01")
    val end = DateTime.parse(year + 1 + "-01-01")
    (start to end).overlap(interval) != null
  }

  def overlaps(year: Int, month: Int, interval: Interval): Boolean = {
    val start = DateTime.parse(year + "-" + month + "-01")
    val end = DateTime.parse(year + "-" + (month + 1) + "-01")
    (start to end).overlap(interval) != null
  }

  def overlaps(year: Int, month: Int, week: Int, interval: Interval): Boolean = {
    val (startDay, endDay) = week match {
      case 0 => (1, 6)
      case 1 => (7, 13)
      case 2 => (14, 20)
      case 3 => (21, 27)
      case 4 => (28, daysOfMonth(year, month))
    }
    val start = DateTime.parse(year + "-" + month + "-" + startDay)
    val end = DateTime.parse(year + "-" + month  + "-" + endDay)
    (start to end).overlap(interval) != null
  }

  def daysOfMonth(year: Int, month: Int): Int =  {
    val dateTime = new DateTime(year, month, 14, 12, 0, 0, 0);
    return dateTime.dayOfMonth().getMaximumValue();
  }

  def flatten[A: Monoid](year: Year[A], interval: Interval): A = {
    val monoid = implicitly[Monoid[A]]
    year match {
      case Year(year, months, x)
        if (overlaps(year, interval)) => {
        x match {
          case Some(a) => monoid.append(monoid.identity, a)
          case None => {
            months.foldRight(monoid.identity){
              case (month, a) => monoid.append(flatten(month, interval), a)
            }
          }
        }
      }
      case _ => monoid.identity
    }
  }

  def flatten[A: Monoid](month: Month[A], interval: Interval): A = {
    val monoid = implicitly[Monoid[A]]
    month match {
      case Month(year ,month, weeks, x )
        if (overlaps(year, month, interval)) => {
         x match {
           case Some(a) => monoid.append(monoid.identity, a)
           case None => {
             weeks.foldRight(monoid.identity){
               case (week, a) => monoid.append(flatten(week, interval), a)
             }
           }
         }
      }

      case _ => monoid.identity
    }
  }

  def flatten[A: Monoid](week: Week[A], interval: Interval): A = {
    val monoid = implicitly[Monoid[A]]
    week match {
      case  Week(year, month, week, a)
        if (overlaps(year, month, week, interval)) => monoid.append(a, monoid.identity)
      case _ => monoid.identity
    }
  }

  def insert[A,B](life: Life[A], event: TimedEvent[B])(zero: A)(f : (A,B) => A): Life[A] =  life match {
    case l @ Life(years) if (years.exists(_.contains(event.time))) => {
      val insertedYears = years.map{year =>
        if (year.contains(event.time))
          insertInYear(year, event)(zero)(f)
         else
          year
      }
      l.copy(years = insertedYears)
    }
    case l @ Life(years) => {
      val createdYear = createYear(TimedEvent(event.time, f(zero, event.value)))
      l.copy(years = createdYear :: years)
    }
  }

  private def insertInYear[A,B](year: Year[A], event: TimedEvent[B])(zero: A)(f: (A,B) => A): Year[A] = year match {
    case y @ Year(_, _, Some(a)) => y.copy(value = Some(f(a, event.value)))
    case y @ Year (_, months, _) if (months.exists(_.contains(event.time))) => {
        val insertedMonths = months.map {month =>
          if (month.contains(event.time))
            insertInMonth(month, event)(zero)(f)
          else
            month
        }
        y.copy(months = insertedMonths)
    }
    case y @ Year(_, months, _) => {
      val createdMonth = createMonth(TimedEvent(event.time, f(zero, event.value)))
      y.copy(months = createdMonth :: months)
    }

  }

  private def insertInMonth[A,B](month: Month[A], event: TimedEvent[B])(zero: A)(f: (A,B) => A): Month[A] = month match {
    case m @ Month(_, _, _, Some(a)) => m.copy(value = Some(f(a, event.value)))
    case m @ Month (_, _, weeks, _) if weeks.exists(_.contains(event.time)) => {

      val insertedWeeks = weeks.map{week =>
        if (week.contains(event.time)) {
          insertInWeek(week, event)(zero)(f)
        }
        else
          week
      }
      m.copy(weeks = insertedWeeks)
    }
    case m @ Month(_, _, weeks, _) =>{
      val createdWeek = createWeek(TimedEvent(event.time, f(zero, event.value)))
      m.copy(weeks = createdWeek :: weeks)
    }

  }

  private def insertInWeek[A,B](week: Week[A], event: TimedEvent[B])(zero: A)(f: (A,B) => A): Week[A] = week match {
    case w @ Week (_, _, _, a) => w.copy(value = f (a, event.value))
  }

  private def createYear[A](timedEvent: TimedEvent[A]): Year[A] = {
    Year(timedEvent.time.getYear, List(createMonth(timedEvent)))
  }

  private def createMonth[A](timedEvent: TimedEvent[A]): Month[A] = {
    val time = timedEvent.time
    Month(time.getYear, time.getMonthOfYear, List(createWeek(timedEvent)))
  }

  private def createWeek[A](timedEvent: TimedEvent[A]): Week[A] = {
    val time = timedEvent.time
    val week = weekNo(time)
    Week(time.getYear, time.getMonthOfYear, week, timedEvent.value)
  }

  def weekNo(date: DateTime): Int =  date.dayOfMonth.get() / 7

}