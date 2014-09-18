package recommender

import org.apache.spark.mllib.recommendation.Rating

/**
 * Created by Ondra Fiedler on 27.8.14.
 */

/**
 * Trait for Recommenders
 */
trait Recommender extends Serializable {

  /**
   * Recommendation of products based on previous ratings
   * @param userRatingsForRecommendation Previous ratings of the user
   * @return Ratings with recommended products
   */
  def recommendFromRatings(userRatingsForRecommendation: Seq[Rating], numberOfRecommendedProducts: Int = 10): Seq[Rating]

  /**
   * Recommendation of products based on previous ratings of given user
   * @param userID ID of the user
   * @return Ratings with recommended products
   */
  def recommendFromUserID(userID: Int, numberOfRecommendedProducts: Int = 10): Seq[Rating]
}
