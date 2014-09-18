package recommender

import breeze.collection.mutable.SparseArray
import breeze.linalg.{DenseVector, SparseVector, Vector}
import inputdata.DataHolder
import org.apache.spark.mllib.recommendation.Rating
import org.apache.spark.rdd.RDD

/**
 * Created by Ondra Fiedler on 4.8.14.
 */

/**
 * breeze.linalg.Vector[Double] with user ID
 */
trait UserVector extends Vector[Double] with Serializable {
  def getUserID(): Int

  def toMLlibVector(): org.apache.spark.mllib.linalg.Vector

  override def toString(): String = {
    "UserID: " + getUserID() + " " + super.toString()
  }
}

/**
 * breeze.linalg.DenseVector[Double] with user ID
 * @param userID ID of user
 * @param ratingsOfUser Array of rating values
 */
class UserDenseVector(userID: Int, ratingsOfUser: Array[Double]) extends DenseVector[Double](ratingsOfUser) with UserVector with Serializable {
  override def getUserID() = userID

  override def toMLlibVector() = new org.apache.spark.mllib.linalg.DenseVector(ratingsOfUser)
}

/**
 * breeze.linalg.SparseVector[Double] with user ID
 * @param userID ID of the user
 * @param ratingsOfUser array of the user's ratings (key==productID)
 */
class UserSparseVector(userID: Int, ratingsOfUser: SparseArray[Double]) extends SparseVector[Double](ratingsOfUser) with UserVector with Serializable {
  def this(ratingsToConvert: Iterable[Rating], size: Int) = this(UserSparseVector.getUserIDFromRatings(ratingsToConvert), UserSparseVector.parseToSparseArray(ratingsToConvert, size))

  override def getUserID() = userID

  override def toMLlibVector(): org.apache.spark.mllib.linalg.Vector = new org.apache.spark.mllib.linalg.SparseVector(size, index, data)
}


/**
 * One UserVector represents exactly one user
 */
class MultipleUsersMixedInOneUserSparseVectorException extends Exception

object UserSparseVector extends Serializable {
  /**
   * Parse ratings from ONE user to a breeze.linalg.SparseVector
   * @param ratingsToConvert ratings (spark.mllib.recommendation.Rating) from one user
   * @param size number of products
   * @return SparseArray containing ratings (Double)
   */
  protected def parseToSparseArray(ratingsToConvert: Iterable[Rating], size: Int): SparseArray[Double] = {

    //Arrays of products and ratings must be sorted by before calling of the SparseArray constructor!
    val sorted = ratingsToConvert.map(rat => (rat.product, rat.rating)).toList.sortBy(rat => rat._1)
    val products = sorted.map(rat => rat._1).toSeq.toArray
    val rats = sorted.map(rat => rat._2).toSeq.toArray

    val sparseArray = new SparseArray[Double](products, rats, products.length, size, 0)

    sparseArray
  }

  /**
   * Get user's ID from ratings. Check if all ratings are from the same user.
   * @param ratingsToConvert
   * @return
   */
  protected def getUserIDFromRatings(ratingsToConvert: Iterable[Rating]): Int = {
    val userID = {
      if (ratingsToConvert.size != 0)
        ratingsToConvert.head.user
      else 0
    }
    //Check if all Ratings are from the same user
    for (rating <- ratingsToConvert)
      if (rating.user != userID)
        throw new MultipleUsersMixedInOneUserSparseVectorException
    userID
  }


  /**
   * Converts Ratings from a dataHolder to RDD of UserSparseVectors (one vector per user)
   * @param dataHolder DataHolder with Ratings
   * @return RDD of UserSparseVectors (one vector per user)
   */
  def convertRatingsFromHolderToUserSparseVectors(dataHolder: DataHolder): RDD[UserVector] = {
    val ratings = dataHolder.getRatings()
    val productSize = dataHolder.getNumberOfProducts()

    //Get RDD[UserSparseVector] from Ratings (one vector for every user)
    val usersRatings = ratings.groupBy(rating => rating.user)
    val vectors = usersRatings.map(irat => {
      val v: UserVector = new UserSparseVector(irat._2, productSize); v
    }).persist()

    vectors
  }
}