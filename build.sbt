name := "Streaming-POC"

version := "1.0"

scalaVersion := "2.10.5"

libraryDependencies ++= Seq(
      "org.apache.kafka" % "kafka_2.10" % "0.8.2.1"
        exclude("javax.jms", "jms")
        exclude("com.sun.jdmk", "jmxtools")
        exclude("com.sun.jmx", "jmxri"),
        "org.apache.spark" %% "spark-streaming" % "1.3.1",
        "org.apache.spark" %% "spark-streaming-kafka" % "1.3.1",
        "org.apache.spark" %% "spark-core" % "1.3.1",
        "com.typesafe" % "config" % "1.2.1",
        "com.typesafe.play" % "play-json_2.10" % "2.4.0-M2",
        "org.scalatest" % "scalatest_2.10" % "2.2.1" % "test",
        "org.scalacheck" % "scalacheck_2.10" % "1.12.4"
     )
    