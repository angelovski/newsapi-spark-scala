# Demo Spark/Scala app

A Spark/Scala demo application that uses data from [News API](https://newsapi.org).

## Setup

Download docker container from [here](https://hub.docker.com/r/dehtec/hadoop)

Clone this repository and cd into the root dir of the project.

Build a fat JAR from the project:
```shell
sbt clean
sbt assembly
```

Run the following commands:

```shell
docker build -t dehtec/hadoop:1.0.0 .
docker run -a stdin -a stdout -i -t -m=4g dehtec/hadoop:1.0.0 /bin/bash
```

This will run a shell inside the container. Next we run a bootstrap.sh script:

```shell
./bootstrap.sh
```

## Usage

API key must be obtained from newsapi.org before running the application.
The key should be exported as ENV variable:
```shell
export NEWS_API_KEY=<key>
```

The application accepts 4 parameters:
- phrase (query the API for this specific keyword)
- date (query the API for this specific date)
- databaseName (name of the Hive database to use)
- tableName (name of the Hive table to use)

Then we are ready to run our application, replacing the 4 parameters with other:
```shell
spark-submit --class com.angelovski.NewsApiMain --master local --driver-memory 1g --executor-memory 1g newsapi.jar tesla 2021-06-28 test_db test_table
```

The application outputs either FAILURE or SUCCESS. On success, it additionally outputs a JSON of required statistics, e.g.:
```shell
+-------------------------------------------------------------------------------------------------------------------+
|value                                                                                                              |
+-------------------------------------------------------------------------------------------------------------------+
|{"date_col":"2021-06-28","source":"N/A","articles":100,"timeframe":"2021-06-28T19:19:16Z-2021-06-28T20:19:16Z"}    |
|{"date_col":"2021-06-30","source":"N/A","articles":99,"timeframe":"2021-06-30T17:07:19Z-2021-06-30T18:07:19Z"}     |
|{"date_col":"2021-07-01","source":"N/A","articles":100,"timeframe":"2021-07-01T18:07:18Z-2021-07-01T19:07:18Z"}    |
|{"date_col":"2021-07-02","source":"N/A","articles":99,"timeframe":"2021-07-02T18:52:00Z-2021-07-02T19:52:00Z"}     |
|{"date_col":"2021-07-03","source":"N/A","articles":2,"timeframe":"2021-07-03T00:00:00Z-2021-07-03T01:00:00Z"}      |
|{"date_col":"2021-07-03","source":"bloomberg","articles":2,"timeframe":"2021-07-03T00:00:00Z-2021-07-03T01:00:00Z"}|
+-------------------------------------------------------------------------------------------------------------------+

SUCCESS
```