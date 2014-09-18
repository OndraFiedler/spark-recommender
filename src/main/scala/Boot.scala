/**
 * Created by Ondra Fiedler on 23.7.14.
 */

import api.Api
import cmd.Conf
import recommender.MainHolder

object Boot extends App {

  //Read arguments from command line
  val opts = new Conf(args)

  //Set up the Recommender system
  MainHolder.setUp(opts)

  //Boot API
  Api.boot(opts.interface(), opts.port(), opts)

}