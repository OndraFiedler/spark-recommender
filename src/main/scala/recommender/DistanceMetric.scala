package recommender

import breeze.linalg.Vector
import cmd.NameAndDescription

/**
 * Created by Ondra Fiedler on 4.8.14.
 */

/**
 * Distance metric between two Vectors
 */
trait DistanceMetric extends Serializable with NameAndDescription {
  def getDistance(v1: Vector[Double], v2: Vector[Double]): Double
}

object DistanceMetric {
  val distanceMetrics: List[DistanceMetric] = List(EuclideanDistance, CosineDistance)
}

object EuclideanDistance extends DistanceMetric with Serializable {
  def getDistance(v1: Vector[Double], v2: Vector[Double]): Double = {
    var distance: Double = 0
    var mutualProducts = 0
    for (i <- v1.activeKeysIterator) {
      if (v2(i) != 0) {
        distance += (v1(i) - v2(i)) * (v1(i) - v2(i))
        mutualProducts += 1
      }
    }
    if (mutualProducts == 0) return Double.PositiveInfinity
    distance / mutualProducts
  }

  override def getName: String = "euclidean"

  override def getDescription: String = "Euclidean distance"
}

object CosineDistance extends DistanceMetric with Serializable {
  def getDistance(v1: Vector[Double], v2: Vector[Double]) = {

    val dotProduct: Double = v1 dot v2
    val v1norm: Double = v1.norm(2.0)
    val v2norm: Double = v2.norm(2.0)

    1 - (dotProduct / (v1norm * v2norm))
  }

  override def getName: String = "cosine"

  override def getDescription: String = "Cosine distance"
}