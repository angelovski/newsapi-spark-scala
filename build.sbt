name := "newsapi"
idePackagePrefix := Some("com.angelovski")
version := "0.1.0-SNAPSHOT"
organization := "com.angelovski"
scalaVersion := "2.11.12"
sbtVersion := "1.0.0-M4"
val sparkVersion = "2.4.6"
val hadoopVersion = "3.2.1"


libraryDependencies ++= Seq(
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "org.json4s" %% "json4s-jackson" % "3.5.3",
  "org.apache.spark" %% "spark-core" % "2.4.6",
  "org.apache.spark" %% "spark-sql" % "2.4.6"
)
