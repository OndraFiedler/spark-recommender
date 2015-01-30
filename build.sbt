name := "SparkRecommender"

version := "0.1"

mainClass in Compile := Some("Boot")

//Scallop
libraryDependencies += "org.rogach" %% "scallop" % "0.9.5"

//Spark
libraryDependencies += "org.apache.spark" %% "spark-core" % "1.2.0" % "provided"

libraryDependencies += "org.apache.spark" % "spark-mllib_2.10" % "1.2.0" % "provided"

resolvers += "Akka Repository" at "http://repo.akka.io/releases/"

//Spray
scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers ++= Seq(
  "spray repo" at "http://repo.spray.io/"
)

libraryDependencies ++= {
  val sprayVersion = "1.2-RC4"
  val akkaVersion = "2.2.3"
  Seq(
    "io.spray" % "spray-can" % sprayVersion,
    "io.spray" % "spray-routing" % sprayVersion,
    "io.spray" % "spray-testkit" % sprayVersion,
    "io.spray" % "spray-client" % sprayVersion,
    "io.spray" %% "spray-json" % "1.2.5",
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "ch.qos.logback" % "logback-classic" % "1.0.12",
    "org.scalatest" %% "scalatest" % "2.0.M7" % "test"
  )
}
