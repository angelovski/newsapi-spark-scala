package com.angelovski
import NewsApi.{Article, DateSerializer, InstantSerializer, TimeStampSerializer}
import NewsApiRestClient.Response

import org.json4s.Formats
import org.json4s.jackson.Serialization._
import scalaj.http.{Http, HttpRequest, HttpResponse}


class NewsApiRestClient private(apiKey: String, host: String = "newsapi.org", useHttps: Boolean = true) {
  import NewsApiRestClient.Params._

  private val protocol = if (useHttps) "https" else "http"
  private val Host = s"$protocol://$host/v2"
  private implicit val formats: Formats = org.json4s.DefaultFormats + InstantSerializer + DateSerializer + TimeStampSerializer

  def everything(
                  query: String,
                  from: Option[String],
                  to: Option[String],
                  sortBy: Option[String],
                  pageSize: Option[Int] = None,
                  page: Option[Int] = None): Response[ArticlesResponse] = {

    val request = Http(s"$Host/everything")
      .param(ApiKey, apiKey)
      .param(Query, query)
    val addQueryParams = Function.chain[HttpRequest](Seq(
      addOptionalQueryParameter(_, From, from map identity),
      addOptionalQueryParameter(_, To, to map identity),
      addOptionalQueryParameter(_, SortBy, sortBy map identity),
      addOptionalQueryParameter(_, PageSize, pageSize.map(_.toString())),
      addOptionalQueryParameter(_, Page, page.map(_.toString()))
    ))
    val response = addQueryParams(request).asString
    parseResponse[ArticlesResponse](response)
  }


  private def addOptionalQueryParameter(request: HttpRequest, key: String, value: Option[String]): HttpRequest = {
    value match {
      case Some(v) => request.param(key, v)
      case None => request
    }
  }

  private def parseResponse[T: Manifest](response: HttpResponse[String]): Either[String, T] = {
    if (!response.is2xx) {
      Left(s"Error: returned code is ${response.code}. body: ${response.body}")
    } else {
      val body = response.body
      Right(read[T](body))
    }
  }
}

case class ArticlesResponse(
                             articles: Seq[Article],
                             totalResults: Int,
                             status: String)

//case class SourcesResponse(
//                            sources: Seq[FullSource],
//                            status: String)

object NewsApiRestClient {
  object Params {
    val ApiKey = "apiKey"
    val Query = "q"
    val Country = "country"
    val Category = "category"
    val Sources = "sources"
    val Domains = "sources"
    val From = "from"
    val To = "to"
    val Language = "language"
    val SortBy = "sortBy"
    val PageSize = "pageSize"
    val Page = "page"
  }

  type Response[T] = Either[String, T]

  def apply(apiKey: String, host: String = "newsapi.org", useHttps: Boolean = true): NewsApiRestClient = {
    new NewsApiRestClient(apiKey, host, useHttps)
  }
}