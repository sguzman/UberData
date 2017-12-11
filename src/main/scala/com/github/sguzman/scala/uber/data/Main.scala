package com.github.sguzman.scala.uber.data

import com.github.sguzman.scala.uber.data.typesafe.data.all_data.AllDataStatement
import com.github.sguzman.scala.uber.data.typesafe.data.statement.Statement
import com.github.sguzman.scala.uber.data.typesafe.verify.PlatformChromeNavData
import io.circe.generic.auto._
import io.circe.parser.decode
import org.feijoas.mango.common.base.Preconditions

import scala.util.{Failure, Success}
import scalaj.http.Http

object Main {
  def main(args: Array[String]): Unit = {
    util.Try({
      //val response = Login.apply

      val cookies = System.getenv("COOKIES")
      Preconditions.checkNotNull(cookies)

      assertCookie(cookies)
      val allData = getAllData(cookies)
      val statementPreviews = allData.map(_.uuid).par.map(u => {
        val url = s"https://partners.uber.com/p3/money/statements/view/$u"
        println(url)
        val request = Http(url).header("Cookie", cookies)
        val response = request.asString
        val body = decode[Statement](response.body)
        println(url, body)
        Preconditions.checkArgument(body.isRight)
        body.right.get
      })

      statementPreviews foreach println

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
}
