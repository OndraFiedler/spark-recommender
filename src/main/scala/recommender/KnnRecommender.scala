package recommender

import breeze.linalg.Vector
import org.apache.spark.SparkContext._
import org.apache.spark.mllib.recommendation.Rating
import org.apache.spark.rdd.RDD

/**
 * Created by Ondra Fiedler on 24.7.14.
 */


/**
 * Recommender which uses the k-nearest neighbors algorithm
 * @param vectorsRDD Vectors representing a set of users. Ratings of users are taken from the Recommender's dataHolder if this field is not specified.
 * @param numberOfNeighbors Number of neighbors
 * @param distanceMetric Metric which determines similarity between users
 * @param lazyStartup If false, then one recommendation is done at startup, to get all the lazy init actions done
 */
class KnnRecommender(vectorsRDD: RDD[UserVector], numberOfNeighbors: Int, distanceMetric: DistanceMetric = CosineDistance, lazyStartup: Boolean = false) extends RecommenderWithUserVectorRepresentation(vectorsRDD) with Serializable {
  def this(k: Int, distanceMetric: DistanceMetric = CosineDistance) = this(UserSparseVector.convertRatingsFromHolderToUserSparseVectors(MainHolder.getDataHolder()), k, distanceMetric, false)

  if (!lazyStartup) {
    //"Recommendation" for getting all the lazy init actions done
    recommend(new UserSparseVector(List(Rating(1, 1, 1)), MainHolder.getDataHolder().getNumberOfProducts()), 1)
    vectorsPairRDD.lookup(0)
  }

  /**
   * Get k nearest vectors from vector targetUser
   * @param targetUser SparseVector which contains ratings(Double) of a user
   * @return n nearest vectors from vector targetUser
   */
  def getNearestNeighbors(targetUser: UserVector): Seq[UserVector] = {

    //Count distance for every vector
    val vectorsWithDistances = vectorsRDD.map(v => (distanceMetric.getDistance(targetUser, v), v))

    //Ordering of vectors is by distance
    implicit def cmp: Ordering[(Double, UserVector)] = Ordering.by[(Double, UserVector), Double](_._1)

    //take k vectors with smallest distances
    val kNearestVectors = vectorsWithDistances.takeOrdered(numberOfNeighbors).map(pair => pair._2)

    kNearestVectors
  }


  /**
   * Recommend
   * @param vector Vector with ratings by target user
   * @param numberOfRecommendedProducts Maximal number of products in the recommendation
   * @return Ratings with recommended products
   */
  override def recommend(vector: UserVector, numberOfRecommendedProducts: Int): Seq[Rating] = {
    //Get k most similar users
    val nearestNeighbors: Seq[Vector[Double]] = getNearestNeighbors(vector)

    //Get average rating of every product used by similar user
    val addedRatings = nearestNeighbors.reduce(_ + _)
    val numberOfRatings = nearestNeighbors.map(vec => vec.map { value => if (value > 0) 1 else 0}).reduce(_ + _)
    val averageRatings = addedRatings.activeIterator.map { tup => val i = tup._1
      (tup._2 / numberOfRatings(i), i)
    }

    //Sort products by average rating
    val averageRatingsSorted = averageRatings.toList.sortBy(p => -p._1)

    //Convert to Ratings and exclude products already rated by the user
    val productsAlreadyRatedByUser = vector.activeKeysIterator.toSeq

    val recommendedProducts = averageRatingsSorted.map(p => Rating(0, p._2, p._1)).filter(rating => !productsAlreadyRatedByUser.contains(rating.product))

    recommendedProducts.take(numberOfRecommendedProducts)
  }
}

class UserNotFoundException extends Exception