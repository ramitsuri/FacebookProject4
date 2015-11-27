package com.ramitsuri.project4

import akka.actor.{Props, ActorSystem}
import akka.io.IO
import spray.can.Http

/**
 * Created by ramit on 11/26/2015.
 */
object StartServer extends App {

  implicit val facebookSystem = ActorSystem("FaceBookSystem")
  val api = facebookSystem.actorOf(Props(new FacebookServer()), "httpInterface")
  IO(Http) ! Http.Bind(api, "localhost", 8080)

}