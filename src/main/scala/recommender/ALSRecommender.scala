package recommender

import org.apache.spark.SparkContext._
import org.apache.spark.mllib.recommendation.{ALS, MatrixFactorizationModel, Rating}
import org.apache.spark.rdd.RDD

/**
 * Created by Ondra Fiedler on 23.7.14.
 */

/**
 * Recommender using Alternating Least Squares (ALS) from Spark's MLlib
 * @param rank Rank of the feature matrices (number of features).
 * @param lambda The regularization parameter
 * @param numIters The number of iterations to run
 */
class ALSRecommender(rank: Int, lambda: Double, numIters: Int) extends Recommender {

  val sc = spark.sparkEnvironment.sc
  val movies = MainHolder.getDataHolder().getIDToProductnameMap()
  val ratings = MainHolder.getDataHolder().getRatings()
  val ratingsGroupedByUser = ratings.map(rat => (rat.user, rat)).groupByKey().persist()
  val model = train()

  protected def train(userRatingsForRecommendationRDD: Option[RDD[Rating]] = None): Option[MatrixFactorizationModel] = {
    val trainingData = userRatingsForRecommendationRDD match {
      case Some(rdd) => ratings.union(rdd)
      case None => ratings
    }
    trainingData.persist

    val model = ALS.train(trainingData, rank, numIters, lambda)
    Some(model)
  }

  override def recommendFromRatings(userRatingsForRecommendation: Seq[Rating], numberOfRecommendedProducts: Int) = {

    val model = train(Some(sc.parallelize(userRatingsForRecommendation)))
    val myRatedMovieIDs = userRatingsForRecommendation.map(_.product).toSet
    val candidates = sc.parallelize(movies.keys.filter(!myRatedMovieIDs.contains(_)).toSeq)
    val recommendations = model.get
      .predict(candidates.map((0, _)))
      .collect
      .sortBy(-_.rating)
      .take(numberOfRecommendedProducts)
    recommendations
  }


  override def recommendFromUserID(userID: Int, numberOfRecommendedProducts: Int) = {
    val userRatings = {
      val ratings = ratingsGroupedByUser.lookup(userID)
      if (ratings.length <= 0) {
        throw new UserNotFoundException
      }
      ratings(0)
    }
    val ratedProducts = userRatings.map(rat => rat.product).toList

    val candidates = sc.parallelize(movies.keys.filter(!ratedProducts.contains(_)).toSeq)

    val recommendations = model.get
      .predict(candidates.map((userID, _)))
      .collect
      .sortBy(-_.rating)
      .take(numberOfRecommendedProducts)
    recommendations
  }
}