package com.angelovski

import NewsApi.{Article, ArticleClean}

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
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
          .withColumnRenamed("date", "date_col")

        //        combining all articles by date
        val dateWindow = Window.partitionBy("date_col").orderBy("publishedAt")
        val dfByDate = df.withColumn("articles_by_date", concat_ws("\n~\n", collect_list("article_clean").over(dateWindow)))

        //        combining all articles by source_id
        val sourceWindow = Window.partitionBy("source_id").orderBy("publishedAt")
        val dfFinal = dfByDate.withColumn("articles_by_source", concat_ws("\n===\n", collect_list("article_clean").over(sourceWindow)))

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
          .mode("Overwrite")
          .format("Parquet")
          .save(s"hdfs://127.0.0.1:9000/raw_data/$tableName/$date")

        val dfRawData = spark.read.option("recursiveFileLookup", "true").parquet(s"hdfs://127.0.0.1:9000/raw_data/$tableName/")

        //        cleaning data from duplicates if job is run more than once
        val fs = FileSystem.get(conf)
        val outPutPath = new Path("/clean_data")
        if (fs.exists(outPutPath))
          fs.delete(outPutPath, true)

        dfRawData
          .sort("publishedAt")
          .repartition(60)
          .coalesce(60)
          .write
          .partitionBy("year", "month")
          .mode("Append")
          .format("Parquet")
          .save(s"hdfs://127.0.0.1:9000/clean_data/$tableName")

        //        Hive:
        import spark.sql

        sql(s"CREATE DATABASE IF NOT EXISTS $databaseName")
        sql(s"USE $databaseName")

        val createTableStatement =
          s"""CREATE EXTERNAL TABLE IF NOT EXISTS $tableName
             |(
             |title string,
             |id string,
             |name string,
             |author string,
             |description string,
             |publishedAt timestamp,
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
             |LOCATION "hdfs://127.0.0.1:9000/clean_data/$tableName"""".stripMargin

        sql(createTableStatement)
        sql(s"msck repair table $tableName")

        //        Analytics:

        val numArticlesPerDayDf = sql(
          s"""SELECT b.date_col,b.source_id,b.total_daily_articles
             |FROM (
             |         SELECT a.date_col,
             |                a.source_id,
             |                a.num_articles,
             |                a.rnk,
             |                SUM(num_articles) OVER (PARTITION BY date_col) AS total_daily_articles
             |         FROM (
             |                  SELECT date_col,
             |                         source_id,
             |                         COUNT(*) AS num_articles,
             |                         RANK() OVER (PARTITION BY date_col ORDER BY COUNT(*) DESC) AS rnk
             |                  FROM $tableName
             |                  GROUP BY date_col, source_id) a
             |     ) b
             |WHERE b.rnk=1""".stripMargin)


        val topTimeframe = sql(
          s"""SELECT DISTINCT a.date_col,
             |                a.timeframe_from,
             |                a.timeframe_to
             |FROM (
             |         SELECT date_col,
             |                timeframe_from,
             |                timeframe_to,
             |                RANK() OVER (
             |                    PARTITION BY date_col ORDER BY count DESC,timeframe_from ASC) AS rnk
             |         FROM (
             |                  SELECT date_col,
             |                         publishedat                                       AS timeframe_from,
             |                         from_unixtime(unix_timestamp(publishedat) + 3600) AS timeframe_to,
             |                         COUNT(*) OVER (
             |                             PARTITION BY date_col
             |                             ORDER BY publishedat ASC RANGE BETWEEN INTERVAL 1 HOUR PRECEDING AND CURRENT ROW
             |               )     AS count
             |                  FROM $tableName) a
             |     ) a
             |WHERE a.rnk = 1""".stripMargin)

        val analyticsDf = numArticlesPerDayDf.join(topTimeframe, "date_col")

        val responseJSON = analyticsDf.toJSON
        responseJSON.show(truncate = false)


      case None =>
        throw new RuntimeException(s"Please provide a valid api key as $NewsApiKeyEnv")
    }


  }
}