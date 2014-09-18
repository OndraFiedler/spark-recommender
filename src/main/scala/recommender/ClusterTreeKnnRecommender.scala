package recommender

import org.apache.spark.mllib.recommendation.Rating
import org.apache.spark.rdd.RDD

/**
 * Created by Ondra Fiedler on 3.9.14.
 */

/**
 * k-NN with a tree of clusters. The tree is created by repetitive splitting the biggest cluster into two smaller by k-Means from MLlib.
 * @param vectorsRDD Vectors representing a set of users. Ratings of users are taken from the Recommender's dataHolder if this field is not specified.
 * @param numberOfClusters Number of clusters
 * @param numberOfNeighbors Number of considered neighbors by the k-NN algorithm
 * @param distanceMetric Metric which determines similarity between users in k-NN
 */
class ClusterTreeKnnRecommender(vectorsRDD: RDD[UserVector], numberOfClusters: Int, numberOfKMeansIterations: Int, numberOfNeighbors: Int, distanceMetric: DistanceMetric = CosineDistance) extends RecommenderWithUserVectorRepresentation(vectorsRDD) with Serializable {

  def this(numberOfClusters: Int, numberOfKMeansIterations: Int, numberOfNeighbors: Int, distanceMetric: DistanceMetric = CosineDistance) = this(UserSparseVector.convertRatingsFromHolderToUserSparseVectors(MainHolder.getDataHolder()), numberOfClusters, numberOfKMeansIterations, numberOfNeighbors, distanceMetric)

  val root = createTree()

  class TreeNode(centroidVal: Option[UserVector], clusterVal: Option[RDD[UserVector]]) {
    val centroid = centroidVal
    var leftChild: Option[TreeNode] = None
    var rightChild: Option[TreeNode] = None
    var cluster: Option[RDD[UserVector]] = clusterVal
    var recommender: Option[KnnRecommender] = None
    val sizeInSubTree: Long = clusterVal match {
      case Some(cluster) => cluster.count
      case None => 0
    }
  }

  /**
   * Splits cluster in a leaf node into two smaller by MLlib's k-Means and puts those new clusters to children of this node
   * @param node A leaf node
   * @return Returns false if the cluster could not be split because it contained only one vector, returns true otherwise
   */
  protected def splitNode(node: TreeNode): Boolean = {
    node.cluster match {
      case Some(rdd) => {
        if (rdd.count() <= 1) return false
        val (centroids, seqOfRDDs) = KMeansClustering.clustering(rdd, 2, numberOfKMeansIterations)
        node.leftChild = Some(new TreeNode(Some(centroids(0)), Some(seqOfRDDs(0))))
        node.rightChild = Some(new TreeNode(Some(centroids(1)), Some(seqOfRDDs(1))))
        node.cluster = None
        return true
      }
      case None => throw new CannotSplitInnerNodeException
    }
  }

  /**
   * Creates a binary tree, which contains clusters in its leaf nodes. A KnnRecommender is created for every cluster.
   * @return Root of the tree
   */
  protected def createTree(): TreeNode = {
    val root = new TreeNode(None, Some(vectorsRDD))

    var leafs = List(root)
    var numberOfCreatedClusters = 1

    while (numberOfCreatedClusters < numberOfClusters) {
      val node = leafs.head

      val splitted = splitNode(node)
      if (splitted) {
        leafs = leafs.drop(1)
        leafs = leafs ::: List(node.leftChild.get) ::: List(node.rightChild.get)
        leafs = leafs.sortBy(node => -node.sizeInSubTree)
      }
      numberOfCreatedClusters += 1
    }

    leafs.foreach(node => node.recommender = Some(new KnnRecommender(node.cluster.get, numberOfNeighbors, distanceMetric)))

    root
  }

  /**
   * Finds the nearest cluster to vector and returns a KnnRecommender that corresponds to this cluster
   * @param vector Vector with ratings of target user
   * @return KnnRecommender
   */
  protected def getRecommender(vector: UserVector): Option[KnnRecommender] = {
    var node = root
    var notFound = true
    var recommender: Option[KnnRecommender] = None
    while (notFound) {
      node.recommender match {
        case Some(recommenderInNode) => {
          recommender = Some(recommenderInNode)
          notFound = false
        }
        case None => {
          val leftCentroid = node.leftChild.get.centroid.get
          val rightCentroid = node.rightChild.get.centroid.get

          if (distanceMetric.getDistance(vector, leftCentroid) < distanceMetric.getDistance(vector, rightCentroid)) {
            node = node.leftChild.get
          }
          else {
            node = node.rightChild.get
          }
        }
      }
    }
    recommender
  }

  class CannotSplitInnerNodeException extends Exception

  class WrongFormatOfClusterTreeException extends Exception

  /**
   * Recommend
   * @param vector Vector with ratings of target user
   * @return Ids of recommended products
   */
  override def recommend(vector: UserVector, numberOfRecommendedProducts: Int): Seq[Rating] = {
    getRecommender(vector) match {
      case Some(recommender) => recommender.recommend(vector, numberOfRecommendedProducts)
      case None => throw new WrongFormatOfClusterTreeException
    }
  }
}