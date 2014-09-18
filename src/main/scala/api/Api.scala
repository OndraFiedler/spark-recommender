package api

/**
 * Created by Ondra Fiedler on 21.7.14.
 */

import akka.actor.ActorDSL._
import akka.actor.{ActorSystem, Props}
import akka.event.Logging
import akka.io.IO
import akka.io.Tcp._
import cmd.Conf
import spray.can.Http

//Adopted from https://github.com/gagnechris/SprayApiDemo

object Api {
  /**
   * Sets up the API at given interface and port
   */
  def boot(interface: String, port: Int, conf: Conf) = {

    // we need an ActorSystem to host our application in
    implicit val system = ActorSystem("spray-api-service")
    val log = Logging(system, getClass)

    val callbackActor = actor(new Act {
      become {
        case b@Bound(connection) => log.info(b.toString)
        case cf@CommandFailed(command) => log.error(cf.toString)
        case all => log.debug("Message from Akka.IO: " + all.toString)
      }
    })

    // create and start the service actor
    val service = system.actorOf(Props(classOf[ServiceActor], conf), "spray-service")

    // start a new HTTP server on given port with the service actor as the handler
    IO(Http).tell(Http.Bind(service, interface, port), callbackActor)
  }

}