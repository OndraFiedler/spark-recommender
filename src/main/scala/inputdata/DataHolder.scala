package inputdata

import org.apache.spark.mllib.recommendation.Rating
import org.apache.spark.rdd.RDD

/**
 * Created by Ondra Fiedler on 26.7.14.
 */

/**
 * Holds ratings and a Map which maps IDs to names of products
 */
trait DataHolder extends Serializable {

  protected val ratings: RDD[Rating]
  protected val productsIDsToNameMap: Map[Int, String]

  def getRatings(): RDD[Rating] = ratings

  def getIDToProductnameMap(): Map[Int, String] = productsIDsToNameMap

  def getNumberOfProducts(): Int = productsIDsToNameMap.keys.max + 1
}

class WrongInputDataException extends Exception