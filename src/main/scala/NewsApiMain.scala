package com.angelovski

import org.apache.hadoop.conf.Configuration
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.hadoop.fs.FileSystem

object NewsApiMain {
  def main(args: Array[String]): Unit = {

    args.foreach(arg => println(arg))

    val phrase = args(0)
    val date = args(1)

    val NewsApiKeyEnv = "NEWS_API_KEY"

    val spark = SparkSession.builder().appName("NewsApi")
      .master("local[*]").getOrCreate()


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
          .withColumn("date",col("publishedAt").substr(0, 10))
          .withColumn("custom_field", concat(lit("{"),col("author"),lit("} "),
            lit("{"),col("title"),lit("} "),
            lit("{"),col("date"),lit("}")
          ))
        res.show()

        res.select("custom_field").show(20, truncate = false)

//        store in HDFS

        val conf = new Configuration()
        conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem")
        conf.set("fs.hdfs.impl", classOf[Nothing].getName)
        conf.set("fs.defaultFS", "hdfs://127.0.0.1:9000")

//        val dfs = FileSystem.get(conf)
        df.write.mode("Overwrite").format("Parquet").save("hdfs://127.0.0.1:9000/test.parquet")

      case None =>
        throw new RuntimeException(s"Please provide a valid api key as $NewsApiKeyEnv")
    }


  }
}