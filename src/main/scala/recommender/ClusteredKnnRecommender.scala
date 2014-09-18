package recommender

import org.apache.spark.mllib.clustering.KMeans
import org.apache.spark.rdd.RDD

/**
 * Created by Ondra Fiedler on 30.7.14.
 */

/**
 * Basis for algorithms that speed up k-NN algorithm by clustering
 * @param vectorsRDD Vectors representing a set of users. Ratings of users are taken from the Recommender's dataHolder if this field is not specified.
 * @param numberOfClusters Number of clusters
 * @param numberOfNeighbors Number of considered neighbors by the k-NN algorithm
 * @param distanceMetric Metric which determines similarity between users in k-NN
 */
abstract class ClusteredKnnRecommender(vectorsRDD: RDD[UserVector], numberOfClusters: Int, numberOfNeighbors: Int, distanceMetric: DistanceMetric = CosineDistance) extends RecommenderWithUserVectorRepresentation(vectorsRDD) with Serializable {

  def this(numberOfClusters: Int, numberOfNeighbors: Int, distanceMetric: DistanceMetric = CosineDistance) = this(UserSparseVector.convertRatingsFromHolderToUserSparseVectors(MainHolder.getDataHolder()), numberOfClusters, numberOfNeighbors, distanceMetric)

  //Get centroids and clusters
  val (centroids, arrayOfClustersRDDs) = clustering()

  //One k-nn recommender per cluster
  val KnnRecommenders = arrayOfClustersRDDs.map(v => new KnnRecommender(v, numberOfNeighbors, distanceMetric))

  //"Recommender" for choosing nearest centroids
  val centroidsRDD = spark.sparkEnvironment.sc.parallelize(centroids).persist()
  val nearestCentroidsRecommender = new KnnRecommender(centroidsRDD, numberOfClusters, distanceMetric)

  /**
   * Groups vectors into clusters
   * @return Sequence of RDDs. Each RDD contains UserVectors corresponding to one cluster
   **/
  protected def clustering(): (Array[UserVector], Seq[RDD[UserVector]])

  override def recommend(userVector: UserVector, numberOfRecommendedProducts: Int) = {

    val nearestCentroids = nearestCentroidsRecommender.getNearestNeighbors(userVector)
    val nearestCentroidsIDs = nearestCentroids.map(v => v.getUserID())

    //TODO iterate more nearest clusters
    val nearestRecommender = KnnRecommenders(nearestCentroidsIDs(0))
    nearestRecommender.recommend(userVector, numberOfRecommendedProducts)
  }
}

/**
 * ClusteredKnnRecommender using k-Means algorithm from MLlib for clustering
 * @param vectorsRDD Vectors representing a set of users. Ratings of users are taken from the Recommender's dataHolder if this field is not specified.
 * @param numberOfClusters Number of clusters
 * @param numberOfKMeansIterations Number of k-Means algorithm
 * @param numberOfNeighbors Number of considered neighbors by the k-nn algorithm
 * @param distanceMetric Metric which determines similarity between users in k-NN
 */
class KMeansClusteredKnnRecommender(vectorsRDD: RDD[UserVector], numberOfClusters: Int, numberOfKMeansIterations: Int, numberOfNeighbors: Int, distanceMetric: DistanceMetric = CosineDistance) extends ClusteredKnnRecommender(vectorsRDD, numberOfClusters, numberOfNeighbors, distanceMetric) with Serializable {

  def this(numberOfClusters: Int, numberOfKMeansIterations: Int, numberOfNeighbors: Int, distanceMetric: DistanceMetric = CosineDistance) = this(UserSparseVector.convertRatingsFromHolderToUserSparseVectors(MainHolder.getDataHolder()), numberOfClusters, numberOfKMeansIterations, numberOfNeighbors, distanceMetric)

  override def clustering(): (Array[UserVector], Seq[RDD[UserVector]]) = {
    KMeansClustering.clustering(vectorsRDD, numberOfClusters, numberOfKMeansIterations)
  }
}

/**
 * Clustering by MLlib's k-Means
 */
object KMeansClustering {
  def clustering(vectorsRDD: RDD[UserVector], numberOfClusters: Int, numberOfKMeansIterations: Int): (Array[UserVector], Seq[RDD[UserVector]]) = {
    val mllibVectorsRDD = vectorsRDD.map(v => v.toMLlibVector())

    val kmeans = new KMeans().setK(numberOfClusters).setMaxIterations(numberOfKMeansIterations)
    val model = kmeans.run(mllibVectorsRDD)

    val predictedClusters = model.predict(mllibVectorsRDD)
    val vectorsWithClusterNumbersRDD = predictedClusters.zip(vectorsRDD)

    val centroidsMLlibVectors = model.clusterCenters
    val centroidsUserVector: Array[UserVector] = centroidsMLlibVectors.zipWithIndex.map { case (v, i) => new UserDenseVector(i, v.toArray)}


    val arrayOfClustersRDDsBeforeRepartition = (0 until numberOfClusters).map(i => vectorsWithClusterNumbersRDD.filter { case (j, cluster) => (i == j)}.map { case (j, vec) => vec})

    val numberOfVectorsInOriginalRDD = vectorsRDD.count()
    val numberOfOriginalPartitions = vectorsRDD.partitions.length

    val seqOfClustersRDDs = arrayOfClustersRDDsBeforeRepartition.map(rdd => {
      val ratioBetweenVectorsInThisClustersAndAllClusters = rdd.count().toDouble / numberOfVectorsInOriginalRDD
      val numberOfPartitions = (numberOfOriginalPartitions * ratioBetweenVectorsInThisClustersAndAllClusters).ceil.toInt
      rdd.repartition(numberOfPartitions).persist()
    })

    (centroidsUserVector, seqOfClustersRDDs)
  }
}