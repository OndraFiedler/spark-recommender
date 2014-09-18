package cmd

import recommender._

/**
 * Created by Ondra Fiedler on 27.8.14.
 */

trait RecommenderFactoryFromConf extends NameAndDescription {
  /**
   * Returns an instance of Recommender
   * @param conf Instance of Conf with parsed command line arguments
   * @return Recommender instance
   */
  def getRecommender(conf: Conf): Recommender

  def getAlgorithmDescription(): String

  def getParametersDescription(): String

  def getDescription(): String = {
    getAlgorithmDescription() + "\nParameters:" + getParametersDescription()
  }
}

object RecommenderFactoryFromConf {
  /**
   * List of all objects that extend RecommenderFactoryFromConf. RecommenderFactoryFromConf object must be listed here in order to be used through command line interface.
   */
  val recommenderFactories: List[RecommenderFactoryFromConf] = List(KnnRecommenderFactory, KMeansClusteredKnnRecommenderFactory, ClusterTreeKnnRecommenderFactory, ALSRecommenderFactory)
}

//Factories:

trait KnnGeneralRecommenderFactory extends RecommenderFactoryFromConf {
  //Parameters
  protected val numberOfNeighborsStr = "numberOfNeighbors"
  protected val numberOfNeighborsDescriptionStr = numberOfNeighborsStr + " = <Int>\n"
  protected val distanceMetricStr = "distanceMetric"
  protected val distanceMetricStrDescriptionStr = distanceMetricStr + " = <String>, Distance metric between two users (vectors of ratings)\n"

  case class KnnParameters(numberOfNeighbors: Int, distanceMetric: DistanceMetric)

  def getKnnParametersFromConf(conf: Conf): KnnParameters = {

    val numberOfNeighbors = conf.getIntParameter(numberOfNeighborsStr).getOrElse(10)
    val distanceMetric = conf.getDistanceMetricFromConf(distanceMetricStr).getOrElse(CosineDistance)

    KnnParameters(numberOfNeighbors, distanceMetric)
  }
}

object KnnRecommenderFactory extends KnnGeneralRecommenderFactory {

  override def getName = "kNN"

  override def getAlgorithmDescription(): String = "k-Nearest Neighbors algorithm"

  override def getParametersDescription(): String = numberOfNeighborsDescriptionStr + distanceMetricStrDescriptionStr

  override def getRecommender(conf: Conf) = {
    val parameters = getKnnParametersFromConf(conf)
    new KnnRecommender(parameters.numberOfNeighbors, parameters.distanceMetric)
  }
}

trait ClusteredKnnGeneralRecommenderFactory extends KnnGeneralRecommenderFactory {
  protected val numberOfClustersStr = "numberOfClusters"
  protected val numberOfClustersDescriptionStr = numberOfClustersStr + " = <Int>\n"

  protected val numberOfClusteringIterationsStr = "numberOfClusteringIterations"
  protected val numberOfClusteringIterationsDescriptionStr = "Number of iterations performed by clustering algorithm"

  case class ClusteredKnnParameters(numberOfClusters: Int, numberOfClusteringIterations: Int, numberOfNeighbors: Int, distanceMetric: DistanceMetric)

  def getClusteredKnnParametersFromConf(conf: Conf): ClusteredKnnParameters = {
    val knnParam = getKnnParametersFromConf(conf)
    val numberOfClusters = conf.getIntParameter(numberOfClustersStr).getOrElse(10)
    val numberOfClusteringIterations = conf.getIntParameter(numberOfClusteringIterationsStr).getOrElse(10)
    ClusteredKnnParameters(numberOfClusters, numberOfClusteringIterations, knnParam.numberOfNeighbors, knnParam.distanceMetric)
  }
}

object KMeansClusteredKnnRecommenderFactory extends ClusteredKnnGeneralRecommenderFactory {
  override def getName: String = "kMeansClusteredKnn"

  override def getAlgorithmDescription(): String = "k-Nearest Neighbors algorithm with users clustered by K-Means"

  override def getParametersDescription(): String = numberOfNeighborsDescriptionStr + distanceMetricStrDescriptionStr + numberOfClustersDescriptionStr + numberOfClusteringIterationsStr

  override def getRecommender(conf: Conf): Recommender = {
    val parameters = getClusteredKnnParametersFromConf(conf)
    new KMeansClusteredKnnRecommender(parameters.numberOfClusters, parameters.numberOfClusteringIterations, parameters.numberOfNeighbors, parameters.distanceMetric)
  }
}


object ClusterTreeKnnRecommenderFactory extends ClusteredKnnGeneralRecommenderFactory {
  override def getName: String = "clusterTreeKnn"

  override def getAlgorithmDescription(): String = "Recommender using tree of clusters"

  override def getParametersDescription(): String = KMeansClusteredKnnRecommenderFactory.getParametersDescription()

  override def getRecommender(conf: Conf): Recommender = {
    val parameters = getClusteredKnnParametersFromConf(conf)
    new ClusterTreeKnnRecommender(parameters.numberOfClusters, parameters.numberOfClusteringIterations, parameters.numberOfNeighbors, parameters.distanceMetric)
  }
}

object ALSRecommenderFactory extends RecommenderFactoryFromConf {
  override def getName: String = "als"

  override def getAlgorithmDescription(): String = "ALS using MLlib"

  protected val rankStr = "rank"
  protected val lambdaStr = "lambda"
  protected val iterStr = "numberOfIterations"

  override def getParametersDescription(): String = rankStr + " = <Int>\n" + lambdaStr + " = <Double>\n" + iterStr + " = <Int>, number of iterations"

  override def getRecommender(conf: Conf): Recommender = {
    val rank = conf.getIntParameter(rankStr).getOrElse(12)
    val lambda = conf.getDoubleParameter(lambdaStr).getOrElse(0.01)
    val iter = conf.getIntParameter(iterStr).getOrElse(10)

    new ALSRecommender(rank, lambda, iter)
  }
}