package inputdata

import org.apache.spark.mllib.recommendation.Rating
import org.apache.spark.rdd.RDD

/**
 * Created by Ondra Fiedler on 8.8.14.
 */

/**
 * Basis for DataHolders of Netflix data (http://www.netflixprize.com/)
 * @param dataDirectoryPath Directory containing the Netflix data
 */
abstract class NetflixDataHolder(dataDirectoryPath: String) extends DataHolder {
  protected val productsIDsToNameMap = loadIDsToProductnameMapFromADirectory()

  /**
   * From file "movie_titles.txt" loads mapping from movies IDs to titles
   * @return Map: movieID -> title
   */
  protected def loadIDsToProductnameMapFromADirectory(): Map[Int, String] = {
    val sc = spark.sparkEnvironment.sc
    val movies = sc.textFile(dataDirectoryPath + "/movie_titles.txt").map { line =>
      val fields = line.split(",")
      // format: (movieID, movieName)
      (fields(0).toInt, fields(2) + " (" + fields(1) + ")")
    }.collect.toMap
    movies
  }
}

/**
 * Loads Netflix data from one file. Each line has format: movieID>,userID,rating,date.
 * @param dataDirectoryPath Directory containing the Netflix data
 * @param filename Short filename of file with ratings (dafault: "ratings.txt")
 */
class NetflixInOneFileDataHolder(dataDirectoryPath: String, filename: String = "ratings.txt") extends NetflixDataHolder(dataDirectoryPath) with Serializable {
  protected val ratings = {
    val sc = spark.sparkEnvironment.sc
    val ratingsRDD = sc.textFile(dataDirectoryPath + "/" + filename).map {
      line => val fields = line.split(",")
        (Rating(fields(1).toInt, fields(0).toInt, fields(2).toDouble))
    }
    ratingsRDD
  }
}

/**
 * Loads Netflix data from the original files.
 * @param dataDirectoryPath Directory containing the Netflix data. This directory has to contain a sub-directory "training_set" with ratings files.
 */
class NetflixInManyFilesDataHolder(dataDirectoryPath: String) extends NetflixDataHolder(dataDirectoryPath) with Serializable {
  protected val ratings = loadRatingsFromADirectory()


  protected def loadRatingsFromADirectory(): RDD[Rating] = {
    val dir = new java.io.File(dataDirectoryPath).listFiles.filter(f => f.getName == "training_set")

    if (dir.length != 1) throw new WrongInputDataException

    val files = dir(0).listFiles
    val ratingsRDDsArray = files.map { file => loadRatingsFromOneFile(file.getAbsolutePath)}
    val ratings = spark.sparkEnvironment.sc.union(ratingsRDDsArray)
    ratings.persist.coalesce(77)
  }

  protected def loadRatingsFromOneFile(absoluteFilePath: String): RDD[Rating] = {
    val ratingsTxtRDD = spark.sparkEnvironment.sc.textFile(absoluteFilePath)
    val movieIDLine = ratingsTxtRDD.first()
    val movieID = movieIDLine.split(":")(0).toInt

    val ratingsRDD = ratingsTxtRDD.map(line => if (line == movieIDLine) {
      Rating(-1, -1, -1)
    } else {
      val fields = line.split(",")
      (Rating(fields(0).toInt, movieID, fields(1).toDouble))
    })
    ratingsRDD.filter(rat => rat.user >= 0)
  }
}