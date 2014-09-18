package cmd

import org.rogach.scallop._
import recommender.DistanceMetric

/**
 * Created by Ondra Fiedler on 27.8.14.
 */

/**
 * Parse arguments (from command line) that can be later accessed as values of the Conf instance
 * @param arguments Command line arguments
 */
class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {

  val datasetTypes = DataHolderFactoryFromConf.dataHolderFactories
  val algorithms = RecommenderFactoryFromConf.recommenderFactories
  val distanceMetrics = DistanceMetric.distanceMetrics

  banner( """
SparkRecommender
----------------
Recommendation system in Scala using the Apache Spark framework

Example:
recommender --data netflix --dir /mnt/share/netflix --method kNN -p numberOfNeighbors=5 distanceMetric=euclidean

Arguments:
          """)

  version("version 0.1")

  //Arguments
  val interface = opt[String](default = Some("localhost"), descr = "Interface for setting up API")
  val port = opt[Int](default = Some(8080), descr = "Port of interface for setting up API")

  val data = opt[String](required = true, validate = { str => datasetTypes.map(_.getName).contains(str)}, descr = {
    "Type of dataset. Possibilities: " + datasetTypes.map(_.getName).reduce(_ + ", " + _)
  })
  val dir = opt[String](required = true, descr = "Directory containing files of dataset")

  val products = opt[Int](default = Some(10), descr = "Maximal number of recommended products")

  val method = opt[String](required = true, validate = { str => algorithms.map(_.getName).contains(str)}, descr = {
    "Algorithm. Possibilities: " + algorithms.map(_.getName).reduce(_ + ", " + _)
  })
  val parameters = props[String]('p', descr = "Parameters for algorithm")

  val version = opt[Boolean]("version", noshort = true, descr = "Print version")
  val help = opt[Boolean]("help", noshort = true, descr = "Show this message")


  //Description of algorithms, datasets and distance metrics
  private val algorithmsStr = "\nAlgorithms:\n" +
    algorithms.map(factory => factory.getName + "\nDescription: " + factory.getDescription + "\n---------------------------\n").reduce(_ + _)

  private val datasetsStr = "\nTypes of datasets:\n" +
    datasetTypes.map(factory => factory.getName + "\nDescription: " + factory.getDescription + "\n---------------------------\n").reduce(_ + _)

  private val distanceMetricsStr = "\nDistance metrics:\n" +
    distanceMetrics.map(factory => factory.getName + "\nDescription: " + factory.getDescription + "\n---------------------------\n").reduce(_ + _)

  footer(algorithmsStr + datasetsStr + distanceMetricsStr)


  /**
   * Get string value of an algorithm parameter
   * @param key Name of the parameter
   * @return Value (Some(String)) or None if the parameter was not given
   */
  def getStringParameter(key: String): Option[String] = {
    parameters.get(key)
  }

  /**
   * Get integer value of an algorithm parameter
   * @param key Name of the parameter
   * @return Value (Some(Int)) or None if the parameter was not given
   */
  def getIntParameter(key: String): Option[Int] = {
    val strOpt = getStringParameter(key)
    strOpt match {
      case Some(s) => Some(s.toInt)
      case None => None
    }
  }

  /**
   * Get Double value of an algorithm parameter
   * @param key Name of the parameter
   * @return Value (Some(Double)) or None if the parameter was not given
   */
  def getDoubleParameter(key: String): Option[Double] = {
    val strOpt = getStringParameter(key)
    strOpt match {
      case Some(s) => Some(s.toDouble)
      case None => None
    }
  }

  /**
   * Get a DistanceMetric from algorithm parameters
   * @param key Name of the parameter
   * @return Value (Some(DistanceMetric)) or None if the parameter was not given
   */
  def getDistanceMetricFromConf(key: String): Option[DistanceMetric] = {
    val dist: Option[DistanceMetric] = parameters.get(key) match {
      case Some(str) => {
        DistanceMetric.distanceMetrics.foreach(dis => {
          if (dis.getName == str) return Some(dis)
        })
        return None
      }
      case None => None
    }
    dist
  }
}