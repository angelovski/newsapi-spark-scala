name := "newsapi"
idePackagePrefix := Some("com.angelovski")
version := "0.1.2-SNAPSHOT"
organization := "com.angelovski"
scalaVersion := "2.12.10"
sbtVersion := "1.0.0-M4"
val sparkVersion = "3.0.0"
val hadoopVersion = "3.2.1"
val jacksonVersion = "2.10.0"
val scalaTestVersion = "3.2.9"

libraryDependencies ++= Seq(
  "org.scalaj" %% "scalaj-http" % "2.3.0" excludeAll ExclusionRule("com.fasterxml.jackson.core", "jackson-databind"),
  "org.json4s" %% "json4s-jackson" % "3.5.3" excludeAll ExclusionRule("com.fasterxml.jackson.core", "jackson-databind"),
  "org.apache.spark" %% "spark-core" % sparkVersion excludeAll ExclusionRule("com.fasterxml.jackson.core", "jackson-databind"),
  "org.apache.spark" %% "spark-sql" % sparkVersion excludeAll ExclusionRule("org.apache.hadoop"),
  "org.apache.spark" %% "spark-hive" % sparkVersion excludeAll ExclusionRule("org.apache.hadoop"),
  "org.apache.hadoop" % "hadoop-client" % hadoopVersion excludeAll ExclusionRule("com.fasterxml.jackson.core", "jackson-databind"),
  "org.apache.hadoop" % "hadoop-hdfs" % hadoopVersion excludeAll ExclusionRule("com.fasterxml.jackson.core", "jackson-databind"),
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test
)

lazy val root = (project in file(".")).
  settings(
    name := "newsapi",
    version := "1.0",
    scalaVersion := "2.11.12",
    mainClass in Compile := Some("com.angelovski.NewsApiMain"),
    libraryDependencies ++= Seq(
      "org.scalaj" %% "scalaj-http" % "2.3.0" excludeAll ExclusionRule("com.fasterxml.jackson.core", "jackson-databind"),
      "org.json4s" %% "json4s-jackson" % "3.5.3" excludeAll ExclusionRule("com.fasterxml.jackson.core", "jackson-databind"),
      "org.apache.spark" %% "spark-core" % sparkVersion excludeAll ExclusionRule("com.fasterxml.jackson.core", "jackson-databind"),
      "org.apache.spark" %% "spark-sql" % sparkVersion excludeAll ExclusionRule("org.apache.hadoop"),
      "org.apache.spark" %% "spark-hive" % sparkVersion excludeAll ExclusionRule("org.apache.hadoop"),
      "org.apache.hadoop" % "hadoop-client" % hadoopVersion excludeAll ExclusionRule("com.fasterxml.jackson.core", "jackson-databind"),
      "org.apache.hadoop" % "hadoop-hdfs" % hadoopVersion excludeAll ExclusionRule("com.fasterxml.jackson.core", "jackson-databind"),
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test
    )
  )

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "services", _*) => MergeStrategy.concat
  case PathList("META-INF", _*) => MergeStrategy.discard
  case _ => MergeStrategy.first

}