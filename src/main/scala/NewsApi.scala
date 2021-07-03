package com.angelovski

import org.joda.time.DateTime
import org.json4s.CustomSerializer
import org.json4s.JsonAST.JString

import java.sql.Timestamp
import java.time.Instant
import java.util.Date

object NewsApi {
  case class Article(
                      title: String,
                      source: Source,
                      author: Option[String],
                      description: String,
                      publishedAt: Timestamp,
                      url: String,
                      urlToImage: String,
                      content: String)

  case class Source(id: Option[String], name: String)


  object InstantSerializer extends CustomSerializer[Instant](_ => (
    { case JString(s) => Instant.parse(s) },
    { case t: Instant => JString(t.toString) }
  ))

  object DateSerializer extends CustomSerializer[DateTime](_ => (
    { case JString(s) => DateTime.parse(s) },
    { case t: DateTime => JString(t.toString) }
  ))

  object TimeStampSerializer extends CustomSerializer[Timestamp](_ => (
    { case JString(s) => Timestamp.valueOf(s.replace('T', ' ').substring(0, 19)) },
    { case t: Timestamp => JString(t.toString) }
  ))

}
