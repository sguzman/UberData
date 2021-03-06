package com.github.sguzman.scala.uber.data

import java.io.{File, PrintWriter}
import java.net.SocketTimeoutException

import com.github.sguzman.scala.uber.data.typesafe.data.all_data.AllDataStatement
import com.github.sguzman.scala.uber.data.typesafe.data.statement.Statement
import com.github.sguzman.scala.uber.data.typesafe.data.trip.Trip
import com.github.sguzman.scala.uber.data.typesafe.verify.PlatformChromeNavData
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import org.feijoas.mango.common.base.Preconditions

import scala.util.{Failure, Success}
import scalaj.http.Http

object Main {
  def main(args: Array[String]): Unit = {
    util.Try({
      val response = Login.apply

      val cookies = response.cookies.mkString("; ")
      Preconditions.checkNotNull(cookies)

      assertCookie(cookies)
      val statement = getStatement(cookies, _: String)
      val trip = getTrip(cookies, _: String)

      val allData = getAllData(cookies)
      val statementPreviews = allData
        .par
        .map(_.uuid)
        .map(_.toString)
        .map(statement)
        .flatMap(_.body.driver.trip_earnings.trips.keySet.toList)
        .map(_.toString)
        .map(trip)
        .toArray

      val map = Map("items" -> statementPreviews)
      val mapStr = map.asJson.toString

      println(mapStr)
      val pw = new PrintWriter(new File("./cache.json"))
      pw.write(mapStr)
      pw.close()

    }) match {
      case Success(_) => println("Done")
      case Failure(e) => Console.err.println(e)
    }
  }

  def assertCookie(cookies: String): Unit = {
    val url = "https://partners.uber.com/p3/platform_chrome_nav_data"
    val checkRequest = Http(url).header("Cookie", cookies)
    val checkResponse = checkRequest.asString

    val checkBody = checkResponse.body
    val checkObj = decode[PlatformChromeNavData](checkBody)
    Preconditions.checkArgument(checkObj.isRight)
  }

  def getAllData(cookies: String): Array[AllDataStatement] = {
    val allDataUrl = "https://partners.uber.com/p3/money/statements/all_data/"
    val allDataRequest = Http(allDataUrl).header("Cookie", cookies)
    val allDataResponse = allDataRequest.asString

    val allDataBody = allDataResponse.body
    val allDataObj = decode[Array[AllDataStatement]](allDataBody)
    Preconditions.checkArgument(allDataObj.isRight)

    allDataObj.right.get
  }

  def getStatement(cookies: String, uuid: String): Statement = util.Try({
    val url = s"https://partners.uber.com/p3/money/statements/view/$uuid"
    val request = Http(url).header("Cookie", cookies)
    val response = request.asString

    if (response.code == 429) {
      getStatement(cookies, uuid)
    }
    else {
      println(s"Success $url")
      val body = decode[Statement](response.body)
      Preconditions.checkArgument(body.isRight)
      body.right.get
    }
  }) match {
    case Success(v) => v
    case Failure(e) => e match {
      case _: SocketTimeoutException => getStatement(cookies, uuid)
    }
  }

  def getTrip(cookies: String, uuid: String): Trip = util.Try({
    val url = s"https://partners.uber.com/p3/money/trips/trip_data/$uuid"
    val request = Http(url).header("Cookie", cookies)
    val response = request.asString

    if (response.code == 429) {
      getTrip(cookies, uuid)
    } else {
      println(s"Success $url")
      val body = decode[Trip](response.body)
      Preconditions.checkArgument(body.isRight)
      body.right.get
    }
  }) match {
    case Success(v) => v
    case Failure(e) => e match {
      case _: SocketTimeoutException => getTrip(cookies, uuid)
    }
  }
}
