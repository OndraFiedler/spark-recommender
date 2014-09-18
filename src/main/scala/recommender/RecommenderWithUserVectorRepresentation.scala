package recommender

import org.apache.spark.SparkContext._
import org.apache.spark.mllib.recommendation.Rating
import org.apache.spark.rdd.RDD

/**
 * Created by Ondra Fiedler on 27.8.14.
 */

/**
 * Basis for all Recommenders that use users modeled as vectors of ratings
 * @param vectorsRDD
 */
abstract class RecommenderWithUserVectorRepresentation(vectorsRDD: RDD[UserVector]) extends Recommender with Serializable {

  val vectorsPairRDD = vectorsRDD.map(vec => (vec.getUserID(), vec)).groupByKey.persist()

  /**
   * Recommend
   * @param vector vector with ratings of target user
   * @return Ratings with recommended products
   */
  def recommend(vector: UserVector, numberOfRecommendedProducts: Int): Seq[Rating]

  /**
   * Recommend
   * @param userRatingsForRecommendation Previous ratings of the user
   * @param numberOfRecommendedProducts maximal number of products in the recommendation
   * @return Ratings with recommended products
   */
  override def recommendFromRatings(userRatingsForRecommendation: Seq[Rating], numberOfRecommendedProducts: Int) = {
    val productSize = MainHolder.getDataHolder().getNumberOfProducts()
    val vector = new UserSparseVector(userRatingsForRecommendation, productSize)
    recommend(vector, numberOfRecommendedProducts)
  }

  /**
   * Recommend
   * @param userID ID of the user
   * @param numberOfRecommendedProducts maximal number of products in the recommendation
   * @return Ratings with recommended products
   */
  override def recommendFromUserID(userID: Int, numberOfRecommendedProducts: Int) = {
    try {
      val userVector = (vectorsPairRDD.lookup(userID)(0).toList)(0)
      recommend(userVector, numberOfRecommendedProducts)
    } catch {
      case e: IndexOutOfBoundsException => throw new UserNotFoundException
    }
  }
}