package com.angelovski

import org.apache.hadoop.conf.Configuration
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, SparkSession}

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
        val Right(response) = client.everything(phrase, from = Some(date), to = Some(date), sortBy = Some("publishedAt"), pageSize = Some(10), page = Some(1))
        println(s"Found ${response.totalResults} headlines.")
        response.articles.foreach(a => println(s"${a.publishedAt} - ${a.source.name} - ${a.title} - ${a.content}"))

        import spark.implicits._
        val df: DataFrame = response.articles
          .toDF()

        //        {Author} {Title} {Date}
        val res = df
          .withColumn("date", col("publishedAt").substr(0, 10))
          .withColumn("custom_field", concat(lit("{"), col("author"), lit("} "),
            lit("{"), col("title"), lit("} "),
            lit("{"), col("date"), lit("}")
          ))
          .withColumn("year", col("publishedAt").substr(0, 4))
          .withColumn("month", col("publishedAt").substr(6, 2))
        res.show()

        res.select("custom_field").show(20, truncate = false)

        //        store in HDFS

        val conf = new Configuration()
        conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem")
        conf.set("fs.hdfs.impl", classOf[Nothing].getName)
        conf.set("fs.defaultFS", "hdfs://127.0.0.1:9000")

        //        partitioning:
        val df_final = res.repartition(col("year"), col("month")).sort("publishedAt")

        df_final.write
          .partitionBy("year", "month")
          .mode("Overwrite")
          .format("Parquet")
          .save("hdfs://127.0.0.1:9000/raw_data/" + tableName)

        //        Hive:
        import spark.sql

        sql("CREATE DATABASE IF NOT EXISTS " + databaseName)
        sql("USE " + databaseName)
        val createTableStatement = "CREATE EXTERNAL TABLE IF NOT EXISTS " + tableName + "\n        (\n          title string,\n          id string,\n          name string,\n          author string,\n          description string,\n          published_at timestamp,\n          url string,\n          urlToImage string,\n          content string,\n          date_col string,\n          custom_field string)\n\n        PARTITIONED BY (year string,month string)\n        STORED AS PARQUET\n        LOCATION \"hdfs://127.0.0.1:9000/raw_data/" + tableName + "\""
        sql(createTableStatement)
        sql("msck repair table " + tableName)

      case None =>
        throw new RuntimeException(s"Please provide a valid api key as $NewsApiKeyEnv")
    }


  }
}