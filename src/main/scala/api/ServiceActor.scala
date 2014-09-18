package api

/**
 * Created by Ondra Fiedler on 21.7.14.
 */

import akka.actor.Actor
import cmd.Conf
import recommender.{MainHolder, UserNotFoundException}
import spray.http.MediaTypes._
import spray.http._
import spray.routing._

trait ApiService extends HttpService {

  def getNumberOfRecommendedProducts(): Int

  val route = pathPrefix("recommend") {
    pathPrefix("fromratings") {
      get {
        respondWithMediaType(`application/json`) {

          parameterMultiMap { params =>

            try {
              val ratings = Parser.parseRatings(params)
              val recommendation = MainHolder.getRecommenderInstance().recommendFromRatings(ratings, getNumberOfRecommendedProducts())
              val json = Parser.parseMovieIDsToJSONResponse(recommendation)
              complete(json.toString())
            }
            catch {
              case wrongApiCallEx: WrongApiCallFormat => {
                complete(StatusCodes.BadRequest)
              }
            }
          }
        }
      }
    } ~
      pathPrefix("fromuserid") {
        get {
          parameter('id) { id =>
            try {
              val recommendation = MainHolder.getRecommenderInstance().recommendFromUserID(id.toInt, getNumberOfRecommendedProducts())
              val json = Parser.parseMovieIDsToJSONResponse(recommendation)
              complete(json.toString())
            }
            catch {
              case numberFormatEx: NumberFormatException => {
                complete(StatusCodes.BadRequest)
              }
              case userNotFoundEx: UserNotFoundException => {
                complete(StatusCodes.BadRequest)
              }
            }
          }
        }
      }
  }
}

class ServiceActor(conf: Conf) extends Actor with ApiService {

  override def getNumberOfRecommendedProducts() = conf.products()

  def actorRefFactory = context

  def receive = runRoute(route)
}