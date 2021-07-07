package com.angelovski

import NewsApi.{Article, ArticleClean}

import org.apache.hadoop.conf.Configuration
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, SparkSession}

import java.sql.Date
import scala.collection.mutable.ListBuffer

object NewsApiMain {
  def main(args: Array[String]): Unit = {

    args.foreach(arg => println(arg))

    val phrase = args(0)
    val date = args(1)
    val databaseName = args(2)
    val tableName = args(3)

    val NewsApiKeyEnv = "NEWS_API_KEY"

    val spark = SparkSession
      .builder()
      .appName("NewsApi")
      .master("local[*]")
      .enableHiveSupport()
      .getOrCreate()


    Option(System.getenv(NewsApiKeyEnv)) match {
      case Some(apiKey) =>
        val client = NewsApiRestClient(apiKey)
        val Right(response) = client.everything(phrase, from = Some(date), to = Some(date), sortBy = Some("publishedAt"), pageSize = Some(100), page = Some(1))
        println(s"Found ${response.totalResults} headlines.")

        //        Not implementing pagination because of API limitations for developer account. Otherwise:
        //        val pageCount = (response.totalResults.toFloat / response.articles.size).ceil.toInt
        //        val articles: Seq[Article] = response.articles
        //
        //        if (pageCount > 1) {
        //          for (i <- 2 to pageCount) {
        //            val Right(response) = client.everything(phrase, from = Some(date), to = Some(date), sortBy = Some("publishedAt"), pageSize = Some(10), page = Some(i))
        //            articles ++ response.articles
        //          }
        //        }

        import spark.implicits._
        val df: DataFrame = response.articles
          .toDF()

          //        {Author} {Title} {Date}
          //        val res = df
          .withColumn("date", col("publishedAt").substr(0, 10))
          .withColumn("custom_field", concat(lit("{"), col("author"), lit("} "),
            lit("{"), col("title"), lit("} "),
            lit("{"), col("date"), lit("}")
          ))
          .withColumn("year", col("publishedAt").substr(0, 4))
          .withColumn("month", col("publishedAt").substr(6, 2))
          .withColumn("article_clean", regexp_replace(col("content"), "[^a-zA-Z0-9 (),.?!-;:]", ""))
          .withColumn("source_id", col("source").getItem("id"))
          .withColumn("source_name", col("source").getItem("name"))

        // combining all articles by date
        val dateWindow = Window.partitionBy("date").orderBy("publishedAt")
        val dfByDate = df.withColumn("articles_by_date", concat_ws("\n~\n", collect_list("article_clean").over(dateWindow)))
        dfByDate.select("date", "articles_by_date").show(10, truncate = false)
        // combining all articles by source_id
        val sourceWindow = Window.partitionBy("source_id").orderBy("publishedAt")
        val dfFinal = dfByDate.withColumn("articles_by_source", concat_ws("\n===\n", collect_list("article_clean").over(sourceWindow)))
        dfFinal.select("source", "articles_by_source").show(10, truncate = false)

        //        store in HDFS

        val conf = new Configuration()
        conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem")
        conf.set("fs.hdfs.impl", classOf[Nothing].getName)
        conf.set("fs.defaultFS", "hdfs://127.0.0.1:9000")

        //        Raw data + partitioning:
        val dfFinalPartitioned = dfFinal
          .sort("publishedAt")
          .repartition(60)
          .coalesce(60)

        dfFinalPartitioned.write
          .partitionBy("year", "month")
          .mode("Overwrite")
          .format("Parquet")
          .save("hdfs://127.0.0.1:9000/articles_data/" + tableName)

        //        Hive:
        import spark.sql

        sql(s"CREATE DATABASE IF NOT EXISTS $databaseName")
        sql(s"USE $databaseName")

        val createTableStatement = s"""CREATE EXTERNAL TABLE IF NOT EXISTS $tableName
                                     |(
                                     |title string,
                                     |id string,
                                     |name string,
                                     |author string,
                                     |description string,
                                     |published_at timestamp,
                                     |url string,
                                     |urlToImage string,
                                     |content string,
                                     |date_col string,
                                     |custom_field string,
                                     |article_clean string,
                                     |source_id string,
                                     |source_name string,
                                     |articles_by_date string,
                                     |articles_by_source string
                                     |)
                                     |
                                     |PARTITIONED BY (year string,month string)
                                     |STORED AS PARQUET
                                     |LOCATION "hdfs://127.0.0.1:9000/articles_data/$tableName"""".stripMargin

        sql(createTableStatement)
        sql(s"msck repair table $tableName")

      case None =>
        throw new RuntimeException(s"Please provide a valid api key as $NewsApiKeyEnv")
    }


  }
}