package api

import org.apache.spark.mllib.recommendation.Rating
import recommender.MainHolder
import spray.json.{DefaultJsonProtocol, _}

/**
 * Created by Ondra Fiedler on 23.7.14.
 */

/**
 * Parse parameters from API requests and parse results to JSON
 */
object Parser {
  val ratingApiStr = "rating"
  val idApiStr = "id"

  val supportedParameters = List(ratingApiStr, idApiStr)

  /**
   * Checks if all parameters are supported
   *
   * @param parameters Parameters as Map[String, List[String] mapping a parameter name to a list of all its values.
   * @return True if all parameters are supported. False otherwise.
   */
  private def checkUnsupportedParameters(parameters: Map[String, List[String]]): Boolean = {

    val unsupportedParams = parameters.filter(par => !supportedParameters.contains(par._1))

    if (unsupportedParams.isEmpty)
      true
    else false
  }

  /**
   * Parse ratings from parameters
   *
   * @param parameters Parameters as Map[String, List[String] mapping a parameter name to a list of all its values.
   * @return Sequence of org.apache.spark.mllib.recommendation.Rating
   */
  def parseRatings(parameters: Map[String, List[String]]): Seq[Rating] = {

    //Check if all used parameters are supported
    if (!checkUnsupportedParameters(parameters))
      throw new WrongApiCallFormat

    if (!parameters.contains(ratingApiStr) || parameters(ratingApiStr).size == 0) return List()

    val ratingsSplit = parameters(ratingApiStr).map(ratingStr => ratingStr.split(","))
    try {
      //ratings(0):id , ratings(1): rating
      val ratings = ratingsSplit.filter(rs => rs.size == 2).map(rs2 => (rs2(0).toInt, rs2(1).toDouble))

      //Wrong format - more values than id and rating
      if (ratingsSplit.size != ratings.size) throw new WrongApiCallFormat

      ratings.map(rating => Rating(0, rating._1, rating._2))

    } catch {
      case nume: java.lang.NumberFormatException => {
        throw new WrongApiCallFormat
      }
    }
  }

  /**
   * Converts ratings to a JSON object
   * @param recommendationRatings ratings for conversion
   * @return JSON object
   */
  def parseMovieIDsToJSONResponse(recommendationRatings: Seq[Rating]): JsValue = {

    val idToProductnameMap = MainHolder.getDataHolder().getIDToProductnameMap()
    val recommendation = recommendationRatings.map(rating => (rating.product, rating.rating, idToProductnameMap(rating.product)))

    case class RecommendationForJson(product: Int, rating: Double, name: String)

    case class AllRecommendationsForJson(recommendations: Seq[JsValue])

    object RecommendationJsonProtocol extends DefaultJsonProtocol {
      implicit val recommendationFormat = jsonFormat3(RecommendationForJson)
      implicit val recommendationsFormat = jsonFormat1(AllRecommendationsForJson)
    }

    import RecommendationJsonProtocol._
    val recommendations = recommendation.map(r => new RecommendationForJson(r._1, r._2, r._3).toJson)

    val json = AllRecommendationsForJson(recommendations).toJson

    json
  }

}

class WrongApiCallFormat extends Exception {}
