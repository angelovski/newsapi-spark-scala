package com.angelovski

import NewsApiMain.buildHdfsPath
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class NewsApiSpec extends AnyFlatSpec with Matchers {

  it should "build the correct path to HDFS with date" in {
    val expectedPath = "hdfs://127.0.0.1:9000/raw_data/test_table/2021-02-01"
    val hdfsHost = "hdfs://127.0.0.1:9000"
    val date = "2021-02-01"
    val tableName = "test_table"
    val subDir = "raw_data"
    val path = buildHdfsPath(hdfsHost, subDir, tableName, Some(date))
    path shouldBe expectedPath
  }

  it should "build the correct path to HDFS without date" in {
    val expectedPath = "hdfs://127.0.0.1:9000/raw_data/test_table"
    val hdfsHost = "hdfs://127.0.0.1:9000"
    val date = None
    val tableName = "test_table"
    val subDir = "raw_data"
    val path = buildHdfsPath(hdfsHost, subDir, tableName, date)
    path shouldBe expectedPath
  }


}
