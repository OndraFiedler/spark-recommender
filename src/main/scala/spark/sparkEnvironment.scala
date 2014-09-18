package spark

import org.apache.log4j.{Level, Logger}
import org.apache.spark.{SparkConf, SparkContext}

/**
 * Created by Ondra Fiedler on 21.7.14.
 */
object sparkEnvironment {

  Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
  Logger.getLogger("org.eclipse.jetty.server").setLevel(Level.OFF)

  val conf = new SparkConf().setAppName("SparkRecommender")
  val sc = new SparkContext(conf)
}