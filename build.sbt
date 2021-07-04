name := "newsapi"
idePackagePrefix := Some("com.angelovski")
version := "0.1.1-SNAPSHOT"
organization := "com.angelovski"
scalaVersion := "2.11.12"
sbtVersion := "1.0.0-M4"
val sparkVersion = "2.4.6"
val hadoopVersion = "3.2.1"


libraryDependencies ++= Seq(
  "org.scalaj" %% "scalaj-http" % "2.3.0" excludeAll(
    ExclusionRule("com.fasterxml.jackson.core", "jackson-databind")
    ),
  "org.json4s" %% "json4s-jackson" % "3.5.3" excludeAll(
    ExclusionRule("com.fasterxml.jackson.core", "jackson-databind")
  ),
  "org.apache.spark" %% "spark-core" % "2.4.6" excludeAll(
    ExclusionRule("com.fasterxml.jackson.core", "jackson-databind")
    ),
  "org.apache.spark" %% "spark-sql" % "2.4.6" excludeAll(
    ExclusionRule("org.apache.hadoop")
    ),
  "org.apache.hadoop" % "hadoop-client" % "3.2.1" excludeAll(
    ExclusionRule("com.fasterxml.jackson.core", "jackson-databind")
    ),
  "org.apache.hadoop" % "hadoop-hdfs" % "3.2.1" excludeAll(
    ExclusionRule("com.fasterxml.jackson.core", "jackson-databind")
    ),
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.7.5"
)

lazy val root = (project in file(".")).
  settings(
    name := "newsapi",
    version := "1.0",
    scalaVersion := "2.11.12",
    mainClass in Compile := Some("com.angelovski.NewsApiMain"),
    libraryDependencies ++= Seq(
        "org.scalaj" %% "scalaj-http" % "2.3.0" excludeAll(
          ExclusionRule("com.fasterxml.jackson.core", "jackson-databind")
          ),
        "org.json4s" %% "json4s-jackson" % "3.5.3" excludeAll(
          ExclusionRule("com.fasterxml.jackson.core", "jackson-databind")
          ),
        "org.apache.spark" %% "spark-core" % "2.4.6" excludeAll(
          ExclusionRule("com.fasterxml.jackson.core", "jackson-databind")
          ),
        "org.apache.spark" %% "spark-sql" % "2.4.6" excludeAll(
          ExclusionRule("org.apache.hadoop")
          ),
        "org.apache.hadoop" % "hadoop-client" % "3.2.1" excludeAll(
          ExclusionRule("com.fasterxml.jackson.core", "jackson-databind")
          ),
        "org.apache.hadoop" % "hadoop-hdfs" % "3.2.1" excludeAll(
          ExclusionRule("com.fasterxml.jackson.core", "jackson-databind")
          ),
        "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.7.5"
      )
  )

//assemblyMergeStrategy in assembly := {
//  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
//  case x => MergeStrategy.first
//}
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "services", _*) => MergeStrategy.concat
  case PathList("META-INF", _*) => MergeStrategy.discard
  case _ => MergeStrategy.first

}