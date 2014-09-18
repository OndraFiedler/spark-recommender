package inputdata

import org.apache.spark.mllib.recommendation.Rating
import org.apache.spark.rdd.RDD

/**
 * Created by Ondra Fiedler on 27.8.14.
 */

/**
 * Loads and holds the MovieLens data (http://grouplens.org/datasets/movielens/)
 * @param dataDirectoryPath Path to the directory with MovieLens data. This directory contain file "ratings.dat" with ratings and file "movies.dat" with mapping between movie IDs and names
 */
class MovieLensDataHolder(dataDirectoryPath: String) extends DataHolder with Serializable {

  protected val ratings = loadRatingsFromADirectory()
  protected val productsIDsToNameMap = loadIDsToProductnameMapFromADirectory(dataDirectoryPath)

  check()

  protected def loadRatingsFromADirectory(): RDD[Rating] = {

    val ratings = spark.sparkEnvironment.sc.textFile(dataDirectoryPath + "/ratings.dat").map { line =>
      val fields = line.split("::")
      // format: Rating(userID, movieID, rating)
      (Rating(fields(0).toInt, fields(1).toInt, fields(2).toDouble))
    }
    ratings
  }

  protected def loadIDsToProductnameMapFromADirectory(dataDirectoryPath: String): Map[Int, String] = {
    val movies = spark.sparkEnvironment.sc.textFile(dataDirectoryPath + "/movies.dat").map { line =>
      val fields = line.split("::")
      // format: (movieID, movieName)
      (fields(0).toInt, fields(1))
    }.collect.toMap
    movies
  }

  protected def check() = {
    val wrong = ratings.filter(r => (r.rating < 0 || (r.rating > 5) || (!productsIDsToNameMap.contains(r.product))))
    if (wrong.count() != 0) throw new WrongInputDataException
  }
}